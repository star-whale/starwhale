import io

from PIL import Image as PILImage

from starwhale import dataset


def main():
    ds = dataset("cifar100-test/version/latest")
    for row in ds.head(n=10):
        with PILImage.open(io.BytesIO(row.features.image.to_bytes())) as img:
            img.show()
        print(
            f"other data: "
            f"fine({row.features['coarse_label_name']}[{row.features['coarse_label']}]), "
            f"coarse({row.features['fine_label_name']}[{row.features['fine_label']}])"
        )


if __name__ == "__main__":
    main()
