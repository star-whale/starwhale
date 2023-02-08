import io

from PIL import Image as PILImage

from starwhale import URI, URIType, get_data_loader


def main():
    uri = URI("fgvc-aircraft-family-test/version/latest", expected_type=URIType.DATASET)
    for idx, data in get_data_loader(uri, 0, 10):
        PILImage.open(io.BytesIO(data["image"].to_bytes())).show()
        print(f"label({data['family']})")


if __name__ == "__main__":
    main()
