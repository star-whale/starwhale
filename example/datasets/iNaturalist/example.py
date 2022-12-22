import io
import json

from PIL import Image as PILImage
from PIL import ImageDraw

from starwhale import dataset

ds_name = "iNaturalist-val/version/latest"
ds = dataset(ds_name)
row = ds.fetch_one()
data = row.data
annotations = row.annotations
with PILImage.open(io.BytesIO(data.fp)) as img:
    draw = ImageDraw.Draw(img)
    draw.text((28, 36), json.dumps(annotations, indent=2), fill="red")
    img.show()
