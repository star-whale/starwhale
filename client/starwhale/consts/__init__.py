import pathlib
import typing as t


# TODO: use str path, not Path Class
HOMEDIR = pathlib.Path.home()
CONFIG_DIR = HOMEDIR / ".config/starwhale"
SW_CLI_CONFIG = CONFIG_DIR / "config.yaml"

ENV_SW_CLI_CONFIG = "SW_CLI_CONFIG"
ENV_LOG_LEVEL = "SW_LOG_LEVEL"

DEFAULT_STARWHALE_API_VERSION = "1.0"
DEFAULT_MODEL_YAML_NAME = "model.yaml"
DEFAULT_MANIFEST_NAME = "_manifest.yaml"
DEFAULT_LOCAL_SW_CONTROLLER_ADDR = "localhost:6543"
DEFAULT_DATASET_YAML_NAME = "dataset.yaml"
LOCAL_FUSE_JSON_NAME = "local_fuse.json"
DEFAULT_INPUT_JSON_FNAME = "input.json"

# TODO: use ~/.starwhale or ~/.cache/starwhale?
SW_LOCAL_STORAGE = HOMEDIR / ".cache/starwhale"

ENV_CONDA = "CONDA_DEFAULT_ENV"
ENV_CONDA_PREFIX = "CONDA_PREFIX"


class PythonRunEnv(t.NamedTuple):
    CONDA: str = "conda"
    VENV: str = "venv"
    SYSTEM: str = "system"


class HTTPMethod(t.NamedTuple):
    GET: str = "GET"
    OPTIONS: str = "OPTIONS"
    HEAD: str = "HEAD"
    POST: str = "POST"
    PUT: str = "PUT"
    DELETE: str = "DELETE"
    PATCH: str = "PATCH"


FMT_DATETIME = "%Y-%m-%d %H:%M:%S %Z"

# TODO: use better DEFAULT words?
DEFAULT_COPY_WORKERS = 4

JSON_INDENT = 4

SW_API_VERSION = "v1"

SHORT_VERSION_CNT = 12
VERSION_PREFIX_CNT = 2


class SWDSBackendType(t.NamedTuple):
    S3: str = "s3"
    FUSE: str = "fuse"


class DataLoaderKind(t.NamedTuple):
    SWDS: str = "swds"
    JSONL: str = "jsonl"


class SWDSSubFileType(t.NamedTuple):
    BIN: str = "swds_bin"
    META: str = "swds_meta"


SWDS_DATA_FNAME_FMT = "data_ubyte_{index}.%s" % SWDSSubFileType.BIN
SWDS_LABEL_FNAME_FMT = "label_ubyte_{index}.%s" % SWDSSubFileType.BIN

CURRENT_FNAME = "current"
