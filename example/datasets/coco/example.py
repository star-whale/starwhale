import io

from PIL import Image as PILImage
from PIL import ImageDraw

from starwhale import URI, URIType, get_data_loader


def draw_bbox(img, bbox_view_):
    bbox1 = ImageDraw.Draw(img)
    bbox1.rectangle(
        [
            (bbox_view_.x, bbox_view_.y),
            (
                bbox_view_.x + bbox_view_.width,
                bbox_view_.y + bbox_view_.height,
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
                draw_bbox(img, seg.bbox_view)

            msk.putalpha(127)
            img.paste(msk, (0, 0), mask=msk)
            img.show()


def link():
    uri = URI("coco-link/version/latest", expected_type=URIType.DATASET)
    for idx, data, annotations in get_data_loader(uri, 0, 1):
        with PILImage.open(io.BytesIO(data.fp)) as img, PILImage.open(
            io.BytesIO(
                annotations["mask"].to_bytes(uri)
            )
        ).convert("RGBA") as msk:
            for seg in annotations["segments_info"]:
                draw_bbox(img, seg.bbox_view)

            msk.putalpha(127)
            img.paste(msk, (0, 0), mask=msk)
            img.show()


if __name__ == "__main__":
    link()
