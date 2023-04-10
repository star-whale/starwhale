import io

from PIL import Image as PILImage

from starwhale import dataset


def main():
    ds = dataset("fgvc-aircraft-family-test/version/latest")
    for row in ds[0:10]:
        PILImage.open(io.BytesIO(row.features.image.to_bytes())).show()
        print(f"label({row.features.family})")


if __name__ == "__main__":
    main()
