import requests
import xmltodict

from starwhale import Link, Image, BoundingBox  # noqa: F401
from starwhale.utils.retry import http_retry


def to_box_view(bndbox):
    xmin = int(bndbox["xmin"])
    ymin = int(bndbox["ymin"])
    xmax = int(bndbox["xmax"])
    ymax = int(bndbox["ymax"])
    return BoundingBox(xmin, ymin, xmax - xmin, ymax - ymin)


@http_retry
def request_link_text(anno_link):
    return requests.get(anno_link, timeout=10).text


def do_iter_item():
    for line in request_link_text(
        "https://starwhale-examples.oss-cn-beijing.aliyuncs.com/dataset/image-net/ILSVRC/ImageSets/CLS-LOC/val.txt"
    ).splitlines():
        img_name = line.split()[0]
        anno_link = f"https://starwhale-examples.oss-cn-beijing.aliyuncs.com/dataset/image-net/ILSVRC/Annotations/CLS-LOC/val/{img_name}.xml"
        xml = request_link_text(anno_link)
        anno = xmltodict.parse(xml)
        obj = anno["annotation"]["object"]
        if isinstance(obj, dict):
            obj = [obj]
        for _obj in obj:
            _obj["bbox_view"] = to_box_view(_obj["bndbox"])
        anno["annotation"]["object"] = obj
        size_ = anno["annotation"]["size"]
        anno["annotation"]["image"] = Image(
            link=Link(
                uri=f"https://starwhale-examples.oss-cn-beijing.aliyuncs.com/dataset/image-net/ILSVRC/Data/CLS-LOC/val/{img_name}.JPEG",
            ),
            display_name=img_name,
            shape=(size_["depth"], size_["height"], size_["width"]),
        )
        yield anno["annotation"]


if __name__ == "__main__":
    do_iter_item()
