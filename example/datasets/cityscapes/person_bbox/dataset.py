import requests

from starwhale import Link, Image, dataset, MIMEType, BoundingBox  # noqa: F401
from starwhale.utils.retry import http_retry

PATH_ROOT = "https://starwhale-examples.oss-cn-beijing.aliyuncs.com/dataset/cityscapes"
ANNO_PATH = "gtBboxCityPersons/train"
DATA_PATH = "leftImg8bit/train"
SUFFIX_ANNO = "_gtBboxCityPersons.json"
SUFFIX_DATA = "_leftImg8bit.png"


def to_box_view(_array):
    return BoundingBox(_array[0], _array[1], _array[2], _array[3])


@http_retry
def request_link_json(anno_link):
    return requests.get(anno_link, timeout=10).json()


def build_ds():
    ds = dataset("city_person")
    tree = request_link_json(f"{PATH_ROOT}/{ANNO_PATH}/tree.json")
    for d in tree:
        if d["type"] != "directory":
            continue
        category = d["name"]
        for f in d["contents"]:
            _name = str(f["name"])
            data = request_link_json(f"{PATH_ROOT}/{ANNO_PATH}/{category}/{_name}")
            if not data["objects"]:
                print(data["objects"])
                continue
            for obj in data["objects"]:
                obj["bbox"] = to_box_view(obj["bbox"])
                obj["bboxVis"] = to_box_view(obj["bboxVis"])
            d_name = _name.replace(SUFFIX_ANNO, SUFFIX_DATA)
            data["image"] = Image(
                display_name=d_name,
                link=Link(
                    uri=f"{PATH_ROOT}/{DATA_PATH}/{category}/{d_name}",
                ),
                mime_type=MIMEType.JPEG,
                shape=(data["imgHeight"], data["imgWidth"]),
            )
            ds.append(
                (
                    f"{category}/{d_name}",
                    data,
                )
            )

    ds.commit()
    ds.close()


if __name__ == "__main__":
    build_ds()
