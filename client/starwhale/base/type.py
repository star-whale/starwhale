from __future__ import annotations

import typing as t
from enum import Enum, unique
from pathlib import Path


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


PathLike = t.Union[str, Path]
OptionalPathLike = t.Optional[PathLike]
