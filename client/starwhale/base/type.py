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
    JOB = "job"
    UNKNOWN = "unknown"


class JobType:
    EVAL = "evaluation"


class EvalTaskType:
    ALL = "all"
    PPL = "ppl"
    CMP = "cmp"


class RunSubDirType:
    RESULT = "result"
    DATASET = "dataset"
    PPL_RESULT = "ppl_result"
    STATUS = "status"
    LOG = "log"
    SWMP = "swmp"
    CONFIG = "config"


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
