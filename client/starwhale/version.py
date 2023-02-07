import os

try:
    from importlib.metadata import version
except ImportError:  # pragma: no cover
    from importlib_metadata import version  # pragma: no cover


STARWHALE_VERSION: str = version("starwhale")  # type: ignore
os.environ["SW_VERSION"] = STARWHALE_VERSION
