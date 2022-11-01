import io

from PIL import Image as PILImage

from starwhale import URI, URIType, get_data_loader


def main():
    uri = URI("fgvc-aircraft-family-test/version/latest", expected_type=URIType.DATASET)
    for idx, data, annotations in get_data_loader(uri, 0, 10):
        PILImage.open(io.BytesIO(data.fp)).show()
        print(
            f"[{idx}] data:{data.type.name}-{data.mime_type}-{data.display_name}, annotations: "
            f"label({annotations['family']})"
        )


if __name__ == "__main__":
    main()
