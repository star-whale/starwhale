from __future__ import annotations

import os
import copy
import json
import typing as t
import tarfile
from abc import ABCMeta
from pathlib import Path
from collections import defaultdict

import yaml
from fs import open_fs
from loguru import logger
from fs.copy import copy_fs, copy_file
from fs.walk import Walker
from fs.tarfs import TarFS

from starwhale import PipelineHandler
from starwhale.utils import console, now_str, load_yaml, gen_uniq_version
from starwhale.consts import (
    FileDesc,
    FileFlag,
    FileNode,
    SWMP_SRC_FNAME,
    DefaultYAMLName,
    EvalHandlerType,
    DEFAULT_PAGE_IDX,
    DEFAULT_PAGE_SIZE,
    DEFAULT_COPY_WORKERS,
    DEFAULT_MANIFEST_NAME,
    DEFAULT_EVALUATION_PIPELINE,
    DEFAULT_EVALUATION_JOBS_FNAME,
    DEFAULT_STARWHALE_API_VERSION,
    DEFAULT_EVALUATION_SVC_META_FNAME,
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
from starwhale.utils.error import NoSupportError, FileFormatError
from starwhale.api._impl.job import Parser, Context, context_holder
from starwhale.core.job.model import STATUS, Generator
from starwhale.utils.progress import run_with_progress_bar
from starwhale.core.eval.store import EvaluationStorage
from starwhale.core.model.copy import ModelCopy
from starwhale.core.model.store import ModelStorage
from starwhale.core.job.scheduler import Scheduler


class ModelRunConfig(ASDictMixin):

    # TODO: use attr to tune class
    def __init__(
        self,
        handler: str,
        type: str = EvalHandlerType.DEFAULT,
        runtime: str = "",
        pkg_data: t.Union[t.List[str], None] = None,
        exclude_pkg_data: t.Union[t.List[str], None] = None,
        envs: t.Union[t.List[str], None] = None,
        **kw: t.Any,
    ):
        self.handler = handler.strip()
        self.typ = type
        self.runtime = runtime.strip()
        self.pkg_data = pkg_data or []
        self.exclude_pkg_data = exclude_pkg_data or []
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
        model: t.List[str],
        run: t.Dict[str, t.Any],
        config: t.List[str] = [],
        desc: str = "",
        tag: t.List[str] = [],
        version: str = DEFAULT_STARWHALE_API_VERSION,
        **kw: t.Any,
    ):
        # TODO: format model name
        self.name = name
        self.model = model or []
        self.config = config or []
        # TODO: support artifacts: local or remote
        self.run = ModelRunConfig(**run)
        self.desc = desc
        self.tag = tag
        self.version = version
        self.kw = kw

        self._do_validate()

    def _do_validate(self) -> None:
        # TODO: use attr validator
        if not self.model:
            raise FileFormatError("need at least one model")

        # TODO: add more validation
        # TODO: add name check

    @classmethod
    def create_by_yaml(cls, path: Path) -> ModelConfig:
        c = load_yaml(path)
        return cls(**c)

    def __str__(self) -> str:
        return f"Model Config: {self.name}"

    def __repr__(self) -> str:
        return f"Model Config: name -> {self.name}, model-> {self.model}"


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
        self._manifest: t.Dict[str, t.Any] = {}  # TODO: use manifest classget_conda_env
        self.models: t.List[t.Dict[str, t.Any]] = []
        self.sources: t.List[t.Dict[str, t.Any]] = []
        self.yaml_name = DefaultYAMLName.MODEL
        self._version = uri.object.version

    def list_tags(self) -> t.List[str]:
        return self.tag.list()

    def add_tags(self, tags: t.List[str], ignore_errors: bool = False) -> None:
        self.tag.add(tags, ignore_errors)

    def remove_tags(self, tags: t.List[str], ignore_errors: bool = False) -> None:
        self.tag.remove(tags, ignore_errors)

    def _gen_steps(self, typ: str, ppl: str, workdir: Path) -> None:
        d = self.store.src_dir
        svc = self._get_service(ppl, workdir)
        _f = d / DEFAULT_EVALUATION_SVC_META_FNAME
        ensure_file(_f, json.dumps(svc.get_spec(), indent=4))
        if typ == EvalHandlerType.DEFAULT:
            # use default
            ppl = DEFAULT_EVALUATION_PIPELINE
        _f = d / DEFAULT_EVALUATION_JOBS_FNAME
        logger.debug(f"job ppl path:{_f}, ppl is {ppl}")
        Parser.generate_job_yaml(ppl, workdir, _f)

    @staticmethod
    def _get_service(module: str, pkg: Path) -> Service:
        module, _, attr = module.partition(":")
        m = load_module(module, pkg)
        apis = dict()
        ins: t.Any = None
        svc: t.Optional[Service] = None

        # TODO: refine this ugly ad hoc
        context_holder.context = Context(
            pkg, version="-1", project="tmp-project-for-build"
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
        return svc

    @classmethod
    def get_pipeline_handler(
        cls,
        workdir: Path,
        yaml_name: str = DefaultYAMLName.MODEL,
    ) -> str:
        _mp = workdir / yaml_name
        _model_config = cls.load_model_config(_mp)
        return cls._get_module(_model_config)

    @classmethod
    def eval_user_handler(
        cls,
        project: str,
        version: str,
        workdir: Path,
        dataset_uris: t.List[str],
        model_yaml_name: str = DefaultYAMLName.MODEL,
        job_name: str = "default",
        step_name: str = "",
        task_index: int = 0,
        task_num: int = 0,
        base_info: t.Dict[str, t.Any] = {},
    ) -> None:
        # init manifest
        _manifest: t.Dict[str, t.Any] = {
            "created_at": now_str(),
            "status": STATUS.START,
            "step": step_name,
            "task_index": task_index,
            "task_num": task_num,
        }
        # load model config by yaml
        _model_config = cls.load_model_config(workdir / model_yaml_name)

        if not version:
            version = gen_uniq_version()

        _project_uri = URI(project, expected_type=URIType.PROJECT)
        _run_dir = EvaluationStorage.local_run_dir(_project_uri.project, version)
        ensure_dir(_run_dir)

        _module = cls._get_module(_model_config)
        _yaml_path = str(workdir / DEFAULT_EVALUATION_JOBS_FNAME)

        # generate if not exists
        if not os.path.exists(_yaml_path):
            _new_yaml_path = _run_dir / DEFAULT_EVALUATION_JOBS_FNAME
            Parser.generate_job_yaml(_module, workdir, _new_yaml_path)
            _yaml_path = str(_new_yaml_path)

        # parse job steps from yaml
        logger.debug(f"parse job from yaml:{_yaml_path}")
        _jobs = Generator.generate_job_from_yaml(_yaml_path)

        if job_name not in _jobs:
            raise RuntimeError(f"job:{job_name} not found")

        _steps = _jobs[job_name]

        console.print(f":hourglass_not_done: start to evaluation[{version}]...")
        _scheduler = Scheduler(
            project=_project_uri.project,
            version=version,
            module=_module,
            workdir=workdir,
            dataset_uris=dataset_uris,
            steps=_steps,
        )
        _status = STATUS.START
        try:
            if not step_name:
                _step_results = _scheduler.schedule()
            elif task_index < 0:
                _step_results = [
                    _scheduler.schedule_single_step(
                        step_name=step_name, task_num=task_num
                    )
                ]
            else:
                _step_results = [
                    _scheduler.schedule_single_task(step_name, task_index, task_num)
                ]

            logger.debug(f"job execute info:{_step_results}")
            _status = STATUS.SUCCESS

            exceptions: t.List[Exception] = []
            for _sr in _step_results:
                for _tr in _sr.task_results:
                    if _tr.exception:
                        exceptions.append(_tr.exception)
            if exceptions:
                raise Exception(*exceptions)
        except Exception as e:
            logger.error(f"job:{job_name} execute error:{e}")
            _status = STATUS.FAILED
            _manifest["error_message"] = str(e)
            raise
        finally:
            _manifest.update(
                {
                    **dict(
                        version=version,
                        project=_project_uri.project,
                        model=_model_config.model[0],
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
                f":{100 if _status == STATUS.SUCCESS else 'broken_heart'}: finish run, {_status}!"
            )

    @classmethod
    def _get_module(cls, _model_config: ModelConfig) -> str:
        if _model_config.run.typ == EvalHandlerType.DEFAULT:
            return DEFAULT_EVALUATION_PIPELINE
        else:
            return _model_config.run.handler

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
        if _store.snapshot_workdir.exists():
            pth = _store.src_dir / DEFAULT_EVALUATION_JOBS_FNAME
            if pth.exists():
                _om = load_yaml(pth)
            else:
                ignore_error("step_spec not found in model snapshot_workdir")
        elif _store.bundle_path.exists():
            if _store.bundle_path.is_file() and tarfile.is_tarfile(_store.bundle_path):
                with TarFS(str(_store.bundle_path)) as tar:
                    with tar.open("src/" + DEFAULT_EVALUATION_JOBS_FNAME) as f:
                        _om = yaml.safe_load(f)
            else:
                ignore_error(
                    "model bundle_path is not tarfile, step_spec not extracted"
                )
        else:
            ignore_error("step_spec not found in model")
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
                    created_at=_manifest["created_at"],
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
                    "created_at": _manifest["created_at"],
                    "tags": _bf.tags,
                }
            )
        return rs, {}

    def buildImpl(self, workdir: Path, **kw: t.Any) -> None:  # type: ignore[override]
        yaml_name = kw.get("yaml_name", DefaultYAMLName.MODEL)
        _mp = workdir / yaml_name
        _model_config = self.load_model_config(_mp)

        logger.debug(f"build workdir:{workdir}")

        operations = [
            (self._gen_version, 5, "gen version"),
            (self._prepare_snapshot, 5, "prepare snapshot"),
            (
                self._copy_src,
                15,
                "copy src",
                dict(workdir=workdir, yaml_name=yaml_name, model_config=_model_config),
            ),
            (
                self._gen_steps,
                5,
                "generate execute steps",
                dict(
                    typ=_model_config.run.typ,
                    ppl=_model_config.run.handler,
                    workdir=workdir,
                ),
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

    def _make_meta_tar(self) -> None:
        w = Walker(exclude=[f["name"] for f in self.models])
        src_fs = open_fs(str(self.store.src_dir.resolve()))
        with tarfile.open(self.store.snapshot_workdir / SWMP_SRC_FNAME, "w:") as tar:
            for f in w.files(src_fs):
                sub_path = f[1:]
                tar.add(str(self.store.src_dir / sub_path), arcname=sub_path)
                self.sources.append(
                    {
                        "name": os.path.basename(f),
                        "path": f"{self.store.src_dir_name}/{sub_path}",
                        "signature": blake2b_file(self.store.src_dir / sub_path),
                        "duplicate_check": False,
                        "desc": FileDesc.SRC.name,
                        "size": file_stat(self.store.src_dir / sub_path).st_size,
                    }
                )

    def _render_manifest(self) -> None:
        self._manifest["resources"] = self.models + self.sources
        super()._render_manifest()

    @classmethod
    def load_model_config(cls, yaml_path: Path) -> ModelConfig:
        cls._do_validate_yaml(yaml_path)
        _config = ModelConfig.create_by_yaml(yaml_path)

        if not _config.model:
            raise FileFormatError("model yaml no model")

        for _fpath in _config.model + _config.config:
            if not (yaml_path.parent / _fpath).exists():
                raise FileFormatError(
                    f"model - {yaml_path.parent / _fpath} is not existed"
                )

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

    def _copy_src(
        self, workdir: Path, yaml_name: str, model_config: ModelConfig
    ) -> None:
        logger.info(
            f"[step:copy]start to copy src {workdir} -> {self.store.src_dir} ..."
        )
        console.print(":thumbs_up: try to copy source code files...")
        _mc = model_config

        workdir_fs = open_fs(str(workdir.resolve()))
        snapshot_src_fs = open_fs(str(self.store.src_dir.resolve()))
        copy_fs(
            workdir_fs,
            snapshot_src_fs,
            walker=self._get_src_walker(
                workdir, _mc.run.pkg_data, _mc.run.exclude_pkg_data
            ),
            workers=DEFAULT_COPY_WORKERS,
        )

        for _fname in _mc.config:
            copy_file(workdir_fs, _fname, snapshot_src_fs, _fname)

        # rename model config if not default to make sure model.yaml exists
        # prevent using the wrong config when running in the cloud
        if yaml_name != DefaultYAMLName.MODEL:
            d = self.store.src_dir
            os.rename(d / yaml_name, d / DefaultYAMLName.MODEL)

        # TODO link models to unified storage
        for _fname in _mc.model:
            copy_file(workdir_fs, _fname, snapshot_src_fs, _fname)
            self.models.append(
                {
                    "name": os.path.basename(_fname),
                    "path": f"{self.store.src_dir_name}/{_fname}",
                    "signature": blake2b_file(self.store.src_dir / _fname),
                    "duplicate_check": True,
                    "desc": FileDesc.MODEL.name,
                    "size": file_stat(self.store.src_dir / _fname).st_size,
                }
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
        _model_config = cls.load_model_config(workdir / model_yaml)
        svc = cls._get_service(_model_config.run.handler, workdir)
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
