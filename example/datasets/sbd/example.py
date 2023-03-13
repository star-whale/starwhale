import io

import numpy
from PIL import Image as PILImage
from PIL import ImageDraw

from starwhale import dataset

ds_name = "sbd/version/latest"
ds = dataset(ds_name)
row = ds["2008_000202"]
data = row.features
with PILImage.open(io.BytesIO(data["image"].to_bytes())) as img:
    draw = ImageDraw.Draw(img)

    msk = PILImage.fromarray(
        numpy.frombuffer(data["boundaries"].to_bytes(), dtype=numpy.uint8).reshape(
            data["shape"]
        )
        * 50
    ).convert("RGBA")
    msk.putalpha(127)
    img.paste(msk, (0, 0), mask=msk)
    img.show()
