import io

from PIL import Image as PILImage
from PIL import ImageDraw

from starwhale import dataset

ds_name = "dtd/version/latest"
ds = dataset(ds_name)
row = ds["banded/banded_0063.jpg"]
data = row.features
with PILImage.open(io.BytesIO(data["image"].to_bytes())) as img:
    draw = ImageDraw.Draw(img)
    i = 0
    for label in data["labels"]:
        draw.text((28, 36 + i * 28), label, fill="blue")
        i += 1
    img.show()
