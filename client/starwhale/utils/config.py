import yaml
import os
import typing as t
from pathlib import Path

from starwhale.consts import (
    SW_CLI_CONFIG,
    DEFAULT_LOCAL_SW_CONTROLLER_ADDR,
    SW_LOCAL_STORAGE,
    ENV_SW_CLI_CONFIG,
)
from starwhale.utils.fs import ensure_dir
from starwhale.utils import fmt_http_server

_config: t.Dict[str, t.Any] = {}


def load_swcli_config() -> t.Dict[str, t.Any]:
    global _config

    if _config:
        return _config

    # TODO: add set_global_env func in cli startup
    fpath = get_swcli_config_path()

    if not os.path.exists(fpath):
        _config = render_default_swcli_config(fpath)
    else:
        with open(fpath) as f:
            _config = yaml.safe_load(f)

    return _config


def render_default_swcli_config(fpath: str) -> t.Dict[str, t.Any]:
    c = dict(
        controller=dict(
            remote_addr=DEFAULT_LOCAL_SW_CONTROLLER_ADDR,
            sw_token="",
            username="",
            user_role="",
        ),
        storage=dict(root=str(SW_LOCAL_STORAGE.resolve())),
    )
    render_swcli_config(c, fpath)
    return c


def update_swcli_config(**kw: t.Any) -> None:
    c = load_swcli_config()
    # TODO: tune update config
    c.update(kw)
    render_swcli_config(c)


def get_swcli_config_path() -> str:
    fpath = os.environ.get(ENV_SW_CLI_CONFIG, "")
    if not fpath or not os.path.exists(fpath):
        fpath = str(SW_CLI_CONFIG)
    return fpath


def render_swcli_config(c: t.Dict[str, t.Any], path: str = "") -> None:
    fpath = path or get_swcli_config_path()
    ensure_dir(os.path.dirname(fpath), recursion=True)
    with open(fpath, "w") as f:
        # TODO: use sw_cli_config class
        yaml.dump(c, f, default_flow_style=False)


# TODO: abstract better common base or mixed class
class SWCliConfigMixed(object):
    def __init__(self, swcli_config: t.Union[t.Dict[str, t.Any], None] = None) -> None:
        self._config = swcli_config or load_swcli_config()

    @property
    def rootdir(self) -> Path:
        return Path(self._config["storage"]["root"])

    @property
    def workdir(self) -> Path:
        return self.rootdir / "workdir"

    @property
    def pkgdir(self) -> Path:
        return self.rootdir / "pkg"

    @property
    def dataset_dir(self) -> Path:
        return self.rootdir / "dataset"

    @property
    def eval_run_dir(self) -> Path:
        return self.rootdir / "run" / "eval"

    @property
    def sw_remote_addr(self) -> str:
        addr = self._controller.get("remote_addr", "")
        return fmt_http_server(addr)

    @property
    def user_name(self) -> str:
        return self._controller.get("user_name", "")

    @property
    def _sw_token(self) -> str:
        return self._controller.get("sw_token", "")

    @property
    def _controller(self) -> t.Dict[str, t.Any]:
        return self._config.get("controller", {})

    @property
    def user_role(self) -> str:
        return self._controller.get("user_role", "")
