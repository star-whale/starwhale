import csv
import typing as t
from pathlib import Path

from starwhale.api.dataset import Text, BuildExecutor


class AGNewsBuildExecutor(BuildExecutor):
    def iter_item(self) -> t.Generator[t.Tuple[t.Any, t.Any], None, None]:
        root_dir = Path(__file__).parent.parent / "data"

        with (root_dir / "test.csv").open("r", encoding="utf-8") as f:
            for row in csv.reader(f, delimiter=",", quotechar='"'):
                # The class labels start from 1 in this dataset
                # https://huggingface.co/datasets/ag_news#default-1
                annotations = {"label": int(row[0]) - 1}
                data = " ".join(row[1:])
                yield Text(content=data), annotations
