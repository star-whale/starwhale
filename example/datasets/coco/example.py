import io
import os

from PIL import Image as PILImage
from PIL import ImageDraw
from urllib.parse import urlparse
from starwhale import URI, URIType, get_data_loader
from starwhale.core.dataset.store import S3Connection, S3StorageBackend


def draw_bbox(img, bbox_view_):
    bbox1 = ImageDraw.Draw(img)
    bbox1.rectangle([(bbox_view_["x"], bbox_view_["y"]),
                     (bbox_view_["x"] + bbox_view_["width"], bbox_view_["y"] + bbox_view_["height"])],
                    fill=None,
                    outline="red")


def raw():
    uri = URI("coco-raw/version/latest", expected_type=URIType.DATASET)
    for idx, data, annotations in get_data_loader(uri, 0, 1):
        with PILImage.open(io.BytesIO(data.fp)) as img, \
            PILImage.open(annotations["mask"]["uri"]).convert("RGBA") as msk:
            for seg in annotations["segments_info"]:
                draw_bbox(img, seg["bbox_view"])

            msk.putalpha(127)
            img.paste(msk, (0, 0), mask=msk)
            img.show()


_ak = os.environ.get("SW_S3_AK", "starwhale")
_sk = os.environ.get("SW_S3_SK", "starwhale")
_endpoint = os.environ.get("SW_S3_EDP", "http://10.131.0.1:9000")
_region = os.environ.get("SW_S3_REGION", "local")
_bucket = "users"


def link():
    s3 = S3StorageBackend(S3Connection(_endpoint, _ak, _sk, _region, _bucket))
    uri = URI("coco-link/version/latest", expected_type=URIType.DATASET)
    for idx, data, annotations in get_data_loader(uri, 0, 1):
        with PILImage.open(io.BytesIO(data.fp)) as img, \
            PILImage.open(
                io.BytesIO(s3._make_file(_bucket, urlparse(annotations["mask"]["uri"]).path).read(-1))).convert(
                "RGBA") as msk:
            for seg in annotations["segments_info"]:
                draw_bbox(img, seg["bbox_view"])

            msk.putalpha(127)
            img.paste(msk, (0, 0), mask=msk)
            img.show()


if __name__ == "__main__":
    raw()
