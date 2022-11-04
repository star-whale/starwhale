import os

import boto3
from botocore.client import Config as S3Config
import json
from pathlib import Path

from starwhale import Link, Image, BoundingBox, MIMEType, S3LinkAuth
from starwhale.core.dataset.store import S3StorageBackend, S3Connection

ROOT_DIR = Path(__file__).parent
DATA_DIR = ROOT_DIR / "data"


def images2dict(imgs):
    _dict = {}
    for img in imgs:
        _dict[img["id"]] = img
    return _dict


def do_iter_item():
    with (DATA_DIR / "annotations" / "panoptic_val2017.json").open("r") as f:
        index = json.load(f)
        img_dict = images2dict(index["images"])
        for anno in index["annotations"]:
            img_meta = img_dict[anno["image_id"]]
            img_name = img_meta["file_name"]
            img_pth = DATA_DIR / "val2017" / img_name
            img_shape = (img_meta["height"], img_meta["width"])
            msk_f_name = anno["file_name"]
            msk_f_pth = DATA_DIR / "annotations" / "panoptic_val2017" / msk_f_name
            segs_info = anno["segments_info"]
            for sg in segs_info:
                x, y, w, h = sg["bbox"]
                sg["bbox_view"] = BoundingBox(
                    x=x, y=y, width=w, height=h
                )

            anno["mask"] = Link(
                auth=None,
                with_local_fs_data=True,
                data_type=Image(display_name=msk_f_name, shape=img_shape, mime_type=MIMEType.PNG),
                uri=str(msk_f_pth.absolute()),
            )
            yield Link(
                uri=str(img_pth.absolute()),
                data_type=Image(display_name=img_name, shape=img_shape),
                with_local_fs_data=True,
            ), anno


PATH_ROOT = "dataset/coco/extracted"
_ak = os.environ.get("SW_S3_AK", "starwhale")
_sk = os.environ.get("SW_S3_SK", "starwhale")
_endpoint = os.environ.get("SW_S3_EDP", "http://10.131.0.1:9000")
_region = os.environ.get("SW_S3_REGION", "local")
_auth = S3LinkAuth(name="SW_S3", access_key=_ak, secret=_sk, endpoint=_endpoint, region=_region)
_bucket = "users"
RUI_ROOT = f"{_bucket}/{PATH_ROOT}"


def do_iter_item_from_remote():
    s3 = boto3.resource(
        "s3",
        endpoint_url=_endpoint,
        aws_access_key_id=_ak,
        aws_secret_access_key=_sk,
        config=S3Config(
            s3={},
            connect_timeout=6000,
            read_timeout=6000,
            signature_version="s3v4",
            retries={
                "total_max_attempts": 1,
                "mode": "standard",
            },
        ),
        region_name=_region,
    )

    index = json.loads(
        s3.Object(_bucket, f"{PATH_ROOT}/annotations/panoptic_val2017.json").get()["Body"].read().decode('utf8'))
    img_dict = images2dict(index["images"])
    for anno in index["annotations"]:
        img_meta = img_dict[anno["image_id"]]
        img_name = img_meta["file_name"]
        img_shape = (img_meta["height"], img_meta["width"])
        msk_f_name = anno["file_name"]
        segs_info = anno["segments_info"]
        for sg in segs_info:
            x, y, w, h = sg["bbox"]
            sg["bbox_view"] = BoundingBox(
                x=x, y=y, width=w, height=h
            )

        anno["mask"] = Link(
            auth=None,
            with_local_fs_data=False,
            data_type=Image(display_name=msk_f_name, shape=img_shape, mime_type=MIMEType.PNG),
            uri=f"s3://{RUI_ROOT}/annotations/panoptic_val2017/{msk_f_name}",
        )
        yield Link(
            auth=_auth,
            uri=f"s3://{RUI_ROOT}/val2017/{img_name}",
            data_type=Image(display_name=img_name, shape=img_shape),
            with_local_fs_data=False,
        ), anno
