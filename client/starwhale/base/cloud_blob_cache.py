from __future__ import annotations

import os
import math
import random
import socket
import typing as t
from urllib.parse import urlparse

_pseudo_host_name = "bc-.starwhale.ai"
_cache_server_port = int(os.getenv("BLOB_CACHE_SERVER_PORT", "18080"))


class _Server:
    def __init__(self, ip: str) -> None:
        self.ip = ip
        self.running = 0
        self.success = 0
        self.error = 0.0
        self.error_step = 1.0

    def score(self) -> float:
        return (self.success + self.error + 1) / (self.success + 1) * (self.running + 1)


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
            error = False
            try:
                best = _servers[0]
                for server in _servers:
                    if server.score() < best.score():
                        best = server
                best.running += 1
                yield result._replace(netloc=f"{best.ip}:{_cache_server_port}").geturl()
                error = True
            finally:
                if error:
                    best.error += best.error_step
                    best.error_step = best.error_step * 2 + math.pow(best.success, 0.5)
                else:
                    best.success += 1
                    best.error_step /= 1.1
                    best.error = max(0, best.error - best.error_step)
                best.running -= 1
