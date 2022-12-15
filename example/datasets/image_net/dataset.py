import xmltodict

from starwhale import Link, Image, BoundingBox  # noqa: F401


def to_box_view(bndbox):
    xmin = int(bndbox["xmin"])
    ymin = int(bndbox["ymin"])
    xmax = int(bndbox["xmax"])
    ymax = int(bndbox["ymax"])
    return BoundingBox(xmin, ymin, xmax - xmin, ymax - ymin)


def do_iter_item():
    import requests

    response = requests.get(
        "https://starwhale-examples.oss-cn-beijing.aliyuncs.com/dataset/image-net/ILSVRC/ImageSets/CLS-LOC/val.txt"
    )
    for line in response.text.splitlines():
        img_name = line.split()[0]
        anno_link = f"https://starwhale-examples.oss-cn-beijing.aliyuncs.com/dataset/image-net/ILSVRC/Annotations/CLS-LOC/val/{img_name}.xml"
        while True:
            try:
                xml = requests.get(anno_link).text
                anno = xmltodict.parse(xml)
                obj = anno["annotation"]["object"]
                if isinstance(obj, dict):
                    obj = [obj]
                for _obj in obj:
                    _obj["bbox_view"] = to_box_view(_obj["bndbox"])
                anno["annotation"]["object"] = obj
                size_ = anno["annotation"]["size"]
                yield Link(
                    uri=f"https://starwhale-examples.oss-cn-beijing.aliyuncs.com/dataset/image-net/ILSVRC/Data/CLS-LOC/val/{img_name}.JPEG",
                    data_type=Image(
                        display_name=img_name,
                        shape=(size_["depth"], size_["height"], size_["width"]),
                    ),
                    with_local_fs_data=False,
                ), anno
                break
            except Exception:
                print(f"timeout {line}")
                continue


if __name__ == "__main__":
    do_iter_item()
