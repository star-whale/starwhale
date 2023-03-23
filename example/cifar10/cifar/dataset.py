import io
import pickle
import typing as t
from pathlib import Path

from PIL import Image as PILImage

from starwhale import Image, MIMEType

ROOT_DIR = Path(__file__).parent.parent / "data" / "cifar-10-batches-py"
TRAIN_DATASET_PATHS = [ROOT_DIR / f"data_batch_{i}" for i in range(1, 6)]
TEST_DATASET_PATHS = [ROOT_DIR / "test_batch"]


def parse_meta() -> t.Dict[str, t.Any]:
    with (ROOT_DIR / "batches.meta").open("rb") as f:
        return pickle.load(f)


dataset_meta = parse_meta()


def _iter_item(paths: t.List[Path]) -> t.Generator:
    for path in paths:
        with path.open("rb") as f:
            content = pickle.load(f, encoding="bytes")
            for data, label, filename in zip(
                content[b"data"], content[b"labels"], content[b"filenames"]
            ):
                image_array = data.reshape(3, 32, 32).transpose(1, 2, 0)
                image_bytes = io.BytesIO()
                PILImage.fromarray(image_array).save(image_bytes, format="PNG")
                yield {
                    "image": Image(
                        fp=image_bytes.getvalue(),
                        display_name=filename.decode(),
                        shape=image_array.shape,
                        mime_type=MIMEType.PNG,
                    ),
                    "label": label,
                    "label_display_name": dataset_meta["label_names"][label],
                }


def iter_cifar10_train_item() -> t.Generator:
    return _iter_item(TRAIN_DATASET_PATHS)


def iter_cifar10_test_item() -> t.Generator:
    return _iter_item(TEST_DATASET_PATHS)
