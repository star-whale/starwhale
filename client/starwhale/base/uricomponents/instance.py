from typing import Any, Dict, Tuple, Optional
from dataclasses import dataclass
from urllib.parse import urlparse

from starwhale.utils import config
from starwhale.base.uri import URI
from starwhale.base.uricomponents.exceptions import NoMatchException


def _get_instances() -> Dict[str, Dict]:
    return config.load_swcli_config().get("instances", {})


def _get_default_instance_alias() -> str:
    return config.load_swcli_config().get("current_instance", "")


def _find_alias_by_url(url: str) -> Tuple[str, str]:
    """parse url and return instance alias and path from url"""
    if not url:
        return _get_default_instance_alias(), ""
    if url.startswith("local/"):
        return "local", url[len("local") :]
    p = urlparse(url)
    # use host as alias when url starts with cloud
    if p.scheme == "cloud":
        return p.netloc, p.path

    ins_url = "://".join([p.scheme, p.netloc])
    hits = [name for (name, conf) in _get_instances().items() if conf["uri"] == ins_url]
    if len(hits) == 1:
        return hits[0], p.path
    raise NoMatchException(url, hits)


def _check_alias_exists(alias: str) -> None:
    if alias not in _get_instances():
        raise NoMatchException(alias)


@dataclass
class Instance:
    """
    Data structure for Instance info
    """

    alias: str
    path: str = ""

    def __init__(
        self,
        uri: str = "",
        instance_alias: Optional[str] = None,
    ) -> None:
        if instance_alias and uri:
            raise Exception("alias and uri can not both set")
        if not instance_alias:
            instance_alias, path = _find_alias_by_url(uri)
            self.path = path.strip("/")
        _check_alias_exists(instance_alias)
        self.alias = instance_alias

    @property
    def info(self) -> Dict[str, str]:
        """Get current instance info"""
        return _get_instances().get(self.alias, {})

    def __getattr__(self, name: str) -> Any:
        return self.info.get(name)

    @property
    def url(self) -> str:
        return self.info["uri"]

    @property
    def type(self) -> str:
        return self.info["type"]

    @property
    def token(self) -> str:
        return self.info["sw_token"]

    @property
    def is_local(self) -> bool:
        return self.url == "local"

    def __str__(self) -> str:
        if self.is_local:
            return self.url
        return f"cloud://{self.alias}"

    def to_uri(self) -> URI:
        return URI.capsulate_uri(str(self))
