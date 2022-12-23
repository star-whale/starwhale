import io

import requests
import scipy.io

from starwhale import Link, Image, dataset, MIMEType, BoundingBox  # noqa: F401
from starwhale.utils.retry import http_retry

PATH_ROOT = (
    "https://starwhale-examples.oss-cn-beijing.aliyuncs.com/dataset/stanford_cars"
)
MAT_FILE = "cars_annos.mat"


@http_retry
def request_link_text(anno_link):
    return requests.get(anno_link, timeout=10).text


def build_ds():
    ds = dataset("stanford_cars", create=True)
    with requests.get(f"{PATH_ROOT}/{MAT_FILE}", timeout=10) as rsp:
        mat = scipy.io.loadmat(io.BytesIO(rsp.content))
        for anno in mat["annotations"][0]:
            relative_im_path = anno[0][0].item()
            bbox_x1 = anno[1][0][0].item()
            bbox_y1 = anno[2][0][0].item()
            bbox_x2 = anno[3][0][0].item()
            bbox_y2 = anno[4][0][0].item()
            clzz = anno[5][0][0].item()
            test = anno[6][0][0].item()
            ds.append(
                (
                    relative_im_path,
                    Link(
                        uri=f"{PATH_ROOT}/{relative_im_path}",
                        data_type=Image(
                            display_name=relative_im_path, mime_type=MIMEType.JPEG
                        ),
                        with_local_fs_data=False,
                    ),
                    {
                        "label": clzz,
                        "bbox": BoundingBox(
                            bbox_x1, bbox_y1, bbox_x2 - bbox_x1, bbox_y2 - bbox_y1
                        ),
                        "test": test,
                    },
                )
            )

    ds.commit()
    ds.close()


if __name__ == "__main__":
    build_ds()
