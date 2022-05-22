import yaml
import os
import typing as t
from pathlib import Path
import getpass

from starwhale.consts import (
    SW_CLI_CONFIG,
    SW_LOCAL_STORAGE,
    ENV_SW_CLI_CONFIG,
    DEFAULT_INSTANCE,
    DEFAULT_PROJECT,
    UserRoleType,
    STANDALONE_INSTANCE,
)

from .fs import ensure_dir, ensure_file
from . import fmt_http_server, console, now_str

_config: t.Dict[str, t.Any] = {}
_CURRENT_SHELL_USERNAME = getpass.getuser()


class InstanceType:
    STANDALONE = "standalone"
    CLOUD = "cloud"


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
        instances={
            STANDALONE_INSTANCE: dict(
                uri=DEFAULT_INSTANCE,
                user_name=_CURRENT_SHELL_USERNAME,
                current_project=DEFAULT_PROJECT,
                type=InstanceType.STANDALONE,
                updated_at=now_str(),  # type: ignore
            )
        },
        current_instance=DEFAULT_INSTANCE,
        storage=dict(root=str(SW_LOCAL_STORAGE.resolve())),
    )
    render_swcli_config(c, fpath)
    return c


def update_swcli_config(**kw: t.Any) -> None:
    c = load_swcli_config()
    # TODO: tune update config
    # TODO: add deepcopy for dict?
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
    ensure_file(fpath, yaml.dump(c, default_flow_style=False), mode=0o600)


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
        addr = self._current_instance_obj.get("uri", "")
        return fmt_http_server(addr)

    @property
    def user_name(self) -> str:
        return self._current_instance_obj.get("user_name", "")

    @property
    def _sw_token(self) -> str:
        return self._current_instance_obj.get("sw_token", "")

    @property
    def _current_instance_obj(self):
        return self._config.get("instances", {}).get(self.current_instance, {})

    @property
    def user_role(self) -> str:
        return self._current_instance_obj.get("user_role", "")

    @property
    def current_instance(self) -> str:
        return self._config["current_instance"]  # type: ignore

    def delete_instance(self, uri: str) -> None:
        if uri == STANDALONE_INSTANCE:
            return

        _insts = self._config["instances"]
        _alias = uri
        if uri in _insts:
            _insts.pop(uri)
        else:
            for k, v in _insts.items():
                if v.get("uri") == uri:
                    _insts.pop(k)
                    _alias = uri

        if _alias == self._config["current_instance"]:
            self._config["current_instance"] = DEFAULT_INSTANCE
        update_swcli_config(**self._config)

    def update_instance(
        self,
        uri: str,
        user_name: str = _CURRENT_SHELL_USERNAME,
        user_role: str = UserRoleType.NORMAL,
        sw_token: str = "",
        current_project: str = DEFAULT_PROJECT,
        alias: str = "",
    ) -> None:
        # TODO: abstrace instance class
        uri = uri.strip()
        if not uri.startswith(("http://", "https://")):
            uri = f"http://{uri}"

        alias = alias or uri
        if alias == STANDALONE_INSTANCE:
            console.print(f":person_running: skip {STANDALONE_INSTANCE} update")
            return

        # TODO: add more instance list and search
        _instances: t.Dict[str, t.Dict[str, str]] = self._config["instances"]
        if alias not in _instances:
            _instances[alias] = {}

        _instances[alias].update(
            uri=uri,
            user_name=user_name,
            user_role=user_role,
            sw_token=sw_token,
            current_project=current_project,
            type=InstanceType.CLOUD,
            updated_at=now_str(),  # type: ignore
        )

        update_swcli_config(**self._config)
