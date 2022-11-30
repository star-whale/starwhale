import io
import os

from PIL import Image as PILImage
from PIL import ImageDraw

from starwhale import URI, URIType, get_data_loader, get_dataset_consumption


def draw_bbox(img, bbox_view_):
    bbox1 = ImageDraw.Draw(img)
    x, y, w, h = bbox_view_
    bbox1.rectangle(
        [
            (x, y),
            (
                x + w,
                y + h,
            ),
        ],
        fill=None,
        outline="red",
    )


def raw():
    uri = URI("coco-raw/version/latest", expected_type=URIType.DATASET)
    for idx, data, annotations in get_data_loader(uri, 0, 1):
        with PILImage.open(io.BytesIO(data.fp)) as img, PILImage.open(
            io.BytesIO(annotations["mask"].to_bytes(uri))
        ).convert("RGBA") as msk:
            for seg in annotations["segments_info"]:
                draw_bbox(img, seg["bbox"])

            msk.putalpha(127)
            img.paste(msk, (0, 0), mask=msk)
            img.show()


def link():
    uri = URI("coco-link/version/latest", expected_type=URIType.DATASET)
    for idx, data, annotations in get_data_loader(uri, 0, 1):
        with PILImage.open(io.BytesIO(data.fp)) as img, PILImage.open(
            io.BytesIO(annotations["mask"].to_bytes(uri))
        ).convert("RGBA") as msk:
            for seg in annotations["segments_info"]:
                draw_bbox(img, seg["bbox"])

            msk.putalpha(127)
            img.paste(msk, (0, 0), mask=msk)
            img.show()


def link_remote():
    os.environ["SW_POD_NAME"] = "pod-1"
    uri = URI(
        "http://localhost:8082/project/starwhale/dataset/coco-link/version/ge3taobuge3gkobzhbtdkzbrgb4ds4q",
        expected_type=URIType.DATASET,
    )
    for idx, data, annotations in get_data_loader(
        uri,
        session_consumption=get_dataset_consumption(
            uri, session_id="1", batch_size=10, instance_uri="http://localhost:8082"
        ),
    ):
        with PILImage.open(io.BytesIO(data.fp)) as img, PILImage.open(
            io.BytesIO(annotations["mask"].to_bytes(uri))
        ).convert("RGBA") as msk:
            for seg in annotations["segments_info"]:
                draw_bbox(img, seg["bbox"])

            msk.putalpha(127)
            img.paste(msk, (0, 0), mask=msk)
            img.show()


if __name__ == "__main__":
    link()
