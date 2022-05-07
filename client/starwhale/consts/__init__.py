import pathlib
from collections import namedtuple


#TODO: use str path, not Path Class
HOMEDIR = pathlib.Path.home()
CONFIG_DIR = HOMEDIR / ".config/starwhale"
SW_CLI_CONFIG = CONFIG_DIR / 'config.yaml'

ENV_SW_CLI_CONFIG = "SW_CLI_CONFIG"
ENV_DEBUG_MODE = "DEBUG_MODE"

DEFAULT_STARWHALE_API_VERSION = "1.0"
DEFAULT_MODEL_YAML_NAME = "model.yaml"
DEFAULT_MANIFEST_NAME = "_manifest.yaml"
DEFAULT_LOCAL_SW_CONTROLLER_ADDR = "localhost:6543"
DEFAULT_DATASET_YAML_NAME = "dataset.yaml"

#TODO: use ~/.starwhale or ~/.cache/starwhale?
SW_LOCAL_STORAGE = HOMEDIR / ".cache/starwhale"

ENV_CONDA = "CONDA_DEFAULT_ENV"
ENV_CONDA_PREFIX = "CONDA_PREFIX"

PYTHON_RUN_ENV = namedtuple("PYTHON_RUN_ENV", ["CONDA", "VENV", "SYSTEM"])(
    "conda", "venv", "system"
)

HTTP_METHOD = namedtuple("HTTP_METHOD", ["GET", "OPTIONS", "HEAD", "POST", "PUT", "DELETE", "PATCH"])(
    "GET", "OPTIONS", "HEAD", "POST", "PUT", "DELETE", "PATCH"
)

FMT_DATETIME = "%Y-%m-%d %H:%M:%S %Z"

#TODO: use better DEFAULT words?
DEFAULT_COPY_WORKERS = 4

SW_API_VERSION = "v1"
