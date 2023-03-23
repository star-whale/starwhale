import requests

from starwhale import Link, Image, Point, dataset, Polygon, MIMEType  # noqa: F401
from starwhale.utils.retry import http_retry

PATH_ROOT = "https://starwhale-examples.oss-cn-beijing.aliyuncs.com/dataset/cityscapes"
ANNO_PATH = "disparity/train"
DATA_PATH_LEFT = "leftImg8bit/train"
DATA_PATH_RIGHT = "rightImg8bit/train"
SUFFIX_MASK = "_disparity.png"
SUFFIX_DATA_LEFT = "_leftImg8bit.png"
SUFFIX_DATA_RIGHT = "_rightImg8bit.png"


@http_retry
def request_link_json(anno_link):
    return requests.get(anno_link, timeout=10).json()


def mask_image(_name, dir_name):
    return Image(
        display_name=_name,
        mime_type=MIMEType.PNG,
        as_mask=True,
        link=Link(uri=f"{PATH_ROOT}/{ANNO_PATH}/{dir_name}/{_name}"),
    )


def build_ds():
    ds = dataset("cityscapes_disparity")
    ds.info["baseline"] = 22
    ds.info["homepage"] = "https://www.cityscapes-dataset.com"
    tree = request_link_json(f"{PATH_ROOT}/{ANNO_PATH}/tree.json")
    for d in tree:
        if d["type"] != "directory":
            continue
        dir_name = d["name"]
        for f in d["contents"]:
            if f["type"] != "file":
                continue
            _name = str(f["name"])
            if not _name.endswith(SUFFIX_MASK):
                continue
            disparity_mask = mask_image(_name, dir_name)
            name = _name.replace(SUFFIX_MASK, "")
            right_image = Image(
                display_name=name,
                link=Link(
                    uri=f"{PATH_ROOT}/{DATA_PATH_RIGHT}/{dir_name}/{name}{SUFFIX_DATA_RIGHT}"
                ),
                mime_type=MIMEType.JPEG,
            )
            left_image = Image(
                display_name=name,
                link=Link(
                    uri=f"{PATH_ROOT}/{DATA_PATH_LEFT}/{dir_name}/{name}{SUFFIX_DATA_LEFT}"
                ),
                mime_type=MIMEType.JPEG,
            )
            ds.append(
                {
                    "left_image_8bit": left_image,
                    "right_image_8bit": right_image,
                    "disparity_mask": disparity_mask,
                }
            )

    ds.commit()
    load_ds = dataset(ds.uri)
    print(load_ds.info)
    ds.close()


if __name__ == "__main__":
    build_ds()
