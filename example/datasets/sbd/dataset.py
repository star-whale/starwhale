import io

import requests
import scipy.io

from starwhale import Link, Image, Binary, dataset, MIMEType  # noqa: F401
from starwhale.utils.retry import http_retry

PATH_ROOT = (
    "https://starwhale-examples.oss-cn-beijing.aliyuncs.com/dataset/Semantic_Boundaries"
)
DATA_PATH = "img"
CLS_PATH = "cls"
TRAIN_INDEX = "train.txt"


@http_retry
def request_link_text(anno_link):
    return requests.get(anno_link, timeout=10).text


def build_ds():
    ds = dataset("sbd")
    items = request_link_text(f"{PATH_ROOT}/{TRAIN_INDEX}").splitlines()
    for item in items:
        with requests.get(f"{PATH_ROOT}/{CLS_PATH}/{item}.mat", timeout=10) as rsp:
            mat = scipy.io.loadmat(io.BytesIO(rsp.content))
            cls = mat["GTcls"]
            boundaries = cls[0][0][1]
            ds.append(
                (
                    item,
                    {
                        "image": Image(
                            link=Link(uri=f"{PATH_ROOT}/{DATA_PATH}/{item}.jpg"),
                            display_name=item,
                            mime_type=MIMEType.JPEG,
                        ),
                        "boundaries": Binary(boundaries.tobytes()),
                        "shape": boundaries.shape,
                    },
                )
            )

    ds.commit()
    ds.close()


if __name__ == "__main__":
    build_ds()
