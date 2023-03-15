import io

from PIL import Image as PILImage
from PIL import ImageDraw

from starwhale import dataset


def draw_bbox(img, bbox_view_):
    bbox1 = ImageDraw.Draw(img)
    x, y, w, h = bbox_view_.x, bbox_view_.y, bbox_view_.width, bbox_view_.height
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


def show():
    ds_name = "image-net/version/latest"
    ds = dataset(ds_name)
    row = ds.fetch_one()
    data = row.features
    with PILImage.open(io.BytesIO(data["image"].to_bytes())) as img:
        for obj in data["object"]:
            draw_bbox(img, obj["bbox_view"])
        img.show()


if __name__ == "__main__":
    show()
