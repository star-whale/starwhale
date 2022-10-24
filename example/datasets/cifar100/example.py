from starwhale import URI, URIType, get_data_loader


def main():
    uri = URI("cifar100-test/version/latest", expected_type=URIType.DATASET)
    for idx, data, annotations in get_data_loader(uri, 0, 10):
        print(
            f"[{idx}] data:{data.type.name}-{data.mime_type}-{data.display_name}, annotations: "
            f"fine({annotations['coarse_label_name']}[{annotations['coarse_label']}]), "
            f"coarse({annotations['fine_label_name']}[{annotations['fine_label']}])"
        )


if __name__ == "__main__":
    main()
