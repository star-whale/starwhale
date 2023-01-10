import json
import base64
import typing as t
from copy import deepcopy
from enum import Enum

from starwhale.utils.error import FormatError


class ASDictMixin:
    def asdict(self, ignore_keys: t.Optional[t.List[str]] = None) -> t.Dict:
        d = deepcopy(self.__dict__)
        ignore_keys = ignore_keys or []
        for _k in ignore_keys:
            d.pop(_k, None)

        r = _do_asdict_convert(d)
        if isinstance(r, dict):
            return r
        else:
            raise FormatError(f"{self} cannot be formatted as a dict")

    def jsonify(self, ignore_keys: t.Optional[t.List[str]] = None) -> str:
        r = self.asdict(ignore_keys)
        return json.dumps(r, separators=(",", ":"))


def _do_asdict_convert(obj: t.Any) -> t.Any:
    if isinstance(obj, dict):
        return {k: _do_asdict_convert(v) for k, v in obj.items()}
    elif (
        hasattr(obj, "_asdict") and hasattr(obj, "_fields") and isinstance(obj, tuple)
    ):  # namedtuple
        return {k: _do_asdict_convert(v) for k, v in obj._asdict().items()}  # type: ignore
    elif isinstance(obj, (list, tuple)):
        return type(obj)(_do_asdict_convert(v) for v in obj)
    elif isinstance(obj, Enum):
        return obj.value
    elif hasattr(obj, "asdict"):
        return obj.asdict()
    elif isinstance(obj, bytes):
        return base64.b64encode(obj).decode()
    else:
        # TODO: add more type parse
        return obj
