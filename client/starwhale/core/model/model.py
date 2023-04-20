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
from loguru import logger
from fs.walk import Walker

from starwhale.utils import (
    console,
    now_str,
    load_yaml,
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
    DEFAULT_PAGE_SIZE,
    SHORT_VERSION_CNT,
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
from starwhale.base.uri import URI
from starwhale.utils.fs import (
    move_dir,
    file_stat,
    ensure_dir,
    ensure_file,
    blake2b_file,
)
from starwhale.base.type import URIType, BundleType, InstanceType
from starwhale.base.cloud import CloudRequestMixed, CloudBundleModelMixin
from starwhale.base.mixin import ASDictMixin
from starwhale.utils.http import ignore_error
from starwhale.utils.load import load_module
from starwhale.api.service import Service
from starwhale.base.bundle import BaseBundle, LocalStorageBundleMixin
from starwhale.utils.error import NoSupportError
from starwhale.base.context import Context
from starwhale.api._impl.job import generate_jobs_yaml
from starwhale.base.scheduler import Step, Scheduler
from starwhale.core.job.store import JobStorage
from starwhale.utils.progress import run_with_progress_bar
from starwhale.base.blob.store import LocalFileStore
from starwhale.core.model.copy import ModelCopy
from starwhale.core.model.store import ModelStorage
from starwhale.api._impl.service import Hijack


@unique
class ModelInfoFilter(Enum):
    basic = "basic"
    model_yaml = "model_yaml"
    manifest = "manifest"
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
        return f"Model Run Config: handlers -> {self.modules}, envs -> {self.envs}"

    def do_validate(self) -> None:
        # TODO: validate handler format
        if not self.modules:
            raise ValueError("model run config must have at least one handler")


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
    def get_model(cls, uri: URI) -> Model:
        _cls = cls._get_cls(uri)
        return _cls(uri)

    @classmethod
    def _get_cls(cls, uri: URI) -> t.Union[t.Type[StandaloneModel], t.Type[CloudModel]]:  # type: ignore
        if uri.instance_type == InstanceType.STANDALONE:
            return StandaloneModel
        elif uri.instance_type == InstanceType.CLOUD:
            return CloudModel
        else:
            raise NoSupportError(f"model uri:{uri}")

    @classmethod
    def copy(
        cls,
        src_uri: str,
        dest_uri: str,
        force: bool = False,
        dest_local_project_uri: str = "",
    ) -> None:
        bc = ModelCopy(
            src_uri,
            dest_uri,
            URIType.MODEL,
            force,
            dest_local_project_uri=dest_local_project_uri,
        )
        bc.do()

    def diff(self, compare_uri: URI) -> t.Dict[str, t.Any]:
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
    def __init__(self, uri: URI) -> None:
        super().__init__(uri)
        self.typ = InstanceType.STANDALONE
        self.store = ModelStorage(uri)
        self.tag = StandaloneTag(uri)
        self._manifest: t.Dict[str, t.Any] = {}  # TODO: use manifest class
        self.models: t.List[t.Dict[str, t.Any]] = []
        self.sources: t.List[t.Dict[str, t.Any]] = []
        self.yaml_name = DefaultYAMLName.MODEL
        self._version = uri.object.version
        self._object_store = LocalFileStore()

    def list_tags(self) -> t.List[str]:
        return self.tag.list()

    def add_tags(self, tags: t.List[str], ignore_errors: bool = False) -> None:
        self.tag.add(tags, ignore_errors)

    def remove_tags(self, tags: t.List[str], ignore_errors: bool = False) -> None:
        self.tag.remove(tags, ignore_errors)

    def _gen_model_serving(self, search_modules: t.List[str], workdir: Path) -> None:
        rc_dir = str(
            self.store.hidden_sw_dir.relative_to(self.store.snapshot_workdir)
            / SW_EVALUATION_EXAMPLE_DIR
        )
        # render spec
        svc = self._get_service(search_modules, workdir, hijack=Hijack(True, rc_dir))
        file = self.store.hidden_sw_dir / EVALUATION_SVC_META_FILE_NAME
        ensure_file(file, json.dumps(svc.get_spec(), indent=4), parents=True)

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
    ) -> None:
        external_info = external_info or {}
        dataset_uris = dataset_uris or []
        scheduler_run_args = scheduler_run_args or {}
        version = version or gen_uniq_version()

        job_yaml_path = model_src_dir / SW_AUTO_DIRNAME / DEFAULT_JOBS_FILE_NAME
        if not job_yaml_path.exists():
            generate_jobs_yaml(
                search_modules=model_config.run.modules,
                package_dir=model_src_dir,
                yaml_path=job_yaml_path,
            )

        console.print(
            f":hourglass_not_done: start to run model, handler:{run_handler} ..."
        )
        scheduler = Scheduler(
            project=project,
            version=version,
            workdir=model_src_dir,
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

            logger.debug(
                f"-->[Finished] run[{version[:SHORT_VERSION_CNT]}] execute finished, results info:{results}"
            )
        except Exception as e:
            scheduler_status = RunStatus.FAILED
            error_message = str(e)
            logger.error(
                f"-->[Failed] run[{version[:SHORT_VERSION_CNT]}] execute failed, error info:{e}"
            )
            raise
        finally:
            _manifest: t.Dict[str, t.Any] = {
                CREATED_AT_KEY: start,
                "scheduler_run_args": scheduler_run_args,
                "version": version,
                "project": project,
                "model_src_dir": str(model_src_dir),
                "datasets": dataset_uris,
                "status": scheduler_status,
                "error_message": error_message,
                "finished_at": now_str(),
                **external_info,
            }

            _dir = JobStorage.local_run_dir(project, version)
            ensure_file(
                _dir / DEFAULT_MANIFEST_NAME,
                yaml.safe_dump(_manifest, default_flow_style=False),
                parents=True,
            )

            console.print(
                f":{100 if scheduler_status == RunStatus.SUCCESS else 'broken_heart'}: finish run, {scheduler_status}!"
            )

    def diff(self, compare_uri: URI) -> t.Dict[str, t.Any]:
        """
        - added: a node that exists in compare but not in base
        - deleted: a node that not exists in compare but in base
        - updated: a node that exists in both of base and compare but signature is different
        - unchanged: a node that exists in both of base and compare, and signature is same
        :param compare_uri:
        :return: diff info
        """
        # TODO use remote get model info for cloud
        if compare_uri.instance_type != InstanceType.STANDALONE:
            raise NoSupportError(
                f"only support standalone uri, but compare_uri({compare_uri}) is for cloud instance"
            )
        if self.uri.object.name != compare_uri.object.name:
            raise NoSupportError(
                f"only support two versions diff in one model, base model:{self.uri}, compare model:{compare_uri}"
            )
        _compare_model = StandaloneModel(compare_uri)
        base_file_maps = resource_to_file_node(
            files=self.store.manifest["resources"],
            parent_path=self.store.snapshot_workdir,
        )
        compare_file_maps = resource_to_file_node(
            files=_compare_model.store.manifest["resources"],
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

        manifest = self.store.manifest
        ret["basic"] = {
            "name": self.uri.object.name,
            "version": self.uri.object.version,
            "project": self.uri.project,
            "path": str(self.store.bundle_path),
            "tags": StandaloneTag(self.uri).list(),
            "created_at": manifest.get(CREATED_AT_KEY, ""),
        }

        ret["manifest"] = manifest
        ret["model_yaml"] = (
            self.store.hidden_sw_dir / DefaultYAMLName.MODEL
        ).read_text()
        job_yaml = load_yaml(self.store.hidden_sw_dir / DEFAULT_JOBS_FILE_NAME)
        ret["handlers"] = job_yaml
        ret["files"] = manifest["resources"]
        ret["basic"]["handlers"] = sorted(job_yaml.keys())
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

            _manifest = load_yaml(_manifest_path)

            _r.append(
                dict(
                    name=self.name,
                    version=_bf.version,
                    path=str(_bf.path.resolve()),
                    tags=_bf.tags,
                    created_at=_manifest[CREATED_AT_KEY],
                    size=_bf.path.stat().st_size,
                )
            )
        return _r

    def remove(self, force: bool = False) -> t.Tuple[bool, str]:
        return self._do_remove(force)

    def recover(self, force: bool = False) -> t.Tuple[bool, str]:
        # TODO: support short version to recover, today only support full-version
        dest_path = (
            self.store.bundle_dir / f"{self.uri.object.version}{BundleType.MODEL}"
        )
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
        project_uri: URI,
        page: int = DEFAULT_PAGE_IDX,
        size: int = DEFAULT_PAGE_SIZE,
        filters: t.Optional[t.Union[t.Dict[str, t.Any], t.List[str]]] = None,
    ) -> t.Tuple[t.Dict[str, t.Any], t.Dict[str, t.Any]]:
        filters = filters or {}
        rs = defaultdict(list)
        for _bf in ModelStorage.iter_all_bundles(
            project_uri,
            bundle_type=BundleType.MODEL,
            uri_type=URIType.MODEL,
        ):
            if not cls.do_bundle_filter(_bf, filters):
                continue

            if _bf.path.is_file():
                # for origin swmp(tar)
                _manifest = ModelStorage.get_manifest_by_path(
                    _bf.path, BundleType.MODEL, URIType.MODEL
                )
            elif (_bf.path / DEFAULT_MANIFEST_NAME).exists():
                _manifest = load_yaml(_bf.path / DEFAULT_MANIFEST_NAME)
            else:
                continue
            rs[_bf.name].append(
                {
                    "name": _bf.name,
                    "version": _bf.version,
                    "path": str(_bf.path.absolute()),
                    "size": _bf.path.stat().st_size,
                    "is_removed": _bf.is_removed,
                    CREATED_AT_KEY: _manifest[CREATED_AT_KEY],
                    "tags": _bf.tags,
                }
            )
        return rs, {}

    def buildImpl(self, workdir: Path, **kw: t.Any) -> None:  # type: ignore[override]
        model_config: ModelConfig = kw["model_config"]
        logger.debug(f"build workdir:{workdir}")

        operations = [
            (self._gen_version, 5, "gen version"),
            (self._prepare_snapshot, 5, "prepare snapshot"),
            (
                self._copy_src,
                15,
                "copy src",
                dict(workdir=workdir, model_config=model_config),
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
                self._gen_model_serving,
                10,
                "generate model serving",
                dict(search_modules=model_config.run.modules, workdir=workdir),
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

    def _make_meta_tar(
        self, size_th_to_tar: int = DEFAULT_FILE_SIZE_THRESHOLD_TO_TAR_IN_MODEL
    ) -> None:
        w = Walker()
        src_fs = open_fs(str(self.store.src_dir.resolve()))
        with tarfile.open(self.store.snapshot_workdir / SWMP_SRC_FNAME, "w:") as tar:
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
                }
                if separate:
                    self.models.append(file_info)
                else:
                    tar.add(str(self.store.src_dir / sub_path), arcname=sub_path)
                    self.sources.append(file_info)

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
        logger.info("[step:prepare-snapshot]prepare model snapshot dirs...")

        ensure_dir(self.store.snapshot_workdir)
        ensure_dir(self.store.src_dir)

        # TODO: cleanup garbage dir
        # TODO: add lock/flag file for gc

        console.print(
            f":file_folder: workdir: [underline]{self.store.snapshot_workdir}[/]"
        )

    def _copy_src(self, workdir: Path, model_config: ModelConfig) -> None:
        logger.info(
            f"[step:copy]start to copy src {workdir} -> {self.store.src_dir} ..."
        )
        console.print(":thumbs_up: try to copy source code files...")

        excludes = None
        ignore = workdir / SW_IGNORE_FILE_NAME
        if ignore.exists():
            with open(ignore, "r") as f:
                excludes = [line.strip() for line in f.readlines()]
        self._object_store.copy_dir(
            str(workdir.resolve()), str(self.store.src_dir.resolve()), excludes=excludes
        )

        model_yaml = yaml.safe_dump(model_config.asdict(), default_flow_style=False)
        ensure_file(
            self.store.hidden_sw_dir / DefaultYAMLName.MODEL, model_yaml, parents=True
        )

        # make sure model.yaml exists, prevent using the wrong config when running in the cloud
        ensure_file(
            self.store.src_dir / DefaultYAMLName.MODEL, model_yaml, parents=True
        )

        logger.info("[step:copy]finish copy files")

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
        model_yaml: str,
        workdir: Path,
        host: str,
        port: int,
    ) -> None:
        _model_config = cls.load_model_config(workdir / model_yaml, workdir)
        svc = cls._get_service(_model_config.run.modules, workdir)
        svc.serve(host, port, _model_config.name)


class CloudModel(CloudBundleModelMixin, Model):
    def __init__(self, uri: URI) -> None:
        super().__init__(uri)
        self.typ = InstanceType.CLOUD

    @classmethod
    @ignore_error(({}, {}))
    def list(
        cls,
        project_uri: URI,
        page: int = DEFAULT_PAGE_IDX,
        size: int = DEFAULT_PAGE_SIZE,
        filter_dict: t.Optional[t.Dict[str, t.Any]] = None,
    ) -> t.Tuple[t.Dict[str, t.Any], t.Dict[str, t.Any]]:
        filter_dict = filter_dict or {}
        crm = CloudRequestMixed()
        return crm._fetch_bundle_all_list(
            project_uri, URIType.MODEL, page, size, filter_dict
        )

    def build(self, *args: t.Any, **kwargs: t.Any) -> None:
        raise NoSupportError("no support build model in the cloud instance")

    def diff(self, compare_uri: URI) -> t.Dict[str, t.Any]:
        raise NoSupportError("no support model diff in the cloud instance")

    @classmethod
    def run(
        cls,
        project_uri: URI,
        model_uri: str,
        dataset_uris: t.List[str],
        runtime_uri: str,
        run_handler: str | int,
        resource_pool: str = "default",
    ) -> t.Tuple[bool, str]:
        crm = CloudRequestMixed()

        r = crm.do_http_request(
            f"/project/{project_uri.project}/job",
            method=HTTPMethod.POST,
            instance_uri=project_uri,
            data=json.dumps(
                {
                    "modelVersionUrl": model_uri,
                    "datasetVersionUrls": ",".join([str(i) for i in dataset_uris]),
                    "runtimeVersionUrl": runtime_uri,
                    "resourcePool": resource_pool,
                    "handler": run_handler,
                }
            ),
        )
        if r.status_code == HTTPStatus.OK:
            return True, r.json()["data"]
        else:
            return False, r.json()["message"]
