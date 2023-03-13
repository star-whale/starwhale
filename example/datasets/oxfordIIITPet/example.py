import io
import json

import numpy
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


ds_name = "oxfordIIITPet/version/latest"
ds = dataset(ds_name)
row = ds.fetch_one()
data = row.features
image = data.pop("image")
mask = data.pop("mask")
with PILImage.open(io.BytesIO(image.to_bytes())) as img, PILImage.open(
    io.BytesIO(mask.to_bytes())
).convert("RGBA") as msk:
    draw = ImageDraw.Draw(img)
    pets = data["pets"]
    for pet in pets:
        draw_bbox(draw, pet.pop("bbox"))

    _npy = numpy.asarray(msk) * 50
    msk = PILImage.fromarray(_npy)
    msk.putalpha(127)
    img.paste(msk, (0, 0), mask=msk)
    draw.text((28, 36), json.dumps(data, indent=2), fill="red")
    img.show()
