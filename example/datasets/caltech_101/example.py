import io

from PIL import Image as PILImage
from PIL import ImageDraw

from starwhale import dataset

ds_name = "caltech-101/version/latest"
ds = dataset(ds_name)
row = ds.fetch_one()
image = row.features["image"]
label = row.features["label"]
with PILImage.open(io.BytesIO(image.to_bytes())) as img:
    draw = ImageDraw.Draw(img)
    draw.text((28, 36), label, fill="red")
    img.show()
