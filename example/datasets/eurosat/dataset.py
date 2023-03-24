import json

import requests

from starwhale import Link, Image, dataset, MIMEType  # noqa: F401
from starwhale.utils.retry import http_retry

PATH_ROOT = "https://starwhale-examples.oss-cn-beijing.aliyuncs.com/dataset/eurosat"


@http_retry
def request_link_text(anno_link):
    return requests.get(anno_link, timeout=10).text


def build_ds():
    ds = dataset("eurosat")
    tree = json.loads(request_link_text(f"{PATH_ROOT}/tree.json"))
    for dir in tree:
        if "directory" != dir.get("type", ""):
            continue
        category = dir["name"]
        for f in dir["contents"]:
            if f["type"] != "file":
                continue
            _name = f["name"]
            ds.append(
                (
                    f"{category}/{_name}",
                    {
                        "image": Image(
                            display_name=_name,
                            mime_type=MIMEType.JPEG,
                            link=Link(uri=f"{PATH_ROOT}/{category}/{_name}"),
                        ),
                        "label": category,
                    },
                )
            )

    ds.commit()
    ds.close()


if __name__ == "__main__":
    build_ds()
