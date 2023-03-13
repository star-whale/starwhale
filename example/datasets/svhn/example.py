import io

from PIL import Image as PILImage
from PIL import ImageDraw

from starwhale import dataset


def draw_bbox(draw, bbox_view_):
    x, y, w, h = bbox_view_.x, bbox_view_.y, bbox_view_.width, bbox_view_.height
    draw.rectangle(
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


ds_name = "svhn/version/latest"
ds = dataset(ds_name)
row = ds.fetch_one()
data = row.features
with PILImage.open(io.BytesIO(data["image"].to_bytes())) as img:
    draw = ImageDraw.Draw(img)
    for num in data["numbers"]:
        draw.text(
            (
                num["bbox"].x + num["bbox"].width / 2,
                num["bbox"].y + num["bbox"].height / 2,
            ),
            str(num["label"]),
            fill="red",
        )
        draw_bbox(draw, num["bbox"])

    img.show()
