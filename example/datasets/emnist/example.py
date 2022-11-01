import numpy as np
import matplotlib.pyplot as plt

from starwhale import URI, URIType, get_data_loader


def show_image(image) -> None:
    plt.imshow(image, cmap='gray')
    plt.show(block=True)


def main():
    uri = URI("emnist-digits-test/version/latest", expected_type=URIType.DATASET)
    for idx, data, annotations in get_data_loader(uri, 0, 10):
        show_image(np.frombuffer(data.fp, dtype=np.uint8).reshape((28, 28)))
        print(
            f"[{idx}] data:{data.type.name}-{data.mime_type}-{data.display_name}, annotations: "
            f"label({annotations['label']})"
        )


if __name__ == "__main__":
    main()
