import io

import matplotlib.pyplot as plt
from PIL import Image as PILImage
from PIL import ImageDraw

from starwhale import dataset

fig, ax = plt.subplots(
    1, 2, figsize=(25, 10), gridspec_kw={"wspace": 0.1, "hspace": 0.1}
)
ds_name = "cityscapes_disparity/version/latest"
ds = dataset(ds_name)
print(ds.info)
row = ds.fetch_one()
data = row.features
l_img_bytes = data["left_image_8bit"].to_bytes()
r_img_bytes = data["right_image_8bit"].to_bytes()
msk_img_bytes = data["disparity_mask"].to_bytes()
with PILImage.open(io.BytesIO(l_img_bytes)) as img, PILImage.open(
    io.BytesIO(msk_img_bytes)
).convert("RGBA") as msk:
    draw = ImageDraw.Draw(img)
    msk.putalpha(60)
    img.paste(msk, (0, 0), mask=msk)
    ax[0].imshow(img)

with PILImage.open(io.BytesIO(r_img_bytes)) as img, PILImage.open(
    io.BytesIO(msk_img_bytes)
).convert("RGBA") as msk:
    draw = ImageDraw.Draw(img)
    msk.putalpha(60)
    img.paste(msk, (0, 0), mask=msk)
    ax[1].imshow(img)

fig.show()
