import yaml
import os

from loguru import logger

from starwhale.consts import (
    SW_CLI_CONFIG, DEFAULT_LOCAL_SW_CONTROLLER_ADDR,
    SW_LOCAL_STORAGE,
)

_config = {}


def load_swcli_config(fpath: str) -> dict:
    global _config

    if _config:
        return _config

    if not os.path.exists(fpath):
        fpath = str(SW_CLI_CONFIG)

    if not os.path.exists(fpath):
        _config = render_default_swcli_config(fpath)
    else:
        with open(fpath) as f:
            _config = yaml.safe_load(f)

    return _config


def render_default_swcli_config(fpath: str) -> dict:
    c = dict(
        controller=dict(
            remote_addr=DEFAULT_LOCAL_SW_CONTROLLER_ADDR,
            sw_token="",
        ),
        storage=dict(
            root=SW_LOCAL_STORAGE
        )
    )

    #TODO: use sw_cli_config class

    with open(fpath, "w") as f:
        yaml.dump(c, f, default_flow_style=False)

    return c