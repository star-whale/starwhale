import struct
import typing as t
from pathlib import Path

from starwhale import GrayscaleImage

_TItem = t.Generator[t.Tuple[t.Any, t.Any], None, None]

ROOT_DIR = Path(__file__).parent
DATA_DIR = ROOT_DIR / "data" / "gzip"


def iter_digits_test_item():
    return _do_iter_item("digits-test")


def iter_digits_train_item():
    return _do_iter_item("digits-train")


def _do_iter_item(fname):
    with (DATA_DIR / f"emnist-{fname}-images-idx3-ubyte").open("rb") as data_file, (
        DATA_DIR / f"emnist-{fname}-labels-idx1-ubyte"
    ).open("rb") as label_file:
        _, data_number, height, width = struct.unpack(">IIII", data_file.read(16))
        _, label_number = struct.unpack(">II", label_file.read(8))
        print(
            f">data({data_file.name}) split data:{data_number}, label:{label_number} group"
        )
        image_size = height * width

        for i in range(0, min(data_number, label_number)):
            _data = data_file.read(image_size)
            _label = struct.unpack(">B", label_file.read(1))[0]
            yield {
                "image": GrayscaleImage(
                    _data,
                    display_name=f"{i}",
                    shape=(height, width),
                ),
                "label": _label,
            }
