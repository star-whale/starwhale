import requests

from starwhale import (  # noqa: F401
    Link,
    Image,
    dataset,
    MIMEType,
    BoundingBox,
    BoundingBox3D,
)
from starwhale.utils.retry import http_retry

PATH_ROOT = "https://starwhale-examples.oss-cn-beijing.aliyuncs.com/dataset/cityscapes"
ANNO_PATH = "gtBbox3d/train"
DATA_PATH_LEFT = "leftImg8bit/train"
DATA_PATH_RIGHT = "rightImg8bit/train"
SUFFIX_ANNO = "_gtBbox3d.json"
SUFFIX_DATA_LEFT = "_leftImg8bit.png"
SUFFIX_DATA_RIGHT = "_rightImg8bit.png"


def to_bbox_view(_array):
    return BoundingBox(_array[0], _array[1], _array[2], _array[3])


def to_bbox_view_3d(_box):
    return BoundingBox3D(to_bbox_view(_box["modal"]), to_bbox_view(_box["amodal"]))


@http_retry
def request_link_json(anno_link):
    return requests.get(anno_link, timeout=10).json()


def build_ds():
    ds = dataset("cityscapes_3dcarbox")
    tree = request_link_json(f"{PATH_ROOT}/{ANNO_PATH}/tree.json")
    for d in tree:
        if d["type"] != "directory":
            continue
        dir_name = d["name"]
        for f in d["contents"]:
            if f["type"] != "file":
                continue
            _name = str(f["name"])
            anno = request_link_json(f"{PATH_ROOT}/{ANNO_PATH}/{dir_name}/{_name}")
            for obj in anno["objects"]:
                obj["2d_sw"] = to_bbox_view_3d(obj["2d"])
            name = _name.replace(SUFFIX_ANNO, "")
            anno["right_image_8bit"] = Image(
                display_name=name,
                link=Link(
                    uri=f"{PATH_ROOT}/{DATA_PATH_RIGHT}/{dir_name}/{name}{SUFFIX_DATA_RIGHT}"
                ),
                mime_type=MIMEType.JPEG,
                shape=(anno["imgHeight"], anno["imgWidth"]),
            )
            anno["left_image_8bit"] = Image(
                display_name=name,
                link=Link(
                    uri=f"{PATH_ROOT}/{DATA_PATH_LEFT}/{dir_name}/{name}{SUFFIX_DATA_LEFT}"
                ),
                mime_type=MIMEType.JPEG,
                shape=(anno["imgHeight"], anno["imgWidth"]),
            )

        ds.append(
            (
                name,
                anno,
            )
        )

    ds.commit()
    ds.close()


if __name__ == "__main__":
    build_ds()
