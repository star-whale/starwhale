import io

from PIL import Image as PILImage
from PIL import ImageDraw

from starwhale import dataset

ds_name = "country211/version/latest"
ds = dataset(ds_name)
row = ds.fetch_one()
data = row.data
with PILImage.open(io.BytesIO(data["image"].to_bytes())) as img:
    draw = ImageDraw.Draw(img)
    draw.text((28, 36), data["label"], fill="red")
    img.show()
