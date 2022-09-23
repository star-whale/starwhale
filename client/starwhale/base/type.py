from enum import Enum, unique

from starwhale.utils.error import NoSupportError


class InstanceType:
    STANDALONE = "standalone"
    CLOUD = "cloud"


class URIType:
    INSTANCE = "instance"
    PROJECT = "project"
    MODEL = "model"
    DATASET = "dataset"
    RUNTIME = "runtime"
    EVALUATION = "evaluation"
    UNKNOWN = "unknown"


class JobType:
    EVAL = "evaluation"


class EvalTaskType:
    ALL = "all"
    SINGLE = "single"


class RunSubDirType:
    DATASET = "dataset"
    STATUS = "status"
    RUNLOG = "runlog"
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


class RuntimeLockFileType:
    VENV = "requirements-sw-lock.txt"
    CONDA = "conda-sw-lock.yaml"


def get_bundle_type_by_uri(uri_type: str) -> str:
    if uri_type == URIType.DATASET:
        return BundleType.DATASET
    elif uri_type == URIType.MODEL:
        return BundleType.MODEL
    elif uri_type == URIType.RUNTIME:
        return BundleType.RUNTIME
    else:
        raise NoSupportError(uri_type)


@unique
class DataFormatType(Enum):
    SWDS_BIN = "swds_bin"
    USER_RAW = "user_raw"
    UNDEFINED = "undefined"


@unique
class ObjectStoreType(Enum):
    LOCAL = "local"
    REMOTE = "remote"
    UNDEFINED = "undefined"


@unique
class DataOriginType(Enum):
    NEW = "+"
    INHERIT = "~"
