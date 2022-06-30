import os

import importlib_metadata

__version__: str = importlib_metadata.version("starwhale")  # type: ignore
os.environ["SW_VERSION"] = __version__


# TODO: only export api
