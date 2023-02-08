import io

from PIL import Image as PILImage

from starwhale import URI, URIType, get_data_loader


def main():
    uri = URI("cifar100-test/version/latest", expected_type=URIType.DATASET)
    for idx, data in get_data_loader(uri, 0, 10):
        with PILImage.open(io.BytesIO(data["image"].to_bytes())) as img:
            img.show()
        print(
            f"other data: "
            f"fine({data['coarse_label_name']}[{data['coarse_label']}]), "
            f"coarse({data['fine_label_name']}[{data['fine_label']}])"
        )


if __name__ == "__main__":
    main()
