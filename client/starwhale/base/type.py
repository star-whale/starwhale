import typing as t


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
