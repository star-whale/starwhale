import pathlib

CONFIG_DIR = pathlib.Path("~/.config/starwhale")
SW_CLI_CONFIG = CONFIG_DIR / 'config.yaml'

ENV_SW_CLI_CONFIG = "SW_CLI_CONFIG"
ENV_DEBUG_MODE = "DEBUG_MODE"