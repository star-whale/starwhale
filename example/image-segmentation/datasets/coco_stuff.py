from __future__ import annotations

import json
import zipfile
from pathlib import Path
from collections import defaultdict

import requests
from tqdm import tqdm

from starwhale import Image, dataset, init_logger
from starwhale.utils import console
from starwhale.base.data_type import COCOObjectAnnotation

# set Starwhale Python SDK logger level to 3 (DEBUG)
init_logger(3)

ROOT = Path(__file__).parent
DATA_DIR = ROOT / "data" / "coco2017-stuff"
VAL_DIR_NAME = "val2017"
ANNOTATION_DIR_NAME = "annotations"


def extract(from_path: Path, to_path: Path, chk_path: Path) -> None:
    if not from_path.exists() or from_path.suffix != ".zip":
        raise ValueError(f"invalid zip file: {from_path}")

    if chk_path.exists() and chk_path.is_dir():
        console.log(f"skip extract {from_path}, dir {chk_path} already exists")
        return

    console.log(f"extract {from_path} to {to_path} ...")
    with zipfile.ZipFile(from_path, "r", zipfile.ZIP_STORED) as z:
        for file in tqdm(
            iterable=z.namelist(),
            total=len(z.namelist()),
            desc=f"extract {from_path.name}",
        ):
            z.extract(member=file, path=to_path)


def download(url: str, to_path: Path) -> None:
    if to_path.exists():
        console.log(f"skip download {url}, file {to_path} already exists")
        return

    to_path.parent.mkdir(parents=True, exist_ok=True)

    with requests.get(url, timeout=60, stream=True) as r:
        r.raise_for_status()
        size = int(r.headers.get("content-length", 0))
        with tqdm(
            total=size,
            unit="B",
            unit_scale=True,
            desc=f"download {url}",
            initial=0,
            unit_divisor=1024,
        ) as pbar:
            with to_path.open("wb") as f:
                for chunk in r.iter_content(chunk_size=8192):
                    f.write(chunk)
                    pbar.update(len(chunk))


def download_and_extract(root_dir: Path) -> None:
    _zip_fpath = DATA_DIR / "val2017.zip"
    # for speedup, fork from http://images.cocodataset.org/zips/val2017.zip
    download(
        "https://starwhale-examples.oss-cn-beijing.aliyuncs.com/dataset/coco2017-stuff/val2017.zip",
        _zip_fpath,
    )
    extract(_zip_fpath, root_dir, root_dir / VAL_DIR_NAME)

    _zip_fpath = DATA_DIR / "stuff_annotations_trainval2017.zip"
    # for speedup, fork from http://images.cocodataset.org/annotations/stuff_annotations_trainval2017.zip
    download(
        "https://starwhale-examples.oss-cn-beijing.aliyuncs.com/dataset/coco2017-stuff/stuff_annotations_trainval2017.zip",
        _zip_fpath,
    )
    extract(_zip_fpath, DATA_DIR, DATA_DIR / ANNOTATION_DIR_NAME)
    extract(
        DATA_DIR / ANNOTATION_DIR_NAME / "stuff_val2017_pixelmaps.zip",
        DATA_DIR / ANNOTATION_DIR_NAME,
        DATA_DIR / ANNOTATION_DIR_NAME / "stuff_val2017_pixelmaps",
    )


def build(root_dir: Path):
    json_path = root_dir / ANNOTATION_DIR_NAME / "stuff_val2017.json"
    with json_path.open() as f:
        content = json.load(f)

        annotations = defaultdict(list)
        for ann in content["annotations"]:
            coco_ann = COCOObjectAnnotation(
                id=ann["id"],
                image_id=ann["image_id"],
                category_id=ann["category_id"],
                area=ann["area"],
                bbox=ann["bbox"],
                iscrowd=ann["iscrowd"],
            )
            coco_ann.segmentation = ann["segmentation"]
            annotations[ann["image_id"]].append(coco_ann)

        console.log("start to build dataset")
        with dataset("coco2017-stuff-val") as ds:
            for image in content["images"]:
                ds[image["id"]] = {
                    "image": Image(
                        fp=root_dir / VAL_DIR_NAME / image["file_name"],
                        shape=[image["width"], image["height"], 3],
                    ),
                    "coco_url": image["coco_url"],
                    "date_captured": image["date_captured"],
                    "annotations": annotations[image["id"]],
                    "pixelmaps": Image(
                        fp=root_dir
                        / ANNOTATION_DIR_NAME
                        / "stuff_val2017_pixelmaps"
                        / image["file_name"].replace(".jpg", ".png"),
                    ),
                }

            ds.commit()

        console.log(f"{ds} has been built")


if __name__ == "__main__":
    download_and_extract(DATA_DIR)
    build(DATA_DIR)
