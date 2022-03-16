import importlib_metadata

__version__: str = importlib_metadata.version("starwhale")

__all__ = [
	"__version__",
	"api",
]