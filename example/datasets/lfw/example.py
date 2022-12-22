import io

from PIL import Image as PILImage
from PIL import ImageDraw

from starwhale import dataset

ds_name = "lfw/version/latest"
ds = dataset(ds_name)
row = ds.fetch_one()
data = row.data
annotations = row.annotations
with PILImage.open(io.BytesIO(data.fp)) as img:
    draw = ImageDraw.Draw(img)
    draw.text((28, 36), annotations["identity"], fill="red")
    img.show()
