import io

from PIL import Image as PILImage
from PIL import ImageDraw

from starwhale import dataset

ds_name = "kitti-lane/version/latest"
ds = dataset(ds_name)
row = ds.fetch_one()
image = row.features["image"]
label_road_pic = row.features["label_road_pic"]
label_lane_pic = row.features["label_lane_pic"]
label = row.features["label_text"]
with PILImage.open(io.BytesIO(image.to_bytes())) as img2, PILImage.open(
    io.BytesIO(label_road_pic.to_bytes())
) as img3, PILImage.open(io.BytesIO(label_lane_pic.to_bytes())) as img4:
    draw = ImageDraw.Draw(img2)
    draw.text((28, 36), label.to_str("ISO-8859-1"), fill="red")
    img2.show()
    img3.show()
    img4.show()
