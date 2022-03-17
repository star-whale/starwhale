import yaml
import os
import typing as t
from pathlib import Path

from loguru import logger

from starwhale.utils.error import FileTypeError, FileFormatError
from starwhale.utils.obj import Dict2Obj
from starwhale.utils import gen_uniq_version
from starwhale.utils.config import load_swcli_config
from starwhale.utils.fs import ensure_dir, ensure_file
from starwhale.consts import DEFAULT_STARWHALE_API_VERSION


class ModelConfig(object):

    #TODO: use attr to tune class
    def __init__(self, name: str, model: t.List[str],
        config: t.List[str], run: dict, desc: str = "",
        tag: t.List[str] = [], version: str=DEFAULT_STARWHALE_API_VERSION):

        #TODO: format model name
        self.name = name
        self.model = model
        self.config = config
        #TODO: define model-run class
        self.run = Dict2Obj(run)
        self.desc = desc
        self.tag = tag
        self.version = version

        self._validator()

    def _validator(self):
        #TODO: use attr validator
        if not self.model:
            raise FileFormatError("need at least one model")

        if (hasattr(self.run, "ppl") or
            not (hasattr(self.run, "runtime") or hasattr(self.run, "base_image"))):
            raise FileFormatError("run need ppl and runtime/base_image field")

        #TODO: add more validation
        #TODO: add name check

    @classmethod
    def create_by_yaml(cls, fpath: str) -> "ModelConfig":
        with open(fpath) as f:
             c = yaml.safe_load(f)

        return ModelConfig(**c)


class ModelPackageLocalStore(object):

    def __init__(self) -> None:
        pass

    @classmethod
    def list(cls):
        #TODO: add filter for list
        pass

    @classmethod
    def push(cls):
        pass

    @classmethod
    def pull(cls):
        pass


class ModelPackage(object):

    def __init__(self, workdir: str, model_yaml_fpath: str) -> None:
        self.workdir = workdir
        self._swmp_config = self.load_model_config(model_yaml_fpath)
        self._swcli_config = load_swcli_config()
        self._swcli_root = Path(self._swcli_config["storage"]["root"])
        self._swcli_workdir = self._swcli_root / "workdir"
        self._swcli_pkg = self._swcli_root / "pkg"

        self._snapshot_workdir = None
        self._version = None
        self._snapshot = None
        self._name = self._swcli_config["nam"]

    @classmethod
    def build(cls, workdir, mpath):
        mp = ModelPackage(workdir, mpath)
        mp._do_validate()
        mp._do_build()

    def _do_validate(self):
        sw = self._swmp_config
        workdir = Path(self.workdir)

        if not sw.model:
            raise FileFormatError("model yaml no model")

        for path in sw.model:
            if not (workdir / path).exists():
                raise FileFormatError(f"model - {path} not existed")

        if not (workdir / sw.run.ppl).exists():
            raise FileExistsError(f"run ppl - {sw.run.ppl} not existed")

        #TODO: add more model.yaml section validation
        #TODO: add 'swcli model check' cmd

    def _do_build(self):
        self._gen_version()
        self._prepare_snapshot()
        self._dump_dep()
        self._copy_src()
        self._render_manifest()
        self._make_swmp_tar()

    def _gen_version(self) -> None:
        if self._version:
            return
        self._version = gen_uniq_version(self._swmp_config.name)

    def _prepare_snapshot(self):
        ensure_dir(self._swcli_pkg)
        ensure_dir(self._swcli_workdir)

        self._snapshot_workdir = self._swcli_workdir / self._name / self._version
        ensure_dir(self._snapshot_workdir)
        ensure_dir(self._snapshot_workdir / "src")
        ensure_dir(self._snapshot_workdir / "dep" / "python")
        ensure_dir(self._snapshot_workdir / "dep" / "conda")

    def _dump_dep(self):
        pass

    def _copy_src(self):
        pass

    def _render_manifest(self):
        pass

    def _make_swmp_tar(self):
        pass

    def load_model_config(self, fpath: str) -> ModelConfig:
        if not os.path.exists(fpath):
            raise Exception(f"model.yaml {fpath} is not existed")

        if not fpath.endswith((".yaml", ".yml")):
            raise FileTypeError(f"{fpath} file type is not yaml|yml")

        return ModelConfig.create_by_yaml(fpath)