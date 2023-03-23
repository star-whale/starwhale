import typing as t
from pathlib import Path

from starwhale import Text

from .helper import normalize_str


def iter_nmt_item() -> t.Generator:
    root_dir = Path(__file__).parent.parent / "data"

    with (root_dir / "fra-test.txt").open("r") as f:
        for line in f.readlines():
            line = line.strip()
            if not line or line.startswith("CC-BY"):
                continue

            _data, _label, *_ = line.split("\t")
            yield {
                "english": Text(normalize_str(_data), encoding="utf-8"),
                "french": Text(normalize_str(_label), encoding="utf-8"),
            }
