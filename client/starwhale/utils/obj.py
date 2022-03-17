import typing as t

from loguru import logger
from functools import wraps


class Dict2Obj(object):

    def __init__(self, d: dict) -> None:
        for (k, v) in d.items():
            #TODO: use iterable type
            if isinstance(v, (list, tuple, set)):
                setattr(self, k, [Dict2Obj(i) if isinstance(i, dict) else i for i in v])
            else:
                setattr(self, k, Dict2Obj(v) if isinstance(v, dict) else v)


class Obj2Dict(object):

    def __call__(self, func: t.Any) -> t.Any:

        @wraps(func)
        def _wraps(*args, **kwargs):
            content =func(*args, **kwargs)
            if isinstance(content, (list, tuple, set)):
                return [m if isinstance(m, dict) else m.as_dict() for m in content]
            elif isinstance(content, dict):
                return content
            else:
                #TODO: replace as_dict
                return content.as_dict()
        return _wraps
