import typing as t
from copy import deepcopy


def flatten(
    origin: t.Dict[str, t.Any], extract_sequence: bool = False
) -> t.Dict[str, t.Any]:
    rt = {}

    def _f_dict(_s: t.Dict[str, t.Any], _prefix: str = "") -> None:
        for _k, _v in _s.items():
            _k = f"{_prefix}{_k}"
            if isinstance(_v, dict):
                _f_dict(_v, _prefix=f"{_k}/")
            elif isinstance(_v, (tuple, list)) and extract_sequence:
                _f_sequence(_v, _prefix=f"{_k}/")
            else:
                rt[_k] = _v

    def _f_sequence(
        data: t.Union[t.Tuple[t.Any, ...], t.List[t.Any]], _prefix: str = ""
    ) -> None:
        index = 0
        for _d in data:
            if isinstance(_d, dict):
                _f_dict(_d, _prefix=f"{_prefix}")
            elif isinstance(_d, (tuple, list)):
                _f_sequence(_d, _prefix=f"{_prefix}")
            else:
                rt[f"{_prefix}{index}"] = _d
            index += 1

    _f_dict(deepcopy(origin))
    return rt
