import io
import pickle
from pathlib import Path

from PIL import Image as PILImage

from starwhale import Image, MIMEType

ROOT_DIR = Path(__file__).parent
DATA_DIR = ROOT_DIR / "data" / "cifar-100-python"


def iter_train_item():
    return _do_iter_item("train")


def iter_test_item():
    return _do_iter_item("test")


def _do_iter_item(fname):
    with (DATA_DIR / "meta").open("rb") as f:
        meta = pickle.load(f, encoding="bytes")

    with (DATA_DIR / fname).open("rb") as f:
        content = pickle.load(f, encoding="bytes")
        for data, fine_label, coarse_label, filename in zip(
            content[b"data"],
            content[b"fine_labels"],
            content[b"coarse_labels"],
            content[b"filenames"],
        ):
            # TODO: support global classLabel
            annotations = {
                "fine_label": fine_label,
                "fine_label_name": meta[b"fine_label_names"][fine_label].decode(),
                "coarse_label": coarse_label,
                "coarse_label_name": meta[b"coarse_label_names"][coarse_label].decode(),
            }

            image_array = data.reshape(3, 32, 32).transpose(1, 2, 0)
            image_bytes = io.BytesIO()
            PILImage.fromarray(image_array).save(image_bytes, format="PNG")

            yield Image(
                fp=image_bytes.getvalue(),
                display_name=filename.decode(),
                shape=image_array.shape,
                mime_type=MIMEType.PNG,
            ), annotations
