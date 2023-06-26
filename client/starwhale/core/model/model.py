from __future__ import annotations

import os
import copy
import json
import shutil
import typing as t
import tarfile
from abc import ABCMeta
from enum import Enum, unique
from http import HTTPStatus
from pathlib import Path
from collections import defaultdict

import yaml
from fs import open_fs
from fs.walk import Walker

from starwhale.utils import (
    console,
    now_str,
    load_yaml,
    pretty_bytes,
    gen_uniq_version,
    validate_obj_name,
)
from starwhale.consts import (
    FileDesc,
    FileFlag,
    FileNode,
    RunStatus,
    HTTPMethod,
    CREATED_AT_KEY,
    SWMP_SRC_FNAME,
    DefaultYAMLName,
    SW_AUTO_DIRNAME,
    DEFAULT_PAGE_IDX,
    DIGEST_FILE_NAME,
    DEFAULT_PAGE_SIZE,
    SHORT_VERSION_CNT,
    RESOURCE_FILES_NAME,
    SW_IGNORE_FILE_NAME,
    DEFAULT_MANIFEST_NAME,
    DEFAULT_JOBS_FILE_NAME,
    SW_EVALUATION_EXAMPLE_DIR,
    DEFAULT_STARWHALE_API_VERSION,
    EVALUATION_SVC_META_FILE_NAME,
    EVALUATION_PANEL_LAYOUT_JSON_FILE_NAME,
    EVALUATION_PANEL_LAYOUT_YAML_FILE_NAME,
    DEFAULT_FILE_SIZE_THRESHOLD_TO_TAR_IN_MODEL,
)
from starwhale.base.tag import StandaloneTag
from starwhale.utils.fs import (
    copy_dir,
    move_dir,
    copy_file,
    empty_dir,
    file_stat,
    ensure_dir,
    ensure_file,
    blake2b_file,
)
from starwhale.base.type import BundleType, InstanceType, RunSubDirType
from starwhale.base.cloud import CloudRequestMixed, CloudBundleModelMixin
from starwhale.base.mixin import ASDictMixin
from starwhale.utils.http import ignore_error
from starwhale.utils.load import load_module
from starwhale.api.service import Service
from starwhale.base.bundle import BaseBundle, LocalStorageBundleMixin
from starwhale.utils.error import NoSupportError
from starwhale.base.context import Context
from starwhale.api._impl.job import Handler, generate_jobs_yaml
from starwhale.base.scheduler import Step, Scheduler
from starwhale.core.job.store import JobStorage
from starwhale.utils.progress import run_with_progress_bar
from starwhale.base.blob.store import LocalFileStore, BuiltinPyExcludes
from starwhale.base.bundle_copy import BundleCopy
from starwhale.base.uri.project import Project
from starwhale.core.model.store import ModelStorage
from starwhale.api._impl.service import Hijack
from starwhale.base.uri.resource import Resource, ResourceType
from starwhale.core.runtime.model import StandaloneRuntime


@unique
class ModelInfoFilter(Enum):
    basic = "basic"
    model_yaml = "model_yaml"
    handlers = "handlers"
    files = "files"
    all = "all"


class ModelRunConfig(ASDictMixin):
    def __init__(
        self,
        handler: str = "",
        modules: t.Optional[t.List[str]] = None,
        envs: t.Optional[t.List[str]] = None,
        **kw: t.Any,
    ):
        modules = modules or []
        # compatible with old runtime.yaml format, handler field needs to handle
        if handler:
            modules.append(handler)
        self.modules = [m.strip() for m in modules if m.strip()]
        self.envs = envs or []

    def __str__(self) -> str:
        return f"Model Run Config: {self.modules}"

    def __repr__(self) -> str:
        return f"Model Run Config: modules -> {self.modules}, envs -> {self.envs}"

    def do_validate(self) -> None:
        if not self.modules:
            raise ValueError("not found any modules in model run config")


class ModelConfig(ASDictMixin):
    def __init__(
        self,
        name: str = "",
        run: t.Optional[t.Dict[str, t.Any]] = None,
        desc: str = "",
        version: str = DEFAULT_STARWHALE_API_VERSION,
        **kw: t.Any,
    ):
        self.name = name.strip()
        self.run = ModelRunConfig(**(run or {}))
        self.desc = desc
        self.version = version

    def do_validate(self) -> None:
        ok, reason = validate_obj_name(self.name)
        if not ok:
            raise ValueError(f"model name {self.name} is invalid: {reason}")

        self.run.do_validate()

    @classmethod
    def create_by_yaml(cls, path: Path) -> ModelConfig:
        c = load_yaml(path)
        return cls(**c)

    def __str__(self) -> str:
        return f"Model Config: {self.name}"

    def __repr__(self) -> str:
        return f"Model Config: name -> {self.name}, run: {self.run}, desc: {self.desc}"


class Model(BaseBundle, metaclass=ABCMeta):
    def __str__(self) -> str:
        return f"Starwhale Model: {self.uri}"

    @classmethod
    def get_model(cls, uri: Resource) -> Model:
        _cls = cls._get_cls(uri)
        return _cls(uri)

    @classmethod
    def _get_cls(cls, uri: Resource) -> t.Union[t.Type[StandaloneModel], t.Type[CloudModel]]:  # type: ignore
        if uri.instance.is_local:
            return StandaloneModel
        elif uri.instance.is_cloud:
            return CloudModel
        else:
            raise NoSupportError(f"model uri:{uri}")

    @classmethod
    def copy(
        cls,
        src_uri: Resource,
        dest_uri: str,
        force: bool = False,
        dest_local_project_uri: str = "",
    ) -> None:
        bc = BundleCopy(
            src_uri,
            dest_uri,
            ResourceType.model,
            force,
            dest_local_project_uri=dest_local_project_uri,
        )
        bc.do()

    def diff(self, compare_uri: Resource) -> t.Dict[str, t.Any]:
        raise NotImplementedError


def resource_to_file_node(
    files: t.List[t.Dict[str, t.Any]], parent_path: Path
) -> t.Dict[str, FileNode]:
    return {
        _f["path"]: FileNode(
            path=parent_path / _f["path"],
            name=_f.get("name") or os.path.basename(_f["path"]),
            size=_f.get("size") or file_stat(parent_path / _f["path"]).st_size,
            signature=_f.get("signature") or blake2b_file(parent_path / _f["path"]),
            file_desc=FileDesc[_f["desc"]],
        )
        for _f in files
    }


class StandaloneModel(Model, LocalStorageBundleMixin):
    def __init__(self, uri: Resource) -> None:
        super().__init__(uri)
        self.typ = InstanceType.STANDALONE
        self.store: ModelStorage = ModelStorage(uri)
        self.tag = StandaloneTag(uri)
        self._manifest: t.Dict[str, t.Any] = {}  # TODO: use manifest class
        self.models: t.List[t.Dict[str, t.Any]] = []
        self.sources: t.List[t.Dict[str, t.Any]] = []
        self.yaml_name = DefaultYAMLName.MODEL
        self._version = uri.version
        self._object_store = LocalFileStore()

    def list_tags(self) -> t.List[str]:
        return self.tag.list()

    def add_tags(self, tags: t.List[str], ignore_errors: bool = False) -> None:
        self.tag.add(tags, ignore_errors)

    def remove_tags(self, tags: t.List[str], ignore_errors: bool = False) -> None:
        self.tag.remove(tags, ignore_errors)

    def _gen_model_serving(self, search_modules: t.List[str], workdir: Path) -> None:
        console.debug(f"generating model serving config for {self.uri} ...")
        rc_dir = str(
            self.store.hidden_sw_dir.relative_to(self.store.snapshot_workdir)
            / SW_EVALUATION_EXAMPLE_DIR
        )
        # render spec
        svc = self._get_service(search_modules, workdir, hijack=Hijack(True, rc_dir))
        file = self.store.hidden_sw_dir / EVALUATION_SVC_META_FILE_NAME
        spec = svc.get_spec()
        ensure_file(file, json.dumps(spec, indent=4), parents=True)
        if len(spec) == 0:
            return

        # make virtual handler to make the model serving can be used in model run
        func = self._serve_handler
        cls_name, _, func_name = func.__qualname__.rpartition(".")
        h = Handler(
            name="serving",
            show_name="virtual handler for model serving",
            func_name=func.__qualname__,
            module_name=func.__module__,
            expose=8080,
            virtual=True,
            extra_kwargs={
                "search_modules": search_modules,
            },
        )
        Handler._register(h, func)

        if len(svc.example_resources) == 0:
            return

        # check duplicate file names, do not support using examples with same name in different dir
        names = set([os.path.basename(i) for i in svc.example_resources])
        if len(names) != len(svc.example_resources):
            raise NoSupportError("duplicate file names in examples")

        # copy example resources for online evaluation in server instance
        dst = self.store.hidden_sw_dir / SW_EVALUATION_EXAMPLE_DIR
        ensure_dir(dst)
        for f in svc.example_resources:
            shutil.copy2(f, dst)

    def _render_eval_layout(self, workdir: Path) -> None:
        # render eval layout
        eval_layout = workdir / SW_AUTO_DIRNAME / EVALUATION_PANEL_LAYOUT_YAML_FILE_NAME
        if eval_layout.exists():
            content = load_yaml(eval_layout)
            dst = self.store.hidden_sw_dir / EVALUATION_PANEL_LAYOUT_JSON_FILE_NAME
            ensure_file(dst, json.dumps(content), parents=True)

    @staticmethod
    def _get_service(
        search_modules: t.List[str], pkg: Path, hijack: t.Optional[Hijack] = None
    ) -> Service:
        from starwhale.api._impl.service import internal_api_list

        apis = dict()

        # TODO: refine this ugly ad hoc
        Context.set_runtime_context(
            Context(pkg, version="-1", project="tmp-project-for-build")
        )

        for module_name in search_modules:
            module_name = module_name.split(":")[0].strip()
            if not module_name:
                continue
            load_module(module_name, pkg)

        apis.update(internal_api_list())

        # TODO: support custom service class for api register
        # TODO: support add_api function for PipelineHandler for api register

        api_within_instance_map = dict()
        # check if we need to instance the model when using custom handler
        for name, api in apis.items():
            qualname = api.func.__qualname__
            cls_name = qualname.rpartition(".")[0]

            if "." in cls_name:
                raise NoSupportError(
                    f"api decorator no supports inner class method: {qualname}"
                )

            # TODO: support deep path classes (also modules)
            if cls_name != "":
                m = load_module(api.func.__module__, pkg)
                api_within_instance_map[name] = getattr(m, cls_name)()

        svc = Service()
        svc.api_within_instance_map = api_within_instance_map

        for api in apis.values():
            svc.add_api_instance(api)

        svc.hijack = hijack
        return svc

    def extract(self, force: bool = False, target: t.Union[str, Path] = "") -> Path:
        target = Path(target)
        console.print(f":package: Extracting model({self.uri}) ...")
        copy_dir(src_dir=self.store.src_dir, dest_dir=target, force=force)
        console.print(f":clap: Model extracted to {target}")
        return target

    @classmethod
    def run(
        cls,
        model_src_dir: Path,
        model_config: ModelConfig,
        project: str,
        version: str = "",
        run_handler: str = "",
        dataset_uris: t.Optional[t.List[str]] = None,
        scheduler_run_args: t.Optional[t.Dict[str, t.Any]] = None,
        external_info: t.Optional[t.Dict[str, t.Any]] = None,
        forbid_snapshot: bool = False,
        cleanup_snapshot: bool = True,
        force_generate_jobs_yaml: bool = False,
    ) -> None:
        external_info = external_info or {}
        dataset_uris = dataset_uris or []
        scheduler_run_args = scheduler_run_args or {}
        version = version or gen_uniq_version()

        job_dir = JobStorage.local_run_dir(project, version)
        if forbid_snapshot:
            snapshot_dir = model_src_dir
        else:
            snapshot_dir = job_dir / RunSubDirType.SNAPSHOT
            # TODO: tune performance for copy files, such as overlay
            copy_dir(model_src_dir, snapshot_dir)

        # change current dir into snapshot dir, this will help user code to find files easily.
        console.print(f":airplane: change current dir to {snapshot_dir}")
        os.chdir(snapshot_dir)

        job_yaml_path = snapshot_dir / SW_AUTO_DIRNAME / DEFAULT_JOBS_FILE_NAME
        if not job_yaml_path.exists() or force_generate_jobs_yaml:
            generate_jobs_yaml(
                search_modules=model_config.run.modules,
                package_dir=snapshot_dir,
                yaml_path=job_yaml_path,
            )

        console.print(
            f":hourglass_not_done: start to run model, handler:{run_handler} ..."
        )
        scheduler = Scheduler(
            project=project,
            version=version,
            workdir=snapshot_dir,
            dataset_uris=dataset_uris,
            steps=Step.get_steps_from_yaml(run_handler, job_yaml_path),
        )
        scheduler_status = RunStatus.START
        error_message = ""
        start = now_str()
        try:
            results = scheduler.run(**scheduler_run_args)
            scheduler_status = RunStatus.SUCCESS
            exceptions: t.List[Exception] = []
            for _r in results:
                for _tr in _r.task_results:
                    if _tr.exception:
                        exceptions.append(_tr.exception)
            if exceptions:
                raise Exception(*exceptions)
        except Exception:
            scheduler_status = RunStatus.FAILED
            console.print_exception()
            raise
        finally:
            _manifest: t.Dict[str, t.Any] = {
                CREATED_AT_KEY: start,
                "scheduler_run_args": scheduler_run_args,
                "version": version,
                "project": project,
                "model_src_dir": str(snapshot_dir),
                "datasets": dataset_uris,
                "status": scheduler_status,
                "error_message": error_message,
                "finished_at": now_str(),
                **external_info,
            }

            ensure_file(
                job_dir / DEFAULT_MANIFEST_NAME,
                yaml.safe_dump(_manifest, default_flow_style=False),
                parents=True,
            )

            console.print(
                f":{100 if scheduler_status == RunStatus.SUCCESS else 'broken_heart'}: finish run, {scheduler_status}!"
            )

            if not forbid_snapshot and cleanup_snapshot:
                empty_dir(snapshot_dir, ignore_errors=True)

    def diff(self, compare_uri: Resource) -> t.Dict[str, t.Any]:
        """
        - added: a node that exists in compare but not in base
        - deleted: a node that not exists in compare but in base
        - updated: a node that exists in both of base and compare but signature is different
        - unchanged: a node that exists in both of base and compare, and signature is same
        :param compare_uri:
        :return: diff info
        """
        # TODO use remote get model info for cloud
        if not compare_uri.instance.is_local:
            raise NoSupportError(
                f"only support standalone uri, but compare_uri({compare_uri}) is for cloud instance"
            )
        if self.uri.name != compare_uri.name:
            raise NoSupportError(
                f"only support two versions diff in one model, base model:{self.uri}, compare model:{compare_uri}"
            )
        _compare_model = StandaloneModel(compare_uri)
        base_file_maps = resource_to_file_node(
            files=self.store.resource_files,
            parent_path=self.store.snapshot_workdir,
        )
        compare_file_maps = resource_to_file_node(
            files=_compare_model.store.resource_files,
            parent_path=_compare_model.store.snapshot_workdir,
        )
        all_paths = {
            p for m in (base_file_maps, compare_file_maps) for p, _ in m.items()
        }

        for _p in all_paths:
            if _p in base_file_maps and _p in compare_file_maps:
                if base_file_maps[_p].signature == compare_file_maps[_p].signature:
                    compare_file_maps[_p].flag = FileFlag.UNCHANGED
                else:
                    compare_file_maps[_p].flag = FileFlag.UPDATED
            if _p in base_file_maps and _p not in compare_file_maps:
                del_file = copy.copy(base_file_maps[_p])
                del_file.flag = FileFlag.DELETED
                compare_file_maps[_p] = del_file
            if _p not in base_file_maps and _p in compare_file_maps:
                compare_file_maps[_p].flag = FileFlag.ADDED

        return {
            "all_paths": all_paths,
            "base_version": base_file_maps,
            "compare_version": compare_file_maps,
        }

    def info(self) -> t.Dict[str, t.Any]:
        ret: t.Dict[str, t.Any] = {}
        if not self.store.bundle_path.exists():
            return ret

        job_yaml = load_yaml(self.store.hidden_sw_dir / DEFAULT_JOBS_FILE_NAME)
        ret["basic"] = copy.deepcopy(self.store.digest)
        ret["basic"].update(
            {
                "name": self.uri.name,
                "version": self.uri.version,
                "project": self.uri.project.name,
                "path": str(self.store.bundle_path),
                "tags": StandaloneTag(self.uri).list(),
                "handlers": sorted(
                    {k for k, v in job_yaml.items() if not v[0].get("virtual", False)}
                ),
            }
        )

        ret["model_yaml"] = (
            self.store.hidden_sw_dir / DefaultYAMLName.MODEL
        ).read_text()
        ret["handlers"] = job_yaml
        ret["files"] = self.store.resource_files
        return ret

    def history(
        self,
        page: int = DEFAULT_PAGE_IDX,
        size: int = DEFAULT_PAGE_SIZE,
    ) -> t.List[t.Dict[str, t.Any]]:
        _r = []
        for _bf in self.store.iter_bundle_history():
            _manifest_path = _bf.path / DEFAULT_MANIFEST_NAME
            if not _manifest_path.exists():
                continue

            _digest_path = _bf.path / "src" / SW_AUTO_DIRNAME / DIGEST_FILE_NAME
            if _digest_path.exists():
                _info = load_yaml(_digest_path)
            else:
                _info = load_yaml(_manifest_path)

            packaged_runtime = _info.get("packaged_runtime")
            if packaged_runtime:
                runtime = f"{packaged_runtime['name']}/version/{packaged_runtime['manifest']['version'][:SHORT_VERSION_CNT]}"
            else:
                runtime = ""

            _r.append(
                dict(
                    name=self.name,
                    version=_bf.version,
                    path=str(_bf.path.resolve()),
                    tags=_bf.tags,
                    created_at=_info[CREATED_AT_KEY],
                    size=_info.get("size", 0),
                    runtime=runtime,
                )
            )
        return _r

    def remove(self, force: bool = False) -> t.Tuple[bool, str]:
        return self._do_remove(force)

    def recover(self, force: bool = False) -> t.Tuple[bool, str]:
        # TODO: support short version to recover, today only support full-version
        dest_path = self.store.bundle_dir / f"{self.uri.version}{BundleType.MODEL}"
        _ok, _reason = move_dir(self.store.recover_loc, dest_path, force)
        _ok2, _reason2 = True, ""
        if self.store.recover_snapshot_workdir.exists():
            _ok2, _reason2 = move_dir(
                self.store.recover_snapshot_workdir, self.store.snapshot_workdir, force
            )
        return _ok and _ok2, _reason + _reason2

    @classmethod
    def list(
        cls,
        project_uri: Project,
        page: int = DEFAULT_PAGE_IDX,
        size: int = DEFAULT_PAGE_SIZE,
        filters: t.Optional[t.Union[t.Dict[str, t.Any], t.List[str]]] = None,
    ) -> t.Tuple[t.Dict[str, t.Any], t.Dict[str, t.Any]]:
        filters = filters or {}
        rs = defaultdict(list)
        for _bf in ModelStorage.iter_all_bundles(
            project_uri,
            bundle_type=BundleType.MODEL,
            uri_type=ResourceType.model,
        ):
            _mpath = _bf.path / DEFAULT_MANIFEST_NAME
            if not _mpath.exists() or not cls.do_bundle_filter(_bf, filters):
                continue

            _digest_path = _bf.path / "src" / SW_AUTO_DIRNAME / DIGEST_FILE_NAME
            if _digest_path.exists():
                _info = load_yaml(_digest_path)
            else:
                _info = load_yaml(_mpath)

            if not _info:
                continue

            rs[_bf.name].append(
                {
                    "name": _bf.name,
                    "version": _bf.version,
                    "path": str(_bf.path.absolute()),
                    "size": _info.get("size", 0),
                    "is_removed": _bf.is_removed,
                    CREATED_AT_KEY: _info[CREATED_AT_KEY],
                    "tags": _bf.tags,
                }
            )
        return rs, {}

    def buildImpl(self, workdir: Path, **kw: t.Any) -> None:  # type: ignore[override]
        model_config: ModelConfig = kw["model_config"]

        operations = [
            (self._gen_version, 5, "gen version"),
            (self._prepare_snapshot, 5, "prepare snapshot"),
            (
                self._copy_src,
                15,
                "copy src",
                dict(
                    workdir=workdir,
                    model_config=model_config,
                    add_all=kw.get("add_all", False),
                ),
            ),
        ]
        packaging_runtime_uri = kw.get("packaging_runtime_uri")
        if packaging_runtime_uri:
            operations.append(
                (
                    self._package_runtime,
                    10,
                    "package runtime",
                    dict(runtime_uri=packaging_runtime_uri),
                )
            )

        operations += [
            (
                self._gen_model_serving,
                10,
                "generate model serving",
                dict(search_modules=model_config.run.modules, workdir=workdir),
            ),
            (
                generate_jobs_yaml,
                5,
                "generate jobs yaml",
                dict(
                    search_modules=model_config.run.modules,
                    package_dir=workdir,
                    yaml_path=self.store.src_dir
                    / SW_AUTO_DIRNAME
                    / DEFAULT_JOBS_FILE_NAME,
                ),
            ),
            (
                self._render_eval_layout,
                1,
                "render eval layout",
                dict(workdir=workdir),
            ),
            (
                self._make_meta_tar,
                20,
                "build model bundle",
            ),
            (
                self._render_manifest,
                5,
                "render manifest",
            ),
            (self._make_auto_tags, 5, "make auto tags"),
        ]

        run_with_progress_bar("model bundle building...", operations)

    def _package_runtime(self, runtime_uri: Resource | str) -> None:
        if isinstance(runtime_uri, str):
            uri = Resource(runtime_uri, typ=ResourceType.runtime)
        else:
            uri = runtime_uri

        # TODO: support runtime uri in the cloud instance
        if not uri.instance.is_local:
            raise NoSupportError(
                f"runtime type {uri.instance.type} not support in package runtime"
            )

        if uri.typ == ResourceType.runtime:
            runtime = StandaloneRuntime(uri)
            bundle_path = runtime.store.bundle_path
            info = {
                "name": runtime.name,
                "manifest": runtime.store.manifest,
                "hash": blake2b_file(bundle_path),
            }
        elif uri.typ == ResourceType.model:
            model = StandaloneModel(uri)
            bundle_path = model.store.packaged_runtime_bundle_path
            packaged_runtime = model.store.digest.get("packaged_runtime")
            if not packaged_runtime or not bundle_path.exists():
                raise RuntimeError(f"model {uri} not packaged runtime")

            info = packaged_runtime
        else:
            raise NoSupportError(f"uri type {uri.typ} not support to package runtime")

        dest = self.store.packaged_runtime_bundle_path
        console.print(f":optical_disk: package runtime({uri}) to {dest}")
        copy_file(bundle_path, dest)

        self._manifest["packaged_runtime"] = {
            **info,
            "path": str(dest.relative_to(self.store.snapshot_workdir)),
        }

    def _make_meta_tar(
        self, size_th_to_tar: int = DEFAULT_FILE_SIZE_THRESHOLD_TO_TAR_IN_MODEL
    ) -> None:
        w = Walker()
        src_fs = open_fs(str(self.store.src_dir.resolve()))
        total_size = 0

        for f in w.files(src_fs):
            sub_path = f[1:]
            size = file_stat(self.store.src_dir / sub_path).st_size
            separate = size > size_th_to_tar
            file_info = {
                "name": os.path.basename(f),
                "path": f"{self.store.src_dir_name}/{sub_path}",
                "signature": blake2b_file(self.store.src_dir / sub_path),
                "duplicate_check": separate,
                "desc": FileDesc.SRC.name if not separate else FileDesc.MODEL.name,
                "size": size,
                "arcname": sub_path,
            }
            if separate:
                self.models.append(file_info)
            else:
                self.sources.append(file_info)
            total_size += size

        self._manifest["size"] = total_size
        console.info(f":basket: resource files size: {pretty_bytes(total_size)}")

        ensure_file(
            self.store.resource_files_path,
            content=yaml.dump(self.models + self.sources, default_flow_style=False),
            parents=True,
        )

        ensure_file(
            self.store.digest_path,
            content=yaml.dump(self._manifest, default_flow_style=False),
            parents=True,
        )

        with tarfile.open(self.store.snapshot_workdir / SWMP_SRC_FNAME, "w:") as tar:
            for file_info in self.sources:
                arcname = str(file_info["arcname"])
                tar.add(str(self.store.src_dir / arcname), arcname=arcname)

            tar.add(
                str(self.store.resource_files_path),
                arcname=f"{SW_AUTO_DIRNAME}/{RESOURCE_FILES_NAME}",
            )

            tar.add(
                str(self.store.digest_path),
                arcname=f"{SW_AUTO_DIRNAME}/{DIGEST_FILE_NAME}",
            )

    def _render_manifest(self) -> None:
        self._manifest["resources"] = self.models + self.sources
        super()._render_manifest()

    @classmethod
    def load_model_config(cls, yaml_path: Path, workdir: Path) -> ModelConfig:
        cls._do_validate_yaml(yaml_path)
        _config = ModelConfig.create_by_yaml(yaml_path)

        # TODO: add more model.yaml section validation
        # TODO: add 'swcli model check' cmd

        cls._load_config_envs(_config)
        return _config

    def _prepare_snapshot(self) -> None:
        ensure_dir(self.store.snapshot_workdir)
        ensure_dir(self.store.src_dir)

        # TODO: cleanup garbage dir
        # TODO: add lock/flag file for gc

        console.print(
            f":file_folder: workdir: [underline]{self.store.snapshot_workdir}[/]"
        )

    def _copy_src(
        self, workdir: Path, model_config: ModelConfig, add_all: bool
    ) -> None:
        """
        Copy source code files to snapshot workdir
        Args:
            workdir: source code dir
            model_config: model config
            add_all: copy all files, include python cache files(defined in BuiltinPyExcludes) and venv or conda files
        Returns: None
        """
        console.print(
            f":peacock: copy source code files: {workdir} -> {self.store.src_dir}"
        )

        excludes = []
        ignore = workdir / SW_IGNORE_FILE_NAME
        if ignore.exists():
            with open(ignore, "r") as f:
                excludes = [line.strip() for line in f.readlines()]
        if not add_all:
            excludes += BuiltinPyExcludes

        console.debug(
            f"copy dir: {workdir} -> {self.store.src_dir}, excludes: {excludes}"
        )
        total_size = self._object_store.copy_dir(
            src_dir=workdir.resolve(),
            dst_dir=self.store.src_dir.resolve(),
            excludes=excludes,
            ignore_venv_or_conda=not add_all,
        )
        console.print(
            f":file_folder: source code files size: {pretty_bytes(total_size)}"
        )

        model_yaml = yaml.safe_dump(model_config.asdict(), default_flow_style=False)
        ensure_file(
            self.store.hidden_sw_dir / DefaultYAMLName.MODEL, model_yaml, parents=True
        )

        # make sure model.yaml exists, prevent using the wrong config when running in the cloud
        ensure_file(
            self.store.src_dir / DefaultYAMLName.MODEL, model_yaml, parents=True
        )

    @classmethod
    def _load_config_envs(cls, _config: ModelConfig) -> None:
        for _env in _config.run.envs:
            _env = _env.strip()
            if not _env:
                continue
            _t = _env.split("=", 1)
            _k, _v = _t[0], "".join(_t[1:])

            if _k not in os.environ:
                os.environ[_k] = _v

    @classmethod
    def serve(
        cls,
        model_config: ModelConfig,
        model_src_dir: Path,
        host: str,
        port: int,
    ) -> None:
        svc = cls._get_service(model_config.run.modules, model_src_dir)
        svc.serve(host, port, model_config.name)

    @classmethod
    def _serve_handler(cls, **kwargs: t.Any) -> None:
        # https://github.com/star-whale/starwhale/pull/2350
        # kwargs see cls._gen_model_serving
        search_modules = kwargs.get("extra_kwargs", {}).get("search_modules", [])
        if len(search_modules) == 0:
            raise ValueError("search_modules is empty")

        workdir = kwargs.get("workdir", "")
        if not workdir:
            raise ValueError("workdir is empty")

        port = kwargs.get("expose", 8080)
        svc = cls._get_service(search_modules, Path(workdir))
        svc.serve("0.0.0.0", port)


class CloudModel(CloudBundleModelMixin, Model):
    def __init__(self, uri: Resource) -> None:
        super().__init__(uri)
        self.typ = InstanceType.CLOUD

    @classmethod
    @ignore_error(({}, {}))
    def list(
        cls,
        project_uri: Project,
        page: int = DEFAULT_PAGE_IDX,
        size: int = DEFAULT_PAGE_SIZE,
        filter_dict: t.Optional[t.Dict[str, t.Any]] = None,
    ) -> t.Tuple[t.Dict[str, t.Any], t.Dict[str, t.Any]]:
        filter_dict = filter_dict or {}
        crm = CloudRequestMixed()
        return crm._fetch_bundle_all_list(
            project_uri, ResourceType.model, page, size, filter_dict
        )

    def build(self, *args: t.Any, **kwargs: t.Any) -> None:
        raise NoSupportError("no support build model in the cloud instance")

    def diff(self, compare_uri: Resource) -> t.Dict[str, t.Any]:
        raise NoSupportError("no support model diff in the cloud instance")

    @classmethod
    def run(
        cls,
        project_uri: Project,
        model_uri: str,
        dataset_uris: t.List[str],
        runtime_uri: str,
        run_handler: str | int,
        resource_pool: str = "default",
    ) -> t.Tuple[bool, str]:
        crm = CloudRequestMixed()
        _model_uri = Resource(
            model_uri, ResourceType.model, project=project_uri, refine=True
        )
        _dataset_uris = [
            Resource(i, ResourceType.dataset, project=project_uri, refine=True)
            for i in dataset_uris
        ]
        _runtime_uri = (
            Resource(
                runtime_uri,
                ResourceType.runtime,
                project=project_uri,
                refine=True,
            )
            if runtime_uri
            else None
        )
        r = crm.do_http_request(
            f"/project/{project_uri.name}/job",
            method=HTTPMethod.POST,
            instance=project_uri.instance,
            data=json.dumps(
                {
                    "modelVersionUrl": _model_uri.info().get("versionId")
                    or _model_uri.version,
                    "datasetVersionUrls": ",".join(
                        [
                            str(i.info().get("versionId") or i.version)
                            for i in _dataset_uris
                        ]
                    ),
                    "runtimeVersionUrl": _runtime_uri.info().get("versionId")
                    or _runtime_uri.version
                    if _runtime_uri
                    else "",
                    "resourcePool": resource_pool,
                    "handler": run_handler,
                }
            ),
        )
        if r.status_code == HTTPStatus.OK:
            return True, r.json()["data"]
        else:
            return False, r.json()["message"]
