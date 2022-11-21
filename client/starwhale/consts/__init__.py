import pathlib

# TODO: use str path, not Path Class
HOMEDIR = pathlib.Path.home()
CONFIG_DIR = HOMEDIR / ".config" / "starwhale"
SW_CLI_CONFIG = CONFIG_DIR / "config.yaml"

ENV_SW_CLI_CONFIG = "SW_CLI_CONFIG"
ENV_LOG_LEVEL = "SW_LOG_LEVEL"
ENV_LOG_VERBOSE_COUNT = "SW_LOG_VERBOSE_COUNT"
ENV_SW_IMAGE_REPO = "SW_IMAGE_REPO"
# SW_LOCAL_STORAGE env used for generating default swcli config
# and overriding 'storage.root' swcli config in runtime
ENV_SW_LOCAL_STORAGE = "SW_LOCAL_STORAGE"

DEFAULT_STARWHALE_API_VERSION = "1.0"
DEFAULT_MANIFEST_NAME = "_manifest.yaml"
DEFAULT_EVALUATION_JOB_NAME = "default"
DEFAULT_EVALUATION_JOBS_FNAME = "eval_jobs.yaml"
DEFAULT_EVALUATION_PIPELINE = "starwhale.core.model.default_handler"
DEFAULT_LOCAL_SW_CONTROLLER_ADDR = "localhost:7827"
LOCAL_CONFIG_VERSION = "2.0"

SW_AUTO_DIRNAME = ".starwhale"

# used by the versions before 2.0
# SW_LOCAL_STORAGE = HOMEDIR / ".cache/starwhale"
DEFAULT_SW_LOCAL_STORAGE = HOMEDIR / SW_AUTO_DIRNAME
# SW_TMP_DIR_NAME dir is used for storing the processing files
SW_TMP_DIR_NAME = ".tmp"

ENV_CONDA = "CONDA_DEFAULT_ENV"
ENV_CONDA_PREFIX = "CONDA_PREFIX"
ENV_VENV = "VIRTUAL_ENV"

ENV_POD_NAME = "SW_POD_NAME"


class DefaultYAMLName:
    MODEL = "model.yaml"
    DATASET = "dataset.yaml"
    RUNTIME = "runtime.yaml"


class PythonRunEnv:
    CONDA = "conda"
    VENV = "venv"
    SYSTEM = "system"
    DOCKER = "docker"
    AUTO = "auto"


class HTTPMethod:
    GET = "GET"
    OPTIONS = "OPTIONS"
    HEAD = "HEAD"
    POST = "POST"
    PUT = "PUT"
    DELETE = "DELETE"
    PATCH = "PATCH"


class UserRoleType:
    ADMIN = "admin"
    NORMAL = "normal"


class SupportArch:
    AMD64 = "amd64"
    ARM64 = "arm64"
    NOARCH = "noarch"


class SupportOS:
    UBUNTU = "ubuntu:20.04"


FMT_DATETIME = "%Y-%m-%d %H:%M:%S %Z"

# TODO: use better DEFAULT words?
DEFAULT_COPY_WORKERS = 4

JSON_INDENT = 4

SW_API_VERSION = "v1"

SHORT_VERSION_CNT = 12
VERSION_PREFIX_CNT = 2


class SWDSBackendType:
    S3 = "s3"
    LocalFS = "local_fs"
    SignedUrl = "signed_url"


class EvalHandlerType:
    DEFAULT = "default"
    CUSTOM = "custom"


class DataLoaderKind:
    SWDS = "swds"
    JSONL = "jsonl"
    RAW = "raw"


class EvaluationResultKind:
    RESULT = "result"
    METRIC = "metric"


class SWDSSubFileType:
    BIN = "swds_bin"
    META = "swds_meta"


SWDS_DATA_FNAME_FMT = "data_ubyte_{index}.%s" % SWDSSubFileType.BIN
ARCHIVED_SWDS_META_FNAME = "archive.%s" % SWDSSubFileType.META

CURRENT_FNAME = "current"

STANDALONE_INSTANCE = "local"
DEFAULT_INSTANCE = STANDALONE_INSTANCE
DEFAULT_PROJECT = "self"

DEFAULT_PAGE_IDX = 1
DEFAULT_PAGE_SIZE = 20
DEFAULT_REPORT_COLS = 20

RECOVER_DIRNAME = ".recover"
OBJECT_STORE_DIRNAME = ".objectstore"
DATA_STORE_DIRNAME = ".datastore"
DEFAULT_PYTHON_VERSION = "3.8"
LATEST_TAG = "latest"

YAML_TYPES = (".yaml", ".yml")

DEFAULT_IMAGE_REPO = "ghcr.io/star-whale"
SW_IMAGE_FMT = "{repo}/starwhale:{tag}"
DEFAULT_SW_TASK_RUN_IMAGE = SW_IMAGE_FMT.format(repo=DEFAULT_IMAGE_REPO, tag=LATEST_TAG)
SW_IGNORE_FILE_NAME = ".swignore"

CNTR_DEFAULT_PIP_CACHE_DIR = "/root/.cache/pip"

SW_DEV_DUMMY_VERSION = "0.0.0.dev0"
SW_PYPI_PKG_NAME = "starwhale"

DEFAULT_CUDA_VERSION = "11.4.0"
DEFAULT_CONDA_CHANNEL = "conda-forge"

WHEEL_FILE_EXTENSION = ".whl"
AUTH_ENV_FNAME = ".auth_env"
