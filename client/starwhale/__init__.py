import importlib_metadata

__version__: str = importlib_metadata.version("starwhale")


#TODO: only export api

__all__ = [
	"__version__",
	"api",
]