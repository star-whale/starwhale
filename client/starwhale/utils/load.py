from statistics import mode
import typing as t
import sys
from pathlib import Path
import importlib

from loguru import logger


def import_cls(workdir: Path, mc: str, parentClass: t.Any=object) -> t.Any:
    _module_name, _cls_name = mc.split(":", 1)
    _changed = False

    _w = str(workdir.absolute())
    if _w not in sys.path:
        sys.path.insert(0, _w)
        _changed = True

    try:
        _module = importlib.import_module(_module_name, package=_w)
        _cls = getattr(_module, _cls_name, None)
        if not _cls or not issubclass(_cls, parentClass):
            raise Exception(f"{mc} is not subclass of {parentClass}")
    except Exception:
        if _changed:
            sys.path.remove(_w)
        raise

    return _cls