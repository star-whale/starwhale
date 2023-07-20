from __future__ import annotations

import os
import random
import socket
import typing as t
from urllib.parse import urlparse

_pseudo_host_name = "bc-.starwhale.ai"
_cache_server_port = int(os.getenv("BLOB_CACHE_SERVER_PORT", "18080"))


class _Server:
    def __init__(self, ip: str) -> None:
        self.ip = ip


_servers: t.List[_Server] | None = None


def init() -> None:
    global _servers
    if _servers is None:
        try:
            _servers = [
                _Server(ip) for ip in socket.gethostbyname_ex(_pseudo_host_name)[2]
            ]
            random.shuffle(_servers)
        except Exception:
            _servers = []


_index = 0


# This module is not thread safe. This function should be invoked by async
# funtions in the same event loop.
def replace_url(url: str, replace: bool) -> t.Generator[str, None, None]:
    assert _servers is not None
    result = urlparse(url)
    if len(_servers) == 0 or not replace:
        while True:
            yield url
    else:
        result = result._replace(scheme="http")
        while True:
            global _index
            _index += 1
            yield result._replace(
                netloc=f"{_servers[_index % len(_servers)].ip}:{_cache_server_port}"
            ).geturl()
