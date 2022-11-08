from starwhale import (
    Text,
    BoundingBox,
    COCOObjectAnnotation,
    URI,
    URIType,
    get_data_loader,
    ClassLabel,
)
from starwhale.api._impl.data_store import _get_type


def iter_swds_bin_item():
    for i in range(0, 100):
        annotations = {
            "index": i,
            "label": f"label-{i}",
            "label_float": 0.100092 + i,
            "list_int": [j for j in range(0, i)],
            "bytes": f"label-{i}".encode(),
            "bbox": BoundingBox(i, i, i + 10, i + 10),
            "list_bbox": [
                BoundingBox(i, i, i + 10, i + 10),
                BoundingBox(i, i, i + 20, i + 20),
            ],
            "coco": COCOObjectAnnotation(
                id=i,
                image_id=i,
                category_id=i,
                segmentation=[1, 2, 3, 4],
                area=i * 10,
                bbox=BoundingBox(i, i, i + 1, i + 10),
                iscrowd=1,
            ),
            "dict": {"a": 1, "b": 2, "c": {"d": 1, "e": ClassLabel([1, 2, 3])}},
        }

        yield f"idx-{i}", Text(f"data-{i}"), annotations


def dataset_load_test():
    uri = URI("complex_annotations/version/latest", expected_type=URIType.DATASET)
    for idx, data, annotations in get_data_loader(uri, "idx-0", "idx-10"):
        print(f"---->[{idx}] {data}")
        ats = "\n".join(
            [f"\t{k}-{v}-{type(v)}-{_get_type(v)}" for k, v in annotations.items()]
        )
        print(f"annotations: {len(annotations)}\n {ats}")


if __name__ == "__main__":
    dataset_load_test()
