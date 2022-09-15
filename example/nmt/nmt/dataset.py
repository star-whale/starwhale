import typing as t
from pathlib import Path

from starwhale.api.dataset import Text, BuildExecutor

from .helper import normalize_str


class NMTDatasetBuildExecutor(BuildExecutor):
    def iter_item(self) -> t.Generator[t.Tuple[t.Any, t.Any], None, None]:
        root_dir = Path(__file__).parent.parent / "data"

        with (root_dir / "fra-test.txt").open("r") as f:
            for line in f.readlines():
                line = line.strip()
                if not line or line.startswith("CC-BY"):
                    continue

                _data, _label, *_ = line.split("\t")
                data = Text(normalize_str(_data), encoding="utf-8")
                annotations = {"label": normalize_str(_label)}
                yield data, annotations
