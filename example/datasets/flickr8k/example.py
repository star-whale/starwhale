import io

from PIL import Image as PILImage
from PIL import ImageDraw

from starwhale import dataset

ds_name = "flickr8k/version/latest"
ds = dataset(ds_name)
row = ds.fetch_one()
data = row.data
with PILImage.open(io.BytesIO(data.fp)) as img:
    draw = ImageDraw.Draw(img)
    i = 0
    for label in row.annotations["labels"]:
        draw.text((28, 36 + i * 28), label, fill="red")
        i += 1
    img.show()
