from __future__ import annotations

import json
from pathlib import Path
from collections import defaultdict

from tqdm import tqdm
from utils import download, extract_zip, get_name_by_coco_category_id
from ultralytics.data.converter import coco91_to_coco80_class

from starwhale import Image, dataset, init_logger
from starwhale.utils import console
from starwhale.base.data_type import BoundingBox

init_logger(3)

ROOT = Path(__file__).parent
DATA_DIR = ROOT / "data" / "coco2017"

# The coco2017 val set is from https://cocodataset.org/#download.


def build() -> None:
    _zip_path = DATA_DIR / "val2017.zip"
    download(
        "https://starwhale-examples.oss-cn-beijing.aliyuncs.com/dataset/coco2017/val2017.zip",
        _zip_path,
    )
    extract_zip(_zip_path, DATA_DIR, DATA_DIR / "val2017/000000000139.jpg")

    _zip_path = DATA_DIR / "annotations_trainval2017.zip"
    download(
        "https://starwhale-examples.oss-cn-beijing.aliyuncs.com/dataset/coco2017/annotations_trainval2017.zip",
        _zip_path,
    )
    json_path = DATA_DIR / "annotations/instances_val2017.json"
    extract_zip(_zip_path, DATA_DIR, json_path)

    coco_classes = coco91_to_coco80_class()

    with json_path.open() as f:
        content = json.load(f)
        annotations = defaultdict(list)
        for ann in content["annotations"]:
            class_id = coco_classes[ann["category_id"] - 1]
            annotations[ann["image_id"]].append(
                {
                    "bbox": BoundingBox(*ann["bbox"]),
                    "class_id": class_id,
                    "class_name": get_name_by_coco_category_id(class_id),
                }
            )

        with dataset("coco_val2017") as ds:
            for image in tqdm(content["images"]):
                name = image["file_name"].split(".jpg")[0]
                for ann in annotations[image["id"]]:
                    bbox = ann["bbox"]
                    ann["darknet_bbox"] = [
                        (bbox.x + bbox.width / 2) / image["width"],
                        (bbox.y + bbox.height / 2) / image["height"],
                        bbox.width / image["width"],
                        bbox.height / image["height"],
                    ]
                ds[name] = {
                    "image": Image(DATA_DIR / "val2017" / image["file_name"]),
                    "annotations": annotations[image["id"]],
                }
            console.print("commit dataset...")
            ds.commit()

    console.print(f"{ds} has been built successfully!")


if __name__ == "__main__":
    build()
