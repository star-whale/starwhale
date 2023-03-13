import io

from PIL import Image as PILImage
from PIL import ImageDraw

from starwhale import dataset

ds_name = "kitti-object-birds/version/latest"
ds = dataset(ds_name)
row = ds.fetch_one()
image2 = row.features["image_2"]
image3 = row.features["image_3"]
label = row.features["label_2"]
velodyne = row.features["velodyne"]
print(len(velodyne.to_bytes()))
with PILImage.open(io.BytesIO(image2.to_bytes())) as img2, PILImage.open(
    io.BytesIO(image3.to_bytes())
) as img3:
    draw = ImageDraw.Draw(img2)
    draw.text((28, 36), label.to_str(), fill="red")
    img2.show()
    img3.show()
