from __future__ import annotations

import os
import copy
import json
import shutil
import typing as t
import tarfile
from abc import ABCMeta, abstractmethod
from pathlib import Path
from collections import defaultdict

import yaml
from fs import open_fs
from loguru import logger
from fs.walk import Walker

from starwhale import PipelineHandler
from starwhale.utils import console, now_str, load_yaml, gen_uniq_version
from starwhale.consts import (
    FileDesc,
    FileFlag,
    FileNode,
    RunStatus,
    CREATED_AT_KEY,
    SWMP_SRC_FNAME,
    DefaultYAMLName,
    SW_AUTO_DIRNAME,
    DEFAULT_PAGE_IDX,
    DEFAULT_PAGE_SIZE,
    SW_IGNORE_FILE_NAME,
    DEFAULT_MANIFEST_NAME,
    SW_EVALUATION_EXAMPLE_DIR,
    DEFAULT_STARWHALE_API_VERSION,
    EVALUATION_SVC_META_FILE_NAME,
    DEFAULT_EVALUATION_JOBS_FILE_NAME,
    EVALUATION_PANEL_LAYOUT_JSON_FILE_NAME,
    EVALUATION_PANEL_LAYOUT_YAML_FILE_NAME,
    DEFAULT_FILE_SIZE_THRESHOLD_TO_TAR_IN_MODEL, DEFAULT_FINETUNE_JOBS_FILE_NAME, DEFAULT_FINETUNE_JOB_NAME,
    DEFAULT_EVALUATION_JOB_NAME,
)
from starwhale.base.tag import StandaloneTag
from starwhale.base.uri import URI
from starwhale.utils.fs import (
    move_dir,
    copy_file,
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
from starwhale.utils.error import NoSupportError, FileFormatError
from starwhale.api._impl.job import generate_jobs_yaml
from starwhale.core.job.step import Step
from starwhale.utils.progress import run_with_progress_bar
from starwhale.base.blob.store import LocalFileStore
from starwhale.core.eval.store import EvaluationStorage
from starwhale.core.model.copy import ModelCopy
from starwhale.core.job.context import Context
from starwhale.core.model.store import ModelStorage
from starwhale.api._impl.service import Hijack
from starwhale.core.job.scheduler import Scheduler


class ModelRunConfig(ASDictMixin):

    # TODO: use attr to tune class
    def __init__(
        self,
        handler: str,
        runtime: str = "",
        envs: t.Union[t.List[str], None] = None,
        **kw: t.Any,
    ):
        self.handler = handler.strip()
        self.runtime = runtime.strip()
        self.envs = envs or []
        self.kw = kw

        self._do_validate()

    def _do_validate(self) -> None:
        if not self.handler:
            raise FileFormatError("need ppl field")

    def __str__(self) -> str:
        return f"Model Run Config: ppl -> {self.handler}"

    def __repr__(self) -> str:
        return f"Model Run Config: ppl -> {self.handler}, runtime -> {self.runtime}"


class ModelConfig(ASDictMixin):

    # TODO: use attr to tune class
    def __init__(
        self,
        name: str,
        run: t.Dict[str, t.Any],
        desc: str = "",
        tag: t.Optional[t.List[str]] = None,
        version: str = DEFAULT_STARWHALE_API_VERSION,
        **kw: t.Any,
    ):
        # TODO: format model name
        self.name = name
        # TODO: support artifacts: local or remote
        self.run = ModelRunConfig(**run)
        self.desc = desc
        self.tag = tag or []
        self.version = version
        self.kw = kw

    @classmethod
    def create_by_yaml(cls, path: Path) -> ModelConfig:
        c = load_yaml(path)
        return cls(**c)

    def __str__(self) -> str:
        return f"Model Config: {self.name}"

    def __repr__(self) -> str:
        return f"Model Config: name -> {self.name}"


class Model(BaseBundle, metaclass=ABCMeta):
    def __str__(self) -> str:
        return f"Starwhale Model: {self.uri}"

    def eval(self) -> None:
        pass

    def ppl(
        self, workdir: Path, yaml_name: str = DefaultYAMLName.MODEL, **kw: t.Any
    ) -> None:
        pass

    def cmp(
        self, workdir: Path, yaml_name: str = DefaultYAMLName.MODEL, **kw: t.Any
    ) -> None:
        pass

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

    def _render_eval_layout(self, workdir: Path) -> None:
        # render eval layout
        eval_layout = workdir / SW_AUTO_DIRNAME / EVALUATION_PANEL_LAYOUT_YAML_FILE_NAME
        if eval_layout.exists():
            content = load_yaml(eval_layout)
            dst = self.store.hidden_sw_dir / EVALUATION_PANEL_LAYOUT_JSON_FILE_NAME
            ensure_file(dst, json.dumps(content), parents=True)

    @classmethod
    def eval_user_handler(
        cls,
        project: str,
        version: str,
        workdir: Path,
        dataset_uris: t.List[str],
        model_yaml_name: str = DefaultYAMLName.MODEL,
        job_name: str = DEFAULT_EVALUATION_JOB_NAME,
        step_name: str = "",
        task_index: t.Optional[int] = None,
        task_num: int = 0,
        base_info: t.Optional[t.Dict[str, t.Any]] = None,
    ) -> None:
        base_info = base_info or {}
        _manifest: t.Dict[str, t.Any] = {
            CREATED_AT_KEY: now_str(),
            "status": RunStatus.START,
            "step": step_name,
            "task_index": task_index,
            "task_num": task_num,
        }
        _model_config = cls.load_model_config(workdir / model_yaml_name, workdir)

        if not version:
            version = gen_uniq_version()

        _project_uri = URI(project, expected_type=URIType.PROJECT)
        _run_dir = EvaluationStorage.local_run_dir(_project_uri.project, version)
        ensure_dir(_run_dir)

        yaml_path = workdir / SW_AUTO_DIRNAME / DEFAULT_EVALUATION_JOBS_FILE_NAME

        if not yaml_path.exists():
            # do not auto generate eval_job.yaml in the user workdir
            yaml_path = EvaluationHandlerParser(
                workdir=workdir,
                handler=_model_config.run.handler,
                target_dir=_run_dir,
            ).run()

        logger.debug(f"parse job from yaml:{yaml_path}")

        console.print(
            f":hourglass_not_done: start to run evaluation[{job_name}:{version}]..."
        )
        _scheduler = Scheduler(
            project=_project_uri.project,
            version=version,
            workdir=workdir,
            dataset_uris=dataset_uris,
            steps=Step.get_steps_from_yaml(job_name, yaml_path),
        )
        _status = RunStatus.START
        try:
            _results = _scheduler.run(
                step_name=step_name, task_index=task_index, task_num=task_num
            )
            _status = RunStatus.SUCCESS

            exceptions: t.List[Exception] = []
            for _r in _results:
                for _tr in _r.task_results:
                    if _tr.exception:
                        exceptions.append(_tr.exception)
            if exceptions:
                raise Exception(*exceptions)

            logger.debug(
                f"-->[Finished] evaluation[{job_name}:{version}] execute finished, results info:{_results}"
            )
        except Exception as e:
            _status = RunStatus.FAILED
            _manifest["error_message"] = str(e)
            logger.error(
                f"-->[Failed] evaluation[{job_name}:{version}] execute failed, error info:{e}"
            )
            raise
        finally:
            _manifest.update(
                {
                    **dict(
                        version=version,
                        project=_project_uri.project,
                        model_dir=str(workdir),
                        datasets=list(dataset_uris),
                        status=_status,
                        finished_at=now_str(),
                    ),
                    **base_info,
                }
            )
            _f = _run_dir / DEFAULT_MANIFEST_NAME
            ensure_file(_f, yaml.safe_dump(_manifest, default_flow_style=False))

            console.print(
                f":{100 if _status == RunStatus.SUCCESS else 'broken_heart'}: finish run, {_status}!"
            )

    @classmethod
    def fine_tune(
        cls,
        project: str,
        workdir: Path,
        dataset_uris: t.List[str],
        model_yaml_name: str = DefaultYAMLName.MODEL,
        job_name: str = DEFAULT_FINETUNE_JOB_NAME,
    ) -> None:
        _model_config = cls.load_model_config(workdir / model_yaml_name, workdir)

        _project_uri = URI(project, expected_type=URIType.PROJECT)

        yaml_path = workdir / SW_AUTO_DIRNAME / DEFAULT_FINETUNE_JOBS_FILE_NAME

        if not yaml_path.exists():
            # do not auto generate eval_job.yaml in the user workdir
            yaml_path = FinetuneHandlerParser(
                workdir=workdir,
                handler=_model_config.run.handler,
                target_dir=workdir,  # todo use tmp?
            ).run()

        logger.debug(f"parse fine tune job from yaml:{yaml_path}")
        version = gen_uniq_version()
        console.print(
            f":hourglass_not_done: start to run fine-tune[{job_name}:{version}]..."
        )
        _scheduler = Scheduler(
            project=_project_uri.project,
            version=version,
            workdir=workdir,
            dataset_uris=dataset_uris,
            steps=Step.get_steps_from_yaml(job_name, yaml_path),
        )
        _status = RunStatus.START
        try:
            _results = _scheduler.run()
            _status = RunStatus.SUCCESS

            exceptions: t.List[Exception] = []
            for _r in _results:
                for _tr in _r.task_results:
                    if _tr.exception:
                        exceptions.append(_tr.exception)
            if exceptions:
                raise Exception(*exceptions)

            logger.debug(
                f"-->[Finished] fine-tune[{job_name}:{version}] execute finished, results info:{_results}"
            )
        except Exception as e:
            _status = RunStatus.FAILED
            logger.error(
                f"-->[Failed] fine-tune[{job_name}:{version}] execute failed, error info:{e}"
            )
            raise
        finally:
            console.print(
                f":{100 if _status == RunStatus.SUCCESS else 'broken_heart'}: finish run, {_status}!"
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
        _manifest = self._get_bundle_info()
        _store = self.store
        _om = {}
        yaml_path = _store.hidden_sw_dir / DEFAULT_EVALUATION_JOBS_FILE_NAME
        if _store.snapshot_workdir.exists():
            if yaml_path.exists():
                _om = load_yaml(yaml_path)
            else:
                logger.warning("step_spec not found in model snapshot_workdir")
        else:
            logger.warning("step_spec not found in model")
        if _om:
            _manifest["step_spec"] = _om
        return _manifest

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
        yaml_path = Path(kw.get("yaml_path", workdir / DefaultYAMLName.MODEL))
        _model_config = self.load_model_config(yaml_path, workdir)

        logger.debug(f"build workdir:{workdir}")

        operations = [
            (self._gen_version, 5, "gen version"),
            (self._prepare_snapshot, 5, "prepare snapshot"),
            (
                self._copy_src,
                15,
                "copy src",
                dict(workdir=workdir, yaml_path=yaml_path),
            ),
            (
                EvaluationHandlerParser(
                    workdir=workdir,
                    handler=_model_config.run.handler,
                    target_dir=self.store.src_dir,
                ).run,
                5,
                "generate eval jobs",
                dict(raise_err=True),
            ),
            (
                FinetuneHandlerParser(
                    workdir=workdir,
                    handler=_model_config.run.handler,
                    target_dir=self.store.src_dir,
                ).run,
                5,
                "generate fine tune jobs",
                dict(raise_err=False),
            ),
            (
                ServeHandlerParser(
                    workdir=workdir,
                    handler=_model_config.run.handler,
                    target_dir=self.store.src_dir,
                ).run,
                10,
                "generate model serving",
                dict(raise_err=True),
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

    def _copy_src(self, workdir: Path, yaml_path: Path) -> None:
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

        # TODO: use an internal protected model yaml path
        # make sure model.yaml exists, prevent using the wrong config when running in the cloud
        target_yaml_path = self.store.src_dir / DefaultYAMLName.MODEL
        copy_file(yaml_path, target_yaml_path)

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
        svc = ServeHandlerParser.get_service(_model_config.run.handler, workdir)
        svc.serve(host, port, _model_config.name)


class HandlerParser:
    sw_dir: str = SW_AUTO_DIRNAME

    def __init__(self, workdir: Path, handler: t.Any, target_dir: Path) -> None:
        self.workdir = workdir
        self.handler = handler
        self.target_dir = target_dir

    @abstractmethod
    def run(self, raise_err: bool = True) -> t.Any:
        ...


class EvaluationHandlerParser(HandlerParser):
    def run(self, raise_err: bool = True) -> t.Any:
        try:
            yaml_path = self.target_dir / self.sw_dir / DEFAULT_EVALUATION_JOBS_FILE_NAME
            generate_jobs_yaml(
                run_handler=self.handler,
                workdir=self.workdir,
                yaml_path=yaml_path,
            )
            return yaml_path
        except Exception as e:
            logger.error("error in evaluation handler parse processing", e)
            if raise_err:
                raise e


class ServeHandlerParser(HandlerParser):
    @staticmethod
    def get_service(
        module: str, pkg: Path, hijack: t.Optional[Hijack] = None
    ) -> Service:
        module, _, attr = module.partition(":")
        m = load_module(module, pkg)
        apis = dict()
        ins: t.Any = None
        svc: t.Optional[Service] = None

        # TODO: refine this ugly ad hoc
        Context.set_runtime_context(
            Context(pkg, version="-1", project="tmp-project-for-build")
        )

        # TODO: check duplication
        for k, v in m.__dict__.items():
            if isinstance(v, Service):
                apis.update(v.apis)
                # use Service in module
                svc = v
        if attr:
            cls = getattr(m, attr)
            ins = cls()
            if isinstance(ins, PipelineHandler):
                apis.update(ins.svc.apis)

        from starwhale.api._impl.service import internal_api_list

        apis.update(internal_api_list())

        # check if we need to instance the model when using custom handler
        if ins is None:
            for i in apis.values():
                fn = i.func.__qualname__
                # TODO: support deep path classes (also modules)
                if "." in fn:
                    ins = getattr(m, fn.split(".", 1)[0])()
                    break

        if svc is None:
            svc = Service()
        for api in apis.values():
            svc.add_api_instance(api)
        svc.api_instance = ins
        svc.hijack = hijack
        return svc

    def run(self, raise_err: bool = True) -> t.Any:
        try:
            rc_dir = f"{self.sw_dir}/{SW_EVALUATION_EXAMPLE_DIR}"
            # render spec
            svc = ServeHandlerParser.get_service(self.handler, self.workdir, hijack=Hijack(True, rc_dir))
            file = self.target_dir / self.sw_dir / EVALUATION_SVC_META_FILE_NAME
            ensure_file(file, json.dumps(svc.get_spec(), indent=4), parents=True)

            if len(svc.example_resources) == 0:
                return

            # check duplicate file names, do not support using examples with same name in different dir
            names = set([os.path.basename(i) for i in svc.example_resources])
            if len(names) != len(svc.example_resources):
                raise NoSupportError("duplicate file names in examples")

            # copy example resources for online evaluation in server instance
            dst = self.target_dir / self.sw_dir / SW_EVALUATION_EXAMPLE_DIR
            ensure_dir(dst)
            for f in svc.example_resources:
                shutil.copy2(f, dst)
        except Exception as e:
            logger.error("error in serve handler parse processing", e)
            if raise_err:
                raise e


class FinetuneHandlerParser(HandlerParser):
    def run(self, raise_err: bool = True) -> t.Any:
        try:
            yaml_path = self.target_dir / self.sw_dir / DEFAULT_FINETUNE_JOBS_FILE_NAME
            generate_jobs_yaml(
                job_name=DEFAULT_FINETUNE_JOB_NAME,
                run_handler=self.handler,
                workdir=self.workdir,
                yaml_path=yaml_path,
            )
            return yaml_path
        except Exception as e:
            logger.error("error in fine tune handler parse processing", e)
            if raise_err:
                raise e


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
