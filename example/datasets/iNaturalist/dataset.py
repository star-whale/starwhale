import requests

from starwhale import Link, Image, dataset, MIMEType  # noqa: F401
from starwhale.utils.retry import http_retry

PATH_ROOT = "https://starwhale-examples.oss-cn-beijing.aliyuncs.com/dataset/iNaturalist"
INDEX_PATH = "val.json"


def array_to_dict(arry):
    _d = {}
    for i in arry:
        _d[i["id"]] = i
    return _d


@http_retry
def request_link_json(anno_link):
    return requests.get(anno_link, timeout=10).json()


def build_ds():
    ds = dataset("iNaturalist-val")
    index = request_link_json(f"{PATH_ROOT}/{INDEX_PATH}")
    img_dict = array_to_dict(index["images"])
    cat_dict = array_to_dict(index["categories"])
    for anno in index["annotations"]:
        img = img_dict[anno["image_id"]]
        cat = cat_dict[anno["category_id"]]
        if img.get("latitude"):
            cat["latitude"] = img.get("latitude")
        if img.get("longitude"):
            cat["longitude"] = img.get("longitude")
        if img.get("date"):
            cat["date"] = img.get("date")
        if cat.get("id"):
            cat.pop("id")
        img_file_name_ = img["file_name"]
        cat["image"] = Image(
            link=Link(uri=f"{PATH_ROOT}/{img_file_name_}"),
            display_name=anno["image_id"],
            mime_type=MIMEType.JPEG,
            shape=(img["height"], img["width"]),
        )
        ds.append(
            (
                anno["image_id"],
                cat,
            )
        )

    ds.commit()
    ds.close()


if __name__ == "__main__":
    build_ds()
