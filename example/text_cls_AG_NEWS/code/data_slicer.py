import re
import csv
import typing as t
from pathlib import Path

from starwhale.api.dataset import Text, BuildExecutor


class AGNewsSlicer(BuildExecutor):
    def iter_item(self) -> t.Generator[t.Tuple[t.Any, t.Any], None, None]:
        root_dir = Path(__file__).parent.parent / "data"

        with (root_dir / "test.csv").open("r", encoding="utf-8") as f:
            for row in csv.reader(f, delimiter=",", quotechar='"'):
                annotations = {"label": row[0]}
                data = " ".join(row[1:])
                data = re.sub("^\s*(.-)\s*$", "%1", data).replace("\\n", "\n")
                yield Text(content=data), annotations
