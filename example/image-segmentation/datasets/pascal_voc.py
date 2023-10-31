from __future__ import annotations

import xml.etree.ElementTree as ET
from pathlib import Path

from starwhale import Image, dataset, BoundingBox, init_logger
from starwhale.utils import console

try:
    from .utils import extract, download
except ImportError:
    from utils import extract, download

# set Starwhale Python SDK logger level to 3 (DEBUG)
init_logger(3)

ROOT = Path(__file__).parent
DATA_DIR = ROOT / "data" / "pascal-voc2012"


def download_and_extract(root_dir: Path):
    _tar_fpath = DATA_DIR / "VOCtrainval_11-May-2012.tar"
    # for speedup, fork from http://host.robots.ox.ac.uk/pascal/VOC/voc2012/VOCtrainval_11-May-2012.tar
    download(
        "https://starwhale-examples.oss-cn-beijing.aliyuncs.com/dataset/pascal2012-voc/VOCtrainval_11-May-2012.tar",
        _tar_fpath,
    )
    extract(_tar_fpath, root_dir, DATA_DIR / "VOCdevkit/VOC2012/SegmentationObject")


def build(root_dir: Path):
    """PASCAL VOC 2012 dataset folder structure:
    - Annotations
        - 2011_000234.xml  <-- annotations description
    - SegmentationObject
        - 2011_000234.png  <-- segmentation png
    - SegmentationClass
        - 2011_000234.png  <-- segmentation png
    - JPEGImages
        - 2011_000234.jpg  <-- original jpeg
    - ImageSets
        - Action
        - Layout
        - Main
        - Segmentation

    We only use segmentation related files to build dataset.
    """

    console.log("start to build dataset")
    data_dir = root_dir / "VOCdevkit/VOC2012"
    with dataset("pascal2012-voc-segmentation") as ds:
        for obj_path in (data_dir / "SegmentationObject").iterdir():
            if not obj_path.is_file() or obj_path.suffix != ".png":
                continue

            name = obj_path.name.split(".")[0]
            annotations = ET.parse(data_dir / "Annotations" / f"{name}.xml").getroot()
            size_node = annotations.find("size")

            objects = []
            for obj in annotations.findall("object"):
                x_min = int(obj.find("bndbox").find("xmin").text)
                x_max = int(obj.find("bndbox").find("xmax").text)
                y_min = int(obj.find("bndbox").find("ymin").text)
                y_max = int(obj.find("bndbox").find("ymax").text)

                objects.append(
                    {
                        "name": obj.find("name").text,
                        "bbox": BoundingBox(
                            x=x_min, y=y_min, width=x_max - x_min, height=y_max - y_min
                        ),
                        "difficult": obj.find("difficult").text,
                        "pose": obj.find("pose").text,
                    }
                )

            ds[name] = {
                "segmentation_object": Image(obj_path),
                "segmentation_class": Image(
                    data_dir / "SegmentationClass" / f"{name}.png"
                ),
                "original": Image(
                    data_dir / "JPEGImages" / f"{name}.jpg",
                    shape=[
                        int(size_node.find("width").text),
                        int(size_node.find("height").text),
                        int(size_node.find("depth").text),
                    ],
                ),
                "segmented": annotations.find("segmented").text,
                "objects": objects,
            }
        ds.commit()
    console.log(f"{ds} has been built")


if __name__ == "__main__":
    download_and_extract(DATA_DIR)
    build(DATA_DIR)
