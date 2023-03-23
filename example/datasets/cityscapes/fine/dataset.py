import requests

from starwhale import Link, Image, Point, dataset, Polygon, MIMEType  # noqa: F401
from starwhale.utils.retry import http_retry

PATH_ROOT = "https://starwhale-examples.oss-cn-beijing.aliyuncs.com/dataset/cityscapes"
ANNO_PATH = "gtFine/train"
DATA_PATH = "leftImg8bit/train"
SUFFIX_COLOR_MASK = "_gtFine_color.png"
SUFFIX_INSTANCE_ID_MASK = "_gtFine_instanceIds.png"
SUFFIX_LABEL_ID_MASK = "_gtFine_labelIds.png"
SUFFIX_POLYGON = "_gtFine_polygons.json"
SUFFIX_DATA = "_leftImg8bit.png"


def to_polygon_view(_array):
    points = [Point(p_array[0], p_array[1]) for p_array in _array]
    return Polygon(points)


@http_retry
def request_link_json(anno_link):
    return requests.get(anno_link, timeout=10).json()


def mask_image(_name, dir_name):
    return Image(
        display_name=_name,
        mime_type=MIMEType.PNG,
        as_mask=True,
        link=Link(uri=f"{PATH_ROOT}/{ANNO_PATH}/{dir_name}/{_name}"),
    )


def build_ds():
    ds = dataset("cityscapes_fine")
    tree = request_link_json(f"{PATH_ROOT}/{ANNO_PATH}/tree.json")
    items = {}
    for d in tree:
        if d["type"] != "directory":
            continue
        dir_name = d["name"]
        for f in d["contents"]:
            if f["type"] != "file":
                continue
            _name = str(f["name"])
            if _name.endswith(SUFFIX_COLOR_MASK):
                item_id = _name.replace(SUFFIX_COLOR_MASK, "")
                if item_id not in items:
                    items[item_id] = {"dir": dir_name}
                items[item_id]["color_mask"] = mask_image(_name, dir_name)
            elif _name.endswith(SUFFIX_INSTANCE_ID_MASK):
                item_id = _name.replace(SUFFIX_INSTANCE_ID_MASK, "")
                if item_id not in items:
                    items[item_id] = {"dir": dir_name}
                items[item_id]["instance_mask"] = mask_image(_name, dir_name)
            elif _name.endswith(SUFFIX_LABEL_ID_MASK):
                item_id = _name.replace(SUFFIX_LABEL_ID_MASK, "")
                if item_id not in items:
                    items[item_id] = {"dir": dir_name}
                items[item_id]["label_mask"] = mask_image(_name, dir_name)
            elif _name.endswith(SUFFIX_POLYGON):
                item_id = _name.replace(SUFFIX_POLYGON, "")
                if item_id not in items:
                    items[item_id] = {"dir": dir_name}
                polygon_anno = request_link_json(
                    f"{PATH_ROOT}/{ANNO_PATH}/{dir_name}/{_name}"
                )
                objs = polygon_anno.get("objects")
                if not objs:
                    continue
                for obj in objs:
                    obj["polygon"] = to_polygon_view(obj["polygon"])
                items[item_id]["polygons"] = polygon_anno

    for name, data in items.items():
        _dir = data["dir"]
        data["image"] = Image(
            display_name=name,
            link=Link(uri=f"{PATH_ROOT}/{DATA_PATH}/{_dir}/{name}{SUFFIX_DATA}"),
            mime_type=MIMEType.JPEG,
            shape=(
                data["polygons"]["imgHeight"],
                data["polygons"]["imgWidth"],
            ),
        )
        ds.append(
            (
                name,
                data,
            )
        )

    ds.commit()
    ds.close()


if __name__ == "__main__":
    build_ds()
