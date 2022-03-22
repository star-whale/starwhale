import yaml
import os

from loguru import logger

from starwhale.consts import (
    SW_CLI_CONFIG, DEFAULT_LOCAL_SW_CONTROLLER_ADDR,
    SW_LOCAL_STORAGE, ENV_SW_CLI_CONFIG,
)
from starwhale.utils.fs import ensure_dir

_config = {}


def load_swcli_config() -> dict:
    global _config

    if _config:
        return _config

    #TODO: add set_global_env func in cli startup
    fpath = get_swcli_config_path()

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
            root=str(SW_LOCAL_STORAGE.resolve())
        )
    )
    render_swcli_config(c, fpath)
    return c


def update_swcli_config(**kw) -> None:
    c = load_swcli_config()
    #TODO: tune update config
    c.update(kw)
    render_swcli_config(c)


def get_swcli_config_path() -> str:
    fpath = os.environ.get(ENV_SW_CLI_CONFIG, "")
    if not fpath or not os.path.exists(fpath):
        fpath = str(SW_CLI_CONFIG)
    return fpath


def render_swcli_config(c: dict, path: str="") -> None:
    fpath = path or get_swcli_config_path()
    ensure_dir(os.path.dirname(fpath), recursion=True)
    with open(fpath, "w") as f:
        #TODO: use sw_cli_config class
        yaml.dump(c, f, default_flow_style=False)