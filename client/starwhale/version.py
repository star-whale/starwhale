import os

try:
    from importlib.metadata import version
except ImportError:
    from importlib_metadata import version


STARWHALE_VERSION: str = version("starwhale")  # type: ignore
os.environ["SW_VERSION"] = STARWHALE_VERSION
