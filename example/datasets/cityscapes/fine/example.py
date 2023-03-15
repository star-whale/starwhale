import io

import matplotlib.pyplot as plt
from PIL import Image as PILImage
from PIL import ImageDraw, ImageEnhance

from starwhale import dataset


def draw_polygon(draw, polygon_view_):
    draw.polygon(
        [(p.x, p.y) for p in polygon_view_.points],
        fill=None,
        outline="red",
    )


fig, ax = plt.subplots(
    2, 2, figsize=(25, 16), gridspec_kw={"wspace": 0.1, "hspace": 0.1}
)
ds_name = "cityscapes_fine/version/latest"
ds = dataset(ds_name)
row = ds.fetch_one()
data = row.features
img_bytes = data["image"].to_bytes()
with PILImage.open(io.BytesIO(img_bytes)) as img, PILImage.open(
    io.BytesIO(data["color_mask"].to_bytes(ds_name))
).convert("RGBA") as msk:
    draw = ImageDraw.Draw(img)
    msk.putalpha(60)
    img.paste(msk, (0, 0), mask=msk)
    ax[0][0].imshow(img)

with PILImage.open(io.BytesIO(img_bytes)) as img, PILImage.open(
    io.BytesIO(data["instance_mask"].to_bytes(ds_name))
).convert("RGBA") as msk:
    draw = ImageDraw.Draw(img)
    msk.putalpha(60)
    img.paste(msk, (0, 0), mask=msk)
    ax[0][1].imshow(img)

with PILImage.open(io.BytesIO(img_bytes)) as img, PILImage.open(
    io.BytesIO(data["label_mask"].to_bytes(ds_name))
).convert("RGBA") as msk:
    draw = ImageDraw.Draw(img)
    enhancer = ImageEnhance.Brightness(msk)
    msk = enhancer.enhance(10)
    msk.putalpha(100)
    img.paste(msk, (0, 0), mask=msk)
    ax[1][0].imshow(img)

with PILImage.open(io.BytesIO(img_bytes)) as img:
    draw = ImageDraw.Draw(img)
    for obj in data["polygons"]["objects"]:
        draw_polygon(draw, obj["polygon"])
    ax[1][1].imshow(img)

fig.show()
