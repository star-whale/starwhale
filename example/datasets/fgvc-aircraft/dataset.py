from pathlib import Path

from PIL import Image as PILImage

from starwhale import Image, MIMEType

ROOT_DIR = Path(__file__).parent
DATA_DIR = ROOT_DIR / "data" / "fgvc-aircraft-2013b" / "data"


def iter_family_test_item():
    return _do_iter_item("images_family_test.txt")


def iter_family_train_item():
    return _do_iter_item("images_family_train.txt")


def _do_iter_item(fname):
    with (DATA_DIR / fname).open("r") as f:
        for line in f:
            meta = line.split(" ", 1)  # ['1514522', 'Boeing 707']
            with (DATA_DIR / "images" / f"{meta[0]}.jpg").open("rb") as img:
                image_bytes = img.read()
            with PILImage.open(DATA_DIR / "images" / f"{meta[0]}.jpg") as img:
                shape = img.size
            yield {
                "image": Image(
                    fp=image_bytes,
                    display_name=meta[0],
                    shape=shape,
                    mime_type=MIMEType.JPEG,
                ),
                "family": meta[1].replace("\n", ""),
            }


def iter_family_test_item_raw():
    return _do_iter_raw_item("images_family_test.txt")


def iter_family_train_item_raw():
    return _do_iter_raw_item("images_family_train.txt")


def _do_iter_raw_item(fname):
    with (DATA_DIR / fname).open("r") as f:
        for line in f:
            meta = line.split(" ", 1)  # ['1514522', 'Boeing 707']
            _img = DATA_DIR / "images" / f"{meta[0]}.jpg"
            with PILImage.open(_img) as img:
                shape = img.size
            yield {
                "image": Image(
                    fp=str(_img.absolute()),
                    display_name=meta[0],
                    shape=shape,
                    mime_type=MIMEType.JPEG,
                ),
                "family": meta[1].replace("\n", ""),
            }
