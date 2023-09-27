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
                _f_dict(_d, _prefix=f"{_prefix}{index}/")
            elif isinstance(_d, (tuple, list)):
                _f_sequence(_d, _prefix=f"{_prefix}{index}/")
            else:
                rt[f"{_prefix}{index}"] = _d
            index += 1

    _f_dict(deepcopy(origin))
    return rt


def transform_dict(d: dict, key_selector: dict) -> dict:
    _r = {}
    for field_selector, v in key_selector.items():
        data = d
        fields = field_selector.split(".")
        for field in fields:
            if "[" in field and "]" in field:
                # Parse array index
                index = int(field[field.find("[") + 1 : field.find("]")])
                # Get field name
                field = field[: field.find("[")]
                # Check if field exists and is an array
                if field in data and isinstance(data[field], list):
                    # Check if array index is within bounds
                    if 0 <= index < len(data[field]):
                        data = data[field][index]
                    else:
                        raise ValueError(
                            f"Array index {index} out of bounds for field {field}"
                        )
                else:
                    raise ValueError(f"Field {field} not found or not an array")
            else:
                # Check if field exists
                if field in data:
                    data = data[field]
                else:
                    raise ValueError(f"Field {field} not found in data")
        _r.update({v: data})
    return _r
