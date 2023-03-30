import json

import requests

from starwhale import Link, Image, dataset, MIMEType  # noqa: F401
from starwhale.utils.retry import http_retry

PATH_ROOT = "https://starwhale-examples.oss-cn-beijing.aliyuncs.com/dataset/caltech-101"
DATA_PATH = "101_ObjectCategories"


@http_retry
def request_link_text(anno_link):
    return requests.get(anno_link, timeout=10).text


def build_ds():
    ds = dataset("caltech-101")
    tree = json.loads(request_link_text(f"{PATH_ROOT}/tree.json"))
    for dir in tree:
        if DATA_PATH not in dir.get("name", ""):
            continue
        for d in dir["contents"]:
            if d["type"] != "directory":
                continue
            category = d["name"]
            for f in d["contents"]:
                _name = f["name"]
                ds.append(
                    (
                        f"{category}/{_name}",
                        {
                            "label": category,
                            "image": Image(
                                display_name=_name,
                                mime_type=MIMEType.JPEG,
                                link=Link(
                                    uri=f"{PATH_ROOT}/{DATA_PATH}/{category}/{_name}",
                                ),
                            ),
                        },
                    )
                )
    ds.commit()
    ds.close()


if __name__ == "__main__":
    build_ds()
