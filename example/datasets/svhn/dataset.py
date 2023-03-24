import io

import h5py
import h5py.h5r
import requests

from starwhale import Link, Image, dataset, MIMEType, BoundingBox  # noqa: F401

PATH_ROOT = "https://starwhale-examples.oss-cn-beijing.aliyuncs.com/dataset/svhn/train"
INDEX_FILE = "digitStruct.mat"


def build_ds():
    ds = dataset("svhn")
    with requests.get(f"{PATH_ROOT}/{INDEX_FILE}", timeout=10) as rsp:
        f = h5py.File(io.BytesIO(rsp.content), "r")
        bboxs = f.get("digitStruct/bbox")
        names = f.get("digitStruct/name")
        for i in range(len(bboxs)):
            bbox = f[bboxs[i][0]]
            total, _ = bbox["label"].shape
            nums = []
            print(f"i:{i}")
            for j in range(total):
                print(f"j:{j}")
                if isinstance(bbox["height"][j][0], h5py.h5r.Reference):
                    height = f[bbox["height"][j][0]][0][0]
                    left = f[bbox["left"][j][0]][0][0]
                    top = f[bbox["top"][j][0]][0][0]
                    width = f[bbox["width"][j][0]][0][0]
                    label = f[bbox["label"][j][0]][0][0]
                else:
                    height = bbox["height"][j][0]
                    left = bbox["left"][j][0]
                    top = bbox["top"][j][0]
                    width = bbox["width"][j][0]
                    label = bbox["label"][j][0]
                nums.append(
                    {"label": label, "bbox": BoundingBox(left, top, width, height)}
                )
            name_obj = f[names[i][0]]
            name = "".join(chr(j[0]) for j in name_obj)
            ds.append(
                (
                    name,
                    {
                        "image": Image(
                            link=Link(uri=f"{PATH_ROOT}/{name}"),
                            display_name=name,
                            mime_type=MIMEType.JPEG,
                        ),
                        "numbers": nums,
                    },
                )
            )

    ds.commit()
    ds.close()


if __name__ == "__main__":
    build_ds()
