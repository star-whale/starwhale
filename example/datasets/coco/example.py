import io
import os

from PIL import Image as PILImage
from PIL import ImageDraw

from starwhale import dataset


def draw_bbox(img, bbox_view_):
    bbox1 = ImageDraw.Draw(img)
    x, y, w, h = bbox_view_
    bbox1.rectangle(
        [
            (x, y),
            (
                x + w,
                y + h,
            ),
        ],
        fill=None,
        outline="red",
    )


def local():
    ds = dataset("coco/version/latest", readonly=True)
    for row in ds:
        with PILImage.open(
            io.BytesIO(row.features.image.to_bytes())
        ) as img, PILImage.open(io.BytesIO(row.features.mask.to_bytes())).convert(
            "RGBA"
        ) as msk:
            for seg in row.features.segments_info:
                draw_bbox(img, seg["bbox"])

            msk.putalpha(127)
            img.paste(msk, (0, 0), mask=msk)
            img.show()


def remote():
    os.environ["SW_POD_NAME"] = "pod-1"
    ds = dataset(
        "http://localhost:8082/project/starwhale/dataset/coco-link/version/ge3taobuge3gkobzhbtdkzbrgb4ds4q"
    ).make_distributed_consumption(session_id="1", batch_size=10)

    for row in ds:
        with PILImage.open(
            io.BytesIO(row.features.image.to_bytes())
        ) as img, PILImage.open(io.BytesIO(row.features.mask.to_bytes())).convert(
            "RGBA"
        ) as msk:
            for seg in row.features.segments_info:
                draw_bbox(img, seg["bbox"])

            msk.putalpha(127)
            img.paste(msk, (0, 0), mask=msk)
            img.show()


if __name__ == "__main__":
    local()
