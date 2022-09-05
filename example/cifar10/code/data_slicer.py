import pickle
import typing as t
from pathlib import Path

from starwhale.api.dataset import BuildExecutor


class CIFAR10Slicer(BuildExecutor):
    def iter_item(self) -> t.Generator[t.Tuple[t.Any, t.Any], None, None]:
        root_dir = Path(__file__).parent.parent / "data"

        with (root_dir / "test_batch").open("rb") as f:
            content = pickle.load(f, encoding="bytes")
            for data, label in zip(content[b"data"], content[b"labels"]):
                annotations = {"label": label}
                yield data.tobytes(), annotations
