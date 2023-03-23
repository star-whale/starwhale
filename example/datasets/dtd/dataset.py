import requests

from starwhale import Link, Image, dataset, MIMEType  # noqa: F401
from starwhale.utils.retry import http_retry

PATH_ROOT = "https://starwhale-examples.oss-cn-beijing.aliyuncs.com/dataset/dtd"
DATA_PATH = "images"
LABEL_PATH = "labels/labels_joint_anno.txt"


@http_retry
def request_link_text(anno_link):
    return requests.get(anno_link, timeout=10).text


def build_ds():
    ds = dataset("dtd")
    lines = request_link_text(f"{PATH_ROOT}/{LABEL_PATH}").splitlines()
    for line in lines:
        tokens = line.split()
        img_pth = tokens[0]
        labels = tokens[1:]
        ds.append(
            (
                img_pth,
                {
                    "image": Image(
                        display_name=img_pth,
                        mime_type=MIMEType.JPEG,
                        link=Link(
                            uri=f"{PATH_ROOT}/{DATA_PATH}/{img_pth}",
                        ),
                    ),
                    "labels": labels,
                },
            )
        )
    ds.commit()
    ds.close()


if __name__ == "__main__":
    build_ds()
