import numpy as np
import matplotlib.pyplot as plt

from starwhale import dataset


def show_image(image) -> None:
    plt.imshow(image, cmap="gray")
    plt.show(block=True)

ds_name = "mnist/version/latest"
ds = dataset(ds_name)
row = ds.fetch_one()
data = row.data["img"]
label = row.data["label"]
show_image(np.frombuffer(data.to_bytes(), dtype=np.uint8).reshape(data.shape))
print(label)
