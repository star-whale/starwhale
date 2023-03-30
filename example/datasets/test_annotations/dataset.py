import io

from PIL import Image as PILImage

from starwhale import (
    URI,
    Link,
    Text,
    Image,
    dataset,
    URIType,
    MIMEType,
    ClassLabel,
    BoundingBox,
    COCOObjectAnnotation,
)
from starwhale.api._impl.data_store import Link as PlainLink
from starwhale.api._impl.data_store import _get_type


def iter_simple_bin_item():
    for i in range(1, 9):
        data = {
            "index": i,
            "text": Text(f"data-{i}"),
            "label": f"label-{i}",
            "label_float": 0.100092 + i,
            "list_int": [j for j in range(0, i)],
            "bytes": f"label-{i}".encode(),
            "link": PlainLink(f"uri-{i}", f"display-{i}"),
        }

        yield f"idx-{i}", data


def iter_swds_bin_item():
    for i in range(1, 9):
        coco = COCOObjectAnnotation(
            id=i,
            image_id=i,
            category_id=i,
            area=i * 10,
            bbox=BoundingBox(i, i, i + 1, i + 10),
            iscrowd=1,
        )
        coco.segmentation = [1, 2, 3, 4]
        data = {
            "index": i,
            "text": Text(f"data-{i}"),
            "label": f"label-{i}",
            "label_float": 0.100092 + i,
            "list_int": [j for j in range(0, i)],
            "bytes": f"label-{i}".encode(),
            "bbox": BoundingBox(i, i, i + 10, i + 10),
            "plain_link": PlainLink(f"uri-{i}", f"display-{i}"),
            "list_bbox": [
                BoundingBox(i, i, i + 10, i + 10),
                BoundingBox(i, i, i + 20, i + 20),
            ],
            "coco": coco,
            "dict": {"a": 1, "b": 2, "c": {"d": 1, "e": ClassLabel([1, 2, 3])}},
            "artifact_s3_link": Link(
                f"s3://minioadmin:minioadmin@10.131.0.1:9000/users/dataset/PennFudanPed/PedMasks/FudanPed0000{i}_mask.png",
                data_type=Image(display_name=f"{i}_mask", mime_type=MIMEType.PNG),
            ),
        }

        yield f"idx-{i}", data


def _load_dataset(uri):
    print("-" * 20)
    print(uri)
    ds = dataset(uri, readonly=True)
    for idx, features in ds["idx-0":"idx-2"]:
        print(
            f"---->[{idx}] {features.text.content} data-length:{len(features.text.to_bytes())}"
        )
        ats = "\n".join(
            [f"\t{k}-{v}-{type(v)}-{_get_type(v)}" for k, v in features.items()]
        )
        print(f"data: {len(features)}\n {ats}")
        if "artifact_s3_link" in features:
            link = features["artifact_s3_link"]
            content = link.to_bytes(uri)
            image = PILImage.open(io.BytesIO(content))
            print(
                f"{link.data_type.display_name} image: width:{image.width}, height:{image.height}, size:{image.size}"
            )


def load_local_dataset():
    uri = URI("simple_annotations/version/latest", expected_type=URIType.DATASET)
    _load_dataset(uri)

    uri = URI("complex_annotations/version/latest", expected_type=URIType.DATASET)
    _load_dataset(uri)


def load_cloud_dataset():
    # need port-forward and starwhale-minio host alias in the local environment: kubectl port-forward -n starwhale ${minio-pod} 9000:9000
    uri = URI(
        "cloud://pre-tianwei/project/datasets/dataset/simple_annotations/version/muytcnrwgbsggyldmy2tqojrnfrxony",
        expected_type=URIType.DATASET,
    )
    _load_dataset(uri)

    uri = URI(
        "cloud://pre-tianwei/project/datasets/dataset/test_annotations/version/gnrwgnrtmnrgkzrymftggnjvmu4wo4i",
        expected_type=URIType.DATASET,
    )
    _load_dataset(uri)

    uri = URI(
        "cloud://pre-tianwei/project/datasets/dataset/complex_annotations/version/gu4dsmlggjrtemzuhe4tgn3fgbyxm6i",
        expected_type=URIType.DATASET,
    )
    _load_dataset(uri)


if __name__ == "__main__":
    load_local_dataset()
    load_cloud_dataset()
