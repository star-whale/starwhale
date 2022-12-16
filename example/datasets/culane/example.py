import io

import numpy
from PIL import Image as PILImage
from PIL import ImageDraw, Image
from starwhale import dataset, URI, URIType

ds_name = "culane/version/latest"
ds = dataset(ds_name)
row = ds.fetch_one()
data = row.data
annotations = row.annotations
with PILImage.open(io.BytesIO(data.fp)) as img, PILImage.open(
    io.BytesIO(annotations["mask"].to_bytes(ds_name))
).convert("RGBA") as msk:
    draw = ImageDraw.Draw(img)
    for line in annotations["lines"]:
        draw.line([float(l) for l in line.split()], width=3, fill="red")
    _npy = numpy.asarray(msk) * 50
    msk = Image.fromarray(_npy)
    msk.putalpha(127)
    img.paste(msk, (0, 0), mask=msk)
    img.show()
