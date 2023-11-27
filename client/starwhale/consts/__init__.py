import pathlib
from enum import Enum, unique
from dataclasses import dataclass

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
SW_BUILT_IN = "starwhale-built-in"

# evaluation related constants
DEFAULT_JOBS_FILE_NAME = "jobs.yaml"
# auto generated evaluation panel layout file name from yaml or local console
EVALUATION_PANEL_LAYOUT_JSON_FILE_NAME = "eval_panel_layout.json"
# user defined evaluation panel layout file name
EVALUATION_PANEL_LAYOUT_YAML_FILE_NAME = "eval_panel_layout.yaml"

DEFAULT_LOCAL_SW_CONTROLLER_ADDR = "localhost:7827"
LOCAL_CONFIG_VERSION = "2.0"
DEFAULT_FILE_SIZE_THRESHOLD_TO_TAR_IN_MODEL = 10 * 1024  # 10KB

DEFAULT_SIGNED_URL_EXPIRE_TIME = 1000 * 3600 * 24  # 24 hours
ENV_SIGNED_URL_EXPIRE_TIME = "SW_MODEL_PROCESS_UNIT_TIME_MILLIS"

SW_AUTO_DIRNAME = ".starwhale"
SW_EVALUATION_EXAMPLE_DIR = "examples"

RESOURCE_FILES_NAME = "resource_files.yaml"
DIGEST_FILE_NAME = "digest.yaml"

# used by the versions before 2.0
# SW_LOCAL_STORAGE = HOMEDIR / ".cache/starwhale"
DEFAULT_SW_LOCAL_STORAGE = HOMEDIR / SW_AUTO_DIRNAME
# SW_TMP_DIR_NAME dir is used for storing the processing files
SW_TMP_DIR_NAME = ".tmp"

ENV_CONDA = "CONDA_DEFAULT_ENV"
ENV_CONDA_PREFIX = "CONDA_PREFIX"
ENV_VENV = "VIRTUAL_ENV"

ENV_POD_NAME = "SW_POD_NAME"
ENV_DISABLE_PROGRESS_BAR = "DISABLE_PROGRESS_BAR"

ENV_BUILD_BUNDLE_FIXED_VERSION_FOR_TEST = "SW_BUILD_BUNDLE_FIXED_VERSION_FOR_TEST"


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
FMT_DATETIME_NO_TZ = "%Y-%m-%d %H:%M:%S"
MINI_FMT_DATETIME = "%H:%M:%S"

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
    Http = "http"


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


@unique
class FileDesc(Enum):
    MANIFEST = "MANIFEST"
    SRC = "SRC"
    SRC_TAR = "SRC_TAR"
    MODEL = "MODEL"
    DATA = "DATA"


class RunStatus:
    INIT = "init"
    START = "start"
    RUNNING = "running"
    SUCCESS = "success"
    FAILED = "failed"


class FileFlag:
    UNCHANGED = "unchanged"
    ADDED = "added"
    UPDATED = "updated"
    DELETED = "deleted"


class DecoratorInjectAttr:
    Step = "_starwhale_inject_step_decorator"
    Predict = "_starwhale_inject_predict_decorator"
    Evaluate = "_starwhale_inject_evaluate_decorator"
    FineTune = "_starwhale_inject_finetune_decorator"


@dataclass
class FileNode:
    path: pathlib.Path
    name: str
    size: int
    file_desc: FileDesc
    signature: str = ""
    flag: str = ""


SWDS_DATA_FNAME_FMT = "data_ubyte_{index}.%s" % SWDSSubFileType.BIN
ARCHIVED_SWDS_META_FNAME = "archive.%s" % SWDSSubFileType.META
SWMP_SRC_FNAME = "src.tar"

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

DEFAULT_IMAGE_REPO = "docker-registry.starwhale.cn/star-whale"
DEFAULT_IMAGE_NAME = "base"
# When release a new base image version, remember to update the FIXED_RELEASE_BASE_IMAGE_VERSION.
# refer to https://github.com/star-whale/starwhale/blob/main/docker/Makefile#L12
FIXED_RELEASE_BASE_IMAGE_VERSION = "0.3.4"
SW_IMAGE_FMT = "{repo}/{name}:{tag}"
DEFAULT_SW_TASK_RUN_IMAGE = SW_IMAGE_FMT.format(
    repo=DEFAULT_IMAGE_REPO,
    name=DEFAULT_IMAGE_NAME,
    tag=FIXED_RELEASE_BASE_IMAGE_VERSION,
)
SW_IGNORE_FILE_NAME = ".swignore"

CNTR_DEFAULT_PIP_CACHE_DIR = "/root/.cache/pip"

SW_DEV_DUMMY_VERSION = "0.0.0.dev0"
SW_PYPI_PKG_NAME = "starwhale"

DEFAULT_CUDA_VERSION = "11.4.0"
DEFAULT_CONDA_CHANNEL = "conda-forge"

WHEEL_FILE_EXTENSION = ".whl"

CREATED_AT_KEY = "created_at"

D_FILE_VOLUME_SIZE = 64 * 1024 * 1024  # 64MB
D_ALIGNMENT_SIZE = 128  # for page cache

DEFAULT_RESOURCE_POOL = "default"
