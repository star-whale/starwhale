import requests
import xmltodict

from starwhale import Link, Image, dataset, MIMEType, BoundingBox  # noqa: F401
from starwhale.utils.retry import http_retry

PATH_ROOT = (
    "https://starwhale-examples.oss-cn-beijing.aliyuncs.com/dataset/oxford-ped-iiit"
)
DATA_PATH = "images"
ANNO_PATH = "annotations"
INDEX_PATH = f"{ANNO_PATH}/list.txt"
XML_PATH = f"{ANNO_PATH}/xmls"
MASK_PATH = f"{ANNO_PATH}/trimaps"


def to_box_view(bndbox):
    xmin = int(bndbox["xmin"])
    ymin = int(bndbox["ymin"])
    xmax = int(bndbox["xmax"])
    ymax = int(bndbox["ymax"])
    return BoundingBox(xmin, ymin, xmax - xmin, ymax - ymin)


@http_retry
def request_link_text(anno_link):
    return requests.get(anno_link, timeout=10).text


def build_ds():
    ds = dataset("oxfordIIITPet")
    lines = request_link_text(f"{PATH_ROOT}/{INDEX_PATH}").splitlines()
    for line in lines[6:]:
        f_name, class_id, species, breed_id = line.split()
        xml = xmltodict.parse(request_link_text(f"{PATH_ROOT}/{XML_PATH}/{f_name}.xml"))
        if "annotation" not in xml:
            continue
        img_shape = (
            int(xml["annotation"]["size"]["depth"]),
            int(xml["annotation"]["size"]["height"]),
            int(xml["annotation"]["size"]["width"]),
        )

        object_ = xml["annotation"]["object"]
        if isinstance(object_, dict):
            object_ = [object_]
        pets = []
        for obj in object_:
            pets.append(
                {
                    "name": obj["name"],
                    "pose": obj["pose"],
                    "truncated": obj["truncated"],
                    "occluded": obj["occluded"],
                    "difficult": obj["difficult"],
                    "bbox": to_box_view(obj["bndbox"]),
                }
            )
        data = {
            "image": Image(
                link=Link(uri=f"{PATH_ROOT}/{DATA_PATH}/{f_name}.jpg"),
                display_name=f_name,
                shape=img_shape,
                mime_type=MIMEType.JPEG,
            ),
            "mask": Image(
                link=Link(
                    uri=f"{PATH_ROOT}/{MASK_PATH}/{f_name}.png",
                ),
                display_name=f_name,
                shape=img_shape,
                mime_type=MIMEType.PNG,
                as_mask=True,
            ),
            "segmented": xml["annotation"]["segmented"],
            "pets": pets,
            "class_id": class_id,
            "species": species,
            "breed_id": breed_id,
        }
        ds.append(
            (
                f_name,
                data,
            )
        )

    ds.commit()
    ds.close()


if __name__ == "__main__":
    build_ds()
