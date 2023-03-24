import requests

from starwhale import Link, Text, Image, Binary, dataset  # noqa: F401
from starwhale.utils.retry import http_retry

PATH_ROOT = "https://starwhale-examples.oss-cn-beijing.aliyuncs.com/dataset/kitti/lane/data_road"


@http_retry
def request_link_json(anno_link):
    return requests.get(anno_link, timeout=10).json()


def build_ds():
    ds = dataset("kitti-lane")
    index_json = request_link_json(f"{PATH_ROOT}/tree.json")
    for item in index_json:
        if "training" != item.get("name"):
            continue
        for directory in item.get("contents"):
            if directory.get("name") != "image_2":
                continue
            for file in directory.get("contents"):
                f_name = str(file.get("name"))
                calib_name = f_name.replace(".png", ".txt")
                name_parts = f_name.split("_")
                name_head = name_parts[0]
                data = {
                    "label_text": Text(
                        link=Link(f"{PATH_ROOT}/training/calib/{calib_name}")
                    ),
                    "label_road_pic": Image(
                        link=Link(
                            f"{PATH_ROOT}/training/gt_image_2/{name_head}_road_{name_parts[1]}"
                        ),
                        as_mask=True,
                    ),
                    "image": Image(link=Link(f"{PATH_ROOT}/training/image_2/{f_name}")),
                }
                if name_head == "um":
                    # add lane pic
                    data.update(
                        {
                            "label_lane_pic": Image(
                                link=Link(
                                    f"{PATH_ROOT}/training/gt_image_2/{name_head}_lane_{name_parts[1]}"
                                ),
                                as_mask=True,
                            )
                        }
                    )
                ds.append(data)

    ds.commit()
    ds.close()


if __name__ == "__main__":
    build_ds()
