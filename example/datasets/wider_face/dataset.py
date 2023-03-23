import requests

from starwhale import Link, Image, dataset, MIMEType, BoundingBox  # noqa: F401
from starwhale.utils.retry import http_retry

PATH_ROOT = "https://starwhale-examples.oss-cn-beijing.aliyuncs.com/dataset/wider_face"
INDEX_PATH = "wider_face_split/wider_face_train_bbx_gt.txt"
DATA_PATH = "WIDER_train/images"


@http_retry
def request_link_text(anno_link):
    return requests.get(anno_link, timeout=10).text


def build_ds():
    ds = dataset("wider_face")
    lines = request_link_text(f"{PATH_ROOT}/{INDEX_PATH}").splitlines()
    current_img = ""
    image_annos = []
    for line in lines:
        if line.endswith(".jpg"):
            if current_img:
                ds.append(
                    (
                        current_img,
                        {
                            "image": Image(
                                link=Link(uri=f"{PATH_ROOT}/{DATA_PATH}/{current_img}"),
                                display_name=current_img,
                                mime_type=MIMEType.JPEG,
                            ),
                            "faces": image_annos,
                        },
                    )
                )
            current_img = line
            image_annos = []
            continue
        split = line.split()
        if len(split) != 10:
            continue
        image_annos.append(
            {
                "bbox": BoundingBox(
                    int(split[0]), int(split[1]), int(split[2]), int(split[3])
                ),
                "blur": split[4],
                "expression": split[5],
                "illumination": split[6],
                "occlusion": split[7],
                "pose": split[8],
                "invalid": split[9],
            }
        )
    ds.commit()
    ds.close()


if __name__ == "__main__":
    build_ds()
