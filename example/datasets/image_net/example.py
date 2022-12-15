import io
from pprint import pprint

from PIL import Image as PILImage
from PIL import ImageDraw

from starwhale import URI, URIType, get_data_loader


def draw_bbox(img, bbox_view_):
    bbox1 = ImageDraw.Draw(img)
    x, y, w, h = bbox_view_.x, bbox_view_.y, bbox_view_.width, bbox_view_.height
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


def show():
    uri = URI("image-net/version/latest", expected_type=URIType.DATASET)
    for idx, data, annotations in get_data_loader(uri, 0, 1):
        pprint(annotations)
        with PILImage.open(io.BytesIO(data.fp)) as img:
            for obj in annotations["annotation"]["object"]:
                draw_bbox(img, obj["bbox_view"])
            img.show()


if __name__ == "__main__":
    show()
