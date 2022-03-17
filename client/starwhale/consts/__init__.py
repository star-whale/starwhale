import pathlib
from unittest.mock import DEFAULT

#TODO: use str path, not Path Class
CONFIG_DIR = pathlib.Path("~/.config/starwhale")
SW_CLI_CONFIG = CONFIG_DIR / 'config.yaml'

ENV_SW_CLI_CONFIG = "SW_CLI_CONFIG"
ENV_DEBUG_MODE = "DEBUG_MODE"

DEFAULT_STARWHALE_API_VERSION = "1.0"

DEFAULT_MODEL_YAML_NAME = "model.yaml"

DEFAULT_LOCAL_SW_CONTROLLER_ADDR = "localhost:6543"


#TODO: use ~/.starwhale or ~/.cache/starwhale?
SW_LOCAL_STORAGE = "~/.cache/starwhale"