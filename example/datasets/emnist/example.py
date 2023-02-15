import numpy as np
import matplotlib.pyplot as plt

from starwhale import URI, URIType, get_data_loader


def show_image(image) -> None:
    plt.imshow(image, cmap="gray")
    plt.show(block=True)


def main():
    uri = URI("emnist-digits-test/version/latest", expected_type=URIType.DATASET)
    for idx, data in get_data_loader(uri, 0, 10):
        show_image(
            np.frombuffer(data["image"].to_bytes(), dtype=np.uint8).reshape(
                data["image"].shape
            )
        )
        print(f"label({data['label']})")


if __name__ == "__main__":
    main()
