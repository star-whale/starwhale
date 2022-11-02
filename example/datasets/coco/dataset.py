import json
from pathlib import Path

from starwhale import Link, Image, BoundingBox, MIMEType

ROOT_DIR = Path(__file__).parent
DATA_DIR = ROOT_DIR / "data"


def images2dict(imgs):
    _dict = {}
    for img in imgs:
        _dict[img["id"]] = img
    return _dict


def do_iter_item():
    with (DATA_DIR / "annotations" / "panoptic_val2017.json").open("r") as f:
        index = json.load(f)
        img_dict = images2dict(index["images"])
        for anno in index["annotations"]:
            img_meta = img_dict[anno["image_id"]]
            img_name = img_meta["file_name"]
            img_pth = DATA_DIR / "val2017" / img_name
            img_shape = (img_meta["height"], img_meta["width"])
            msk_f_name = anno["file_name"]
            msk_f_pth = DATA_DIR / "annotations" / "panoptic_val2017" / msk_f_name
            segs_info = anno["segments_info"]
            for sg in segs_info:
                x, y, w, h = sg["bbox"]
                sg["bbox_view"] = BoundingBox(
                    x=x, y=y, width=w, height=h
                )

            anno["mask"] = Link(
                auth=None,
                with_local_fs_data=True,
                data_type=Image(display_name=msk_f_name, shape=img_shape, mime_type=MIMEType.PNG),
                uri=str(msk_f_pth.absolute()),
            )
            yield Link(
                uri=str(img_pth.absolute()),
                data_type=Image(display_name=img_name, shape=img_shape),
                with_local_fs_data=True,
            ), anno
