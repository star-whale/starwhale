import requests

from starwhale import Link, Image, dataset, MIMEType, BoundingBox  # noqa: F401
from starwhale.utils.retry import http_retry

PATH_ROOT = "https://starwhale-examples.oss-cn-beijing.aliyuncs.com/dataset/GTSRB/Final_Training/Images"


@http_retry
def request_link_text(anno_link):
    return requests.get(anno_link, timeout=10).text


def build_ds():
    ds = dataset("gtsrb")
    for i in range(40):
        dir = str(i).zfill(5)
        lines = request_link_text(f"{PATH_ROOT}/{dir}/GT-{dir}.csv").splitlines()
        for line in lines[1:]:
            file_name, w, h, xmin, ymin, xmax, ymax, clzz = line.split(";")
            ds.append(
                (
                    f"{dir}/{file_name}",
                    {
                        "image": Image(
                            link=Link(
                                uri=f"{PATH_ROOT}/{dir}/{file_name}",
                            ),
                            display_name=file_name,
                            mime_type=MIMEType.PPM,
                            shape=(int(w), int(h)),
                        ),
                        "class": clzz,
                        "bbox": BoundingBox(
                            int(xmin),
                            int(ymin),
                            int(xmax) - int(xmin),
                            int(ymax) - int(ymin),
                        ),
                    },
                )
            )

    ds.commit()
    ds.close()


if __name__ == "__main__":
    build_ds()
