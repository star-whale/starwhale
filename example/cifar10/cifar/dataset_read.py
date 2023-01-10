import io

from PIL import Image as PILImage

from starwhale import dataset

ds_name = "cifar10/version/latest"
ds = dataset(ds_name)
row = ds.fetch_one()
image = row.data["image"]
label = row.data["label"]
label_display_name = row.data["label_display_name"]
with PILImage.open(io.BytesIO(image.to_bytes())) as img:
    img.show()
    print(f"label: {label}; label_display_name:{label_display_name}")
