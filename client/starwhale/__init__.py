import os

try:
    from importlib.metadata import version
except ImportError:
    from importlib_metadata import version

__version__: str = version("starwhale")  # type: ignore
os.environ["SW_VERSION"] = __version__


# TODO: only export api
