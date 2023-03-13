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


ds_name = "wider_face/version/latest"
ds = dataset(ds_name)
row = ds["0--Parade/0_Parade_marchingband_1_205.jpg"]
data = row.features
with PILImage.open(io.BytesIO(data["image"].to_bytes())) as img:
    draw = ImageDraw.Draw(img)
    for face in data["faces"]:
        draw_bbox(draw, face["bbox"])
    img.show()
