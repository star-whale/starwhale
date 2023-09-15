import typing

import numpy as np
import matplotlib.pyplot as plt

from starwhale import dataset


def show_image(image: typing.Any) -> None:
    plt.imshow(image, cmap="gray")
    plt.show(block=True)  # type: ignore


ds_name = "mnist/version/latest"
ds = dataset(ds_name)
row = ds.fetch_one()
show_image(np.frombuffer(row.features.img.to_bytes(), dtype=np.uint8).reshape(row.features.img.shape))  # type: ignore
print(row.features.label)  # type: ignore
