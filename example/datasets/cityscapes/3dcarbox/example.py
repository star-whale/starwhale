import io

import matplotlib.pyplot as plt
from PIL import Image as PILImage
from PIL import ImageDraw

from starwhale import dataset


def draw_bbox(draw, bbox_view_):
    x, y, w, h = bbox_view_.x, bbox_view_.y, bbox_view_.width, bbox_view_.height
    draw.rectangle(
        [
            (x, y),
            (
                x + w,
                y + h,
            ),
        ],
        fill=None,
        outline="red",
    )


fig, ax = plt.subplots(
    1, 2, figsize=(25, 10), gridspec_kw={"wspace": 0.1, "hspace": 0.1}
)
ds_name = "cityscapes_3dcarbox/version/latest"
ds = dataset(ds_name)
row = ds.fetch_one()
data = row.features
l_img_bytes = data["left_image_8bit"].to_bytes()
r_img_bytes = data["right_image_8bit"].to_bytes()
objs = data["objects"]
with PILImage.open(io.BytesIO(l_img_bytes)) as img:
    draw = ImageDraw.Draw(img)
    for obj in objs:
        draw_bbox(draw, obj["2d_sw"].bbox_a)
        draw_bbox(draw, obj["2d_sw"].bbox_b)
    ax[0].imshow(img)

with PILImage.open(io.BytesIO(r_img_bytes)) as img:
    draw = ImageDraw.Draw(img)
    for obj in objs:
        draw_bbox(draw, obj["2d_sw"].bbox_a)
        draw_bbox(draw, obj["2d_sw"].bbox_b)
    ax[1].imshow(img)

fig.show()
