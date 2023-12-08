from __future__ import annotations

from pathlib import Path

from utils import download, extract_zip, get_name_by_coco_category_id

from starwhale import Image, dataset, BoundingBox, init_logger
from starwhale.utils import console

init_logger(3)

ROOT = Path(__file__).parent
DATA_DIR = ROOT / "data" / "coco128"

# Copy from https://www.kaggle.com/datasets/ultralytics/coco128.


def build() -> None:
    _zip_path = DATA_DIR / "coco128.zip"
    download("https://ultralytics.com/assets/coco128.zip", _zip_path)
    extract_zip(
        _zip_path, DATA_DIR, DATA_DIR / "coco129/images/train2017/000000000650.jpg"
    )

    with dataset("coco128") as ds:
        for img_path in (DATA_DIR / "coco128/images/train2017").glob("*.jpg"):
            name = img_path.name.split(".jpg")[0]

            # YOLO Darknet format: https://docs.plainsight.ai/labels/exporting-labels/yolo
            # Format: <object-class> <x_center> <y_center> <width> <height>
            # Meaning: object-class> - zero-based index representing the class in obj.names from 0 to (classes-1).
            #         <x_center> <y_center> <width> <height> - float values relative to width and height of image, it can be equal from (0.0 to 1.0].
            #         <x_center> = <absolute_x> / <image_width>
            #         <height> = <absolute_height> / <image_height>

            annotations = []
            image = Image(img_path)
            i_width, i_height = image.to_pil().size

            label_path = DATA_DIR / "coco128/labels/train2017" / f"{name}.txt"
            if not label_path.exists():
                continue

            for line in label_path.read_text().splitlines():
                class_id, x, y, w, h = line.split()
                class_id, x, y, w, h = (
                    int(class_id),
                    float(x),
                    float(y),
                    float(w),
                    float(h),
                )
                annotations.append(
                    {
                        "class_id": class_id,
                        "class_name": get_name_by_coco_category_id(class_id),
                        "darknet_bbox": [x, y, w, h],
                        "bbox": BoundingBox.from_darknet(x, y, w, h, i_width, i_height),
                    }
                )

            ds[name] = {"image": image, "annotations": annotations}

        console.print("commit dataset...")
        ds.commit()

    console.print(f"{ds} has been built successfully!")


if __name__ == "__main__":
    build()
