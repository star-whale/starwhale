import typing as t
from pathlib import Path

from starwhale.api.dataset import Text, BuildExecutor

from .helper import normalizeString


class DataSetProcessExecutor(BuildExecutor):
    def iter_item(self) -> t.Generator[t.Tuple[t.Any, t.Any], None, None]:
        root_dir = Path(__file__).parent.parent / "data"

        with (root_dir / "test_eng-fra.txt").open("r") as f:
            for line in f.readlines():
                line = line.strip()
                if not line or line.startswith("CC-BY"):
                    continue

                _data, _label = line.split("\t", 1)
                data = Text(normalizeString(_data), encoding="utf-8")
                annotations = {"label": normalizeString(_label)}
                yield data, annotations
