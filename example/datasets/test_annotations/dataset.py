from starwhale import (
    URI,
    Link,
    Text,
    Audio,
    URIType,
    LinkAuth,
    MIMEType,
    ClassLabel,
    BoundingBox,
    GrayscaleImage,
    get_data_loader,
    COCOObjectAnnotation,
)
from starwhale.api._impl.data_store import Link as PlainLink
from starwhale.api._impl.data_store import _get_type


def iter_simple_bin_item():
    for i in range(0, 2):
        annotations = {
            "index": i,
            "label": f"label-{i}",
            "label_float": 0.100092 + i,
            "list_int": [j for j in range(0, i)],
            "bytes": f"label-{i}".encode(),
            "link": PlainLink(f"uri-{i}", f"display-{i}"),
        }

        yield f"idx-{i}", Text(f"data-{i}"), annotations


def iter_swds_bin_item():
    for i in range(0, 2):
        annotations = {
            "index": i,
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
            "artifact_local_link": Link(
                "path/to/file.png",
                data_type=GrayscaleImage(display_name=f"[{i}]", shape=(28, 28, 1)),
                with_local_fs_data=True,
            ),
            "artifact_s3_link": Link(
                "s3://bucket/key/file.mp3",
                auth=LinkAuth("default"),
                data_type=Audio(display_name=f"{i}-audio", mime_type=MIMEType.MP3),
            ),
        }

        yield f"idx-{i}", Text(f"data-{i}"), annotations


def _load_dataset(uri):
    print(uri)
    for idx, data, annotations in get_data_loader(uri, "idx-0", "idx-10"):
        print(f"---->[{idx}] {data}")
        ats = "\n".join(
            [f"\t{k}-{v}-{type(v)}-{_get_type(v)}" for k, v in annotations.items()]
        )
        print(f"annotations: {len(annotations)}\n {ats}")


def load_local_dataset():
    uri = URI("simple_annotations/version/latest", expected_type=URIType.DATASET)
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
        "cloud://pre-tianwei/project/datasets/dataset/complex_annotations/version/ge2dezlfgy4gcmzuhe4tgn3fnu3hm5a",
        expected_type=URIType.DATASET,
    )
    _load_dataset(uri)


if __name__ == "__main__":
    load_local_dataset()
    load_cloud_dataset()
