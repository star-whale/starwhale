import requests

from starwhale import Link, Image, dataset, MIMEType  # noqa: F401
from starwhale.utils.retry import http_retry

PATH_ROOT = "https://starwhale-examples.oss-cn-beijing.aliyuncs.com/dataset/lfw"
INDEX_PATH = "http://vis-www.cs.umass.edu/lfw/peopleDevTrain.txt"


@http_retry
def request_link_text(anno_link):
    return requests.get(anno_link, timeout=10).text


def build_ds():
    ds = dataset("lfw")
    lines = request_link_text(INDEX_PATH).splitlines()
    for line in lines[1:]:
        name, pic = line.split()
        f_name = f"{name}_{pic.zfill(4)}.jpg"
        ds.append(
            (
                f_name,
                {
                    "image": Image(
                        link=Link(
                            uri=f"{PATH_ROOT}/{name}/{f_name}",
                        ),
                        display_name=f_name,
                        mime_type=MIMEType.JPEG,
                    ),
                    "identity": name,
                },
            )
        )

    ds.commit()
    ds.close()


if __name__ == "__main__":
    build_ds()
