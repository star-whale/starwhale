import io

from PIL import Image as PILImage
from PIL import ImageEnhance

from starwhale import dataset

ds_name = "pfp/version/latest"
ds = dataset(ds_name)
row = ds.fetch_one()
image = row.features["image"]
mask = row.features["mask"]
with PILImage.open(io.BytesIO(image.to_bytes())) as img, PILImage.open(
    io.BytesIO(mask.to_bytes())
).convert("RGBA") as msk:
    enhancer = ImageEnhance.Brightness(msk)
    msk = enhancer.enhance(100)
    msk.putalpha(127)
    msk.show()
    img.paste(msk, (0, 0), mask=msk)
    img.show()
