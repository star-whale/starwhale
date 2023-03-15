import io

from PIL import Image as PILImage
from PIL import ImageDraw

from starwhale import dataset

ds_name = "celeba-align/version/latest"
ds = dataset(ds_name)
row = ds["000019.jpg"]
data = row.features
with PILImage.open(io.BytesIO(data["image"].to_bytes())) as img:
    draw = ImageDraw.Draw(img)
    draw.point(data["landmark"]["left_eye"].to_list(), fill="green")
    draw.point(data["landmark"]["right_eye"].to_list(), fill="green")
    draw.point(data["landmark"]["left_mouse"].to_list(), fill="green")
    draw.point(data["landmark"]["right_mouse"].to_list(), fill="green")
    draw.point(data["landmark"]["nose"].to_list(), fill="green")
    img.show()
