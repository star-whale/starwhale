from enum import Enum, unique

from starwhale.utils.error import NoSupportError
from starwhale.base.uri.resource import ResourceType


class InstanceType:
    STANDALONE = "standalone"
    CLOUD = "cloud"


class RunSubDirType:
    DATASET = "dataset"
    STATUS = "status"
    RUNLOG = "runlog"
    SNAPSHOT = "snapshot"
    LOG = "log"
    SWMP = "swmp"
    SWRT = "swrt"


class JobOperationType:
    CANCEL = "cancel"
    PAUSE = "pause"
    RESUME = "resume"
    CREATE = "create"
    REMOVE = "remove"
    RECOVER = "recover"
    INFO = "info"


class BundleType:
    MODEL = ".swmp"
    DATASET = ".swds"
    RUNTIME = ".swrt"


class RuntimeArtifactType:
    RUNTIME = "runtime_yaml"
    DEPEND = "dependencies"
    WHEELS = "wheels"
    FILES = "files"
    CONFIGS = "configs"


class RuntimeLockFileType:
    VENV = "requirements-sw-lock.txt"
    CONDA = "conda-sw-lock.yaml"


def get_bundle_type_by_uri(uri_type: ResourceType) -> str:
    if uri_type == ResourceType.dataset:
        return BundleType.DATASET
    elif uri_type == ResourceType.model:
        return BundleType.MODEL
    elif uri_type == ResourceType.runtime:
        return BundleType.RUNTIME
    else:
        raise NoSupportError(uri_type)


@unique
class DependencyType(Enum):
    PIP_PKG = "pip_pkg"
    PIP_REQ_FILE = "pip_req_file"
    CONDA_PKG = "conda_pkg"
    CONDA_ENV_FILE = "conda_env_file"
    WHEEL = "wheel"
    NATIVE_FILE = "native_file"
    COMMAND = "command"


@unique
class DatasetChangeMode(Enum):
    PATCH = "patch"
    OVERWRITE = "overwrite"


@unique
class PredictLogMode(Enum):
    PLAIN = "plain"
    PICKLE = "pickle"


@unique
class DatasetFolderSourceType(Enum):
    IMAGE = "image"
    VIDEO = "video"
    AUDIO = "audio"
