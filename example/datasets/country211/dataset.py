import json

import requests

from starwhale import Link, Image, dataset, MIMEType  # noqa: F401
from starwhale.utils.retry import http_retry

PATH_ROOT = "https://starwhale-examples.oss-cn-beijing.aliyuncs.com/dataset/country211"
DATA_PATH = "."


@http_retry
def request_link_text(anno_link):
    return requests.get(anno_link, timeout=10).text


def build_ds(split: str = "test"):
    ds = dataset("country211")
    tree = json.loads(request_link_text(f"{PATH_ROOT}/tree.json"))
    for dir in tree:
        if DATA_PATH not in dir.get("name", ""):
            continue
        for d in dir["contents"]:
            if d["type"] != "directory":
                continue
            if d["name"] != split:
                continue
            for _d in d["contents"]:
                category = _d["name"]
                for f in _d["contents"]:
                    _name = f["name"]
                    ds.append(
                        (
                            f"{category}/{_name}",
                            {
                                "image": Image(
                                    display_name=_name,
                                    mime_type=MIMEType.JPEG,
                                    link=Link(
                                        uri=f"{PATH_ROOT}/{split}/{category}/{_name}",
                                    ),
                                ),
                                "label": category,
                            },
                        )
                    )
    ds.commit()
    ds.close()


if __name__ == "__main__":
    build_ds("test")
