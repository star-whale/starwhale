import numpy as np
import matplotlib.pyplot as plt

from starwhale import dataset


def show_image(image) -> None:
    plt.imshow(image, cmap="gray")
    plt.show(block=True)


def main():
    ds = dataset("emnist-digits-test/version/latest")
    for row in ds[0:10]:
        show_image(
            np.frombuffer(row.features.image.to_bytes(), dtype=np.uint8).reshape(
                row.features.image.shape
            )
        )
        print(f"label({row.features.label})")


if __name__ == "__main__":
    main()
