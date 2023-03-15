import numpy as np
import matplotlib.pyplot as plt

from starwhale import dataset


def show_image(image) -> None:
    plt.imshow(image, cmap="gray")
    plt.show(block=True)


ds_name = "fer2013/version/latest"
ds = dataset(ds_name)
row = ds.fetch_one()
data = row.features
show_image(
    np.frombuffer(data["image"].to_bytes(), dtype=np.uint8).reshape(data["image"].shape)
)
print(data["label"])
