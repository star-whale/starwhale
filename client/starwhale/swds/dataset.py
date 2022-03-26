import typing as t
import yaml
from pathlib import Path
from collections import namedtuple
from datetime import datetime
import platform

from loguru import logger

from starwhale.utils.fs import (
    ensure_dir, ensure_file, blake2b_file,
    BLAKE2B_SIGNATURE_ALGO
)
from starwhale import __version__
from starwhale.utils import (
    convert_to_bytes, gen_uniq_version
)
from starwhale.utils.venv import dump_python_dep_env, detect_pip_req
from starwhale.utils.error import FileTypeError, NoSupportError
from starwhale.consts import (
    DEFAULT_STARWHALE_API_VERSION, FMT_DATETIME,
    DEFAULT_MANIFEST_NAME
)
from starwhale.utils.config import load_swcli_config


DS_PROCESS_MODE = namedtuple("DS_PROCESS_MODE", ["DEFINE", "GENERATE"])(
    "define", "generate"
)
D_DS_PROCESS_MODE = DS_PROCESS_MODE.GENERATE

D_FILE_VOLUME_SIZE = 64 * 1024 * 1024  # 64MB
D_ALIGNMENT_SIZE = 4 * 1024            # 4k for page cache
D_USER_BATCH_SIZE = 1

#TODO: use attr to tune code
class DataSetAttr(object):

    def __init__(self, volume_size: t.Union[int, str] = D_FILE_VOLUME_SIZE,
                 alignment_size: t.Union[int, str]= D_ALIGNMENT_SIZE,
                 batch_size: int = D_USER_BATCH_SIZE) -> None:
        self.batch_size = batch_size
        self.volume_size = convert_to_bytes(volume_size)
        self.batch_size = convert_to_bytes(batch_size)


#TODO: abstract base class from DataSetConfig and ModelConfig
#TODO: use attr to tune code
class DataSetConfig(object):

    def __init__(self, name: str, data_dir: str, process: str,
                 mode: str = D_DS_PROCESS_MODE,
                 pip_req: str = "",
                 tag: t.List[str] = [],
                 desc: str = "",
                 version: str = DEFAULT_STARWHALE_API_VERSION,
                 attr: dict= {},
                 ) -> None:
        self.name = name
        self.mode = mode
        self.data_dir = str(data_dir)
        self.process = process
        self.tag = tag
        self.desc = desc
        self.version = version
        self.pip_req = pip_req
        self.attr = DataSetAttr(**attr)

        self._validator()

    def _validator(self):
        if self.mode not in DS_PROCESS_MODE:
            raise NoSupportError(f"{self.mode} mode no support")

        if ":" not in self.process:
            raise Exception(f"please use module:class format, current is: {self.process}")

        #TODO: add more validator

    def __str__(self) -> str:
        return f"DataSet Config {self.name}"

    def __repr__(self) -> str:
        return f"DataSet Config {self.name}, mode:{self.mode}, data:{self.data_dir}"

    @classmethod
    def create_by_yaml(cls, fpath: t.Union[str, Path]) -> "DataSetConfig":
        fpath = Path(fpath)

        with fpath.open("r") as f:
            c = yaml.safe_load(f)

        return cls(**c)

#TODO: abstract base object for DataSet and ModelPackage
class DataSet(object):

    def __init__(self, workdir: str, ds_yaml_name: str, dry_run: bool=False) -> None:
        self.workdir = Path(workdir)
        self._dry_run = dry_run
        self._ds_yaml_name = ds_yaml_name
        self._ds_path = self.workdir / ds_yaml_name

        self._swcli_config = load_swcli_config()

        self._snapshot_workdir = Path()
        self._swds_config = self.load_dataset_config(self._ds_path)
        self._name = self._swds_config.name
        self._version = ""
        self._manifest = {}

    def __str__(self) -> str:
        return f"DataSet {self._name}"

    def __repr__(self) -> str:
        return f"DataSet {self._name} @{self.workdir}"

    def _do_validate(self):
        if not (self.workdir / self._swds_config.data_dir).exists():
            raise FileNotFoundError(f"{self._swds_config.data_dir} is not existed")

    @property
    def dataset_dir(self):
        return Path(self._swcli_config["storage"]["root"]) / "dataset"

    @logger.catch
    def _do_build(self):
        #TODO: design dataset layer mechanism
        #TODO: design uniq build steps for model build, swmp build
        self._gen_version()
        self._prepare_snapshot()
        self._call_build_swds()
        self._calculate_signature()
        self._dump_dep()
        self._render_manifest()

    def _calculate_signature(self):
        logger.info(f"[step:signature]try to calculate signature with {_algo} @ {self._data_dir}")
        _algo = BLAKE2B_SIGNATURE_ALGO
        _sign = dict()

        for c in self._data_dir.iterdir():
            if not c.is_file():
                continue
            _sign[c.name] = f"{_algo}:{blake2b_file(c)}"

        self._manifest["signature"] = _sign

        logger.info(f"[step:signature]finish calculate signature with {_algo} @ {_sign}")

    def _dump_dep(self):
        logger.info("[step:dump]dump conda or venv environment...")

        _manifest = dump_python_dep_env(
            dep_dir=self._snapshot_workdir / "dep",
            pip_req_fpath=detect_pip_req(self.workdir, self._swds_config.pip_req),
            skip_gen_env=True,  #TODO: add venv dump?
        )

        self._manifest["dep"] = _manifest

        logger.info("[step:dump]finish dump dep")

    def _call_build_swds(self):
        self._manifest["dataset_attr"] = self._swds_config.attr.__dict__
        self._manifest["mode"] = self._swds_config.mode

    def _render_manifest(self):
        self._manifest["build"] = dict(
            os=platform.system(),
            sw_version=__version__,
        )
        _f = self._snapshot_workdir / DEFAULT_MANIFEST_NAME
        ensure_file(_f, yaml.dump(self._manifest, default_flow_style=False))
        logger.info(f"[step:manifest]render manifest: {_f}")

    def _gen_version(self):
        if not self._version:
            self._version = gen_uniq_version()

        #TODO: abstract with ModelPackage
        self._manifest["version"] = self._version
        self._manifest["created_at"] = datetime.now().astimezone().strftime(FMT_DATETIME)
        logger.info(f"[step:version] dataset swds version: {self._version}")

    def _prepare_snapshot(self):
        self._snapshot_workdir = self.dataset_dir / self._name / self._version

        if self._snapshot_workdir.exists():
            raise Exception(f"{self._snapshot_workdir} has already exists, will abort")

        ensure_dir(self._data_dir)
        ensure_dir(self._src_dir)
        ensure_dir(self._docker_dir)

        logger.info(f"[step:prepare-snapshot]swds snapshot workdir: {self._snapshot_workdir}")

    @property
    def _data_dir(self):
        return self._snapshot_workdir / "data"

    @property
    def _src_dir(self):
        return self._snapshot_workdir / "src"

    @property
    def _docker_dir(self):
        return self._snapshot_workdir / "dep" / "docker"

    @classmethod
    def build(cls, workdir: str, ds_yaml_name: str, dry_run: bool=False) -> None:
        ds = DataSet(workdir, ds_yaml_name, dry_run)
        ds._do_validate()
        ds._do_build()

    @classmethod
    def push(cls, swds: str):
        pass

    @classmethod
    def info(cls, swds: str):
        pass

    @classmethod
    def list(cls):
        #TODO: add filter
        pass

    def load_dataset_config(self, fpath: t.Union[str, Path]) -> DataSetConfig:
        fpath = Path(fpath)
        if not fpath.exists():
            raise FileExistsError(f"dataset yaml {fpath} is not existed")

        if not str(fpath).endswith((".yaml", ".yml")):
            raise FileTypeError(f"{fpath} file type is not yaml|yml")

        return DataSetConfig.create_by_yaml(fpath)