import numpy as np
import requests

from starwhale import Link, Image, dataset, MIMEType, NumpyBinary  # noqa: F401
from starwhale.utils.retry import http_retry

PATH_ROOT = (
    "https://starwhale-examples.oss-cn-beijing.aliyuncs.com/dataset/sintel/training"
)
PATH_ALBEDO = "albedo"
PATH_CLEAN = "clean"
PATH_FINAL = "final"
PATH_FLOW = "flow"
PATH_FLOW_VIZ = "flow_viz"
PATH_INVALID = "invalid"
PATH_OCCLUSIONS = "occlusions"
SUFFIX_FLO = ".flo"
SUFFIX_PNG = ".png"
PREFIX_FILE = "frame_"


@http_retry
def request_link_json(index_link):
    return requests.get(index_link, timeout=10).json()


@http_retry
def request_link_content(link):
    return requests.get(link, timeout=10).content


def file_path(dir, dir2, file_name):
    return f"{PATH_ROOT}/{dir}/{dir2}/{file_name}"


def next_image(image_name: str):
    i_n = image_name.replace(PREFIX_FILE, "")
    i_n = i_n.replace(SUFFIX_PNG, "")
    return PREFIX_FILE + str(int(i_n) + 1).zfill(4) + SUFFIX_PNG


def path_to_image(image_path: str):
    return Image(
        link=Link(image_path),
        mime_type=MIMEType.PNG,
    )


def flo_to_binary(flow_bytes: bytes):
    (magic,) = np.frombuffer(flow_bytes[0:4], np.float32)
    if 202021.25 != magic:
        print("Magic number incorrect. Invalid .flo file")
    else:
        w = np.frombuffer(flow_bytes[4:8], np.int32)
        h = np.frombuffer(flow_bytes[8:12], np.int32)
        print(f"Reading {w} x {h} flo file")
        data = np.frombuffer(flow_bytes[12:], np.float32)
        return NumpyBinary(
            fp=data.tobytes(), shape=(h.item(), w.item(), 2), dtype=np.float32
        )


def build_ds():
    ds = dataset("sintel")
    json = request_link_json(f"{PATH_ROOT}/tree.json")
    for folder in json:
        if folder.get("name") != PATH_FLOW:
            continue
        for d in folder["contents"]:
            _dir = d["name"]
            for f in d["contents"]:
                flo_fn = str(f["name"])
                img_file_name = flo_fn.replace(SUFFIX_FLO, SUFFIX_PNG)
                ds.append(
                    {
                        "frame0/albedo": path_to_image(
                            file_path(PATH_ALBEDO, _dir, img_file_name)
                        ),
                        "frame0/clean": path_to_image(
                            file_path(PATH_CLEAN, _dir, img_file_name)
                        ),
                        "frame0/final": path_to_image(
                            file_path(PATH_FINAL, _dir, img_file_name)
                        ),
                        "frame1/albedo": path_to_image(
                            file_path(PATH_ALBEDO, _dir, next_image(img_file_name))
                        ),
                        "frame1/clean": path_to_image(
                            file_path(PATH_CLEAN, _dir, next_image(img_file_name))
                        ),
                        "frame1/final": path_to_image(
                            file_path(PATH_FINAL, _dir, next_image(img_file_name))
                        ),
                        "flow_viz": path_to_image(
                            file_path(PATH_FLOW_VIZ, _dir, img_file_name)
                        ),
                        "flow_bin": flo_to_binary(
                            request_link_content(file_path(PATH_FLOW, _dir, flo_fn))
                        ),
                        "pix_occlusions": path_to_image(
                            file_path(PATH_OCCLUSIONS, _dir, img_file_name)
                        ),
                        "pix_invalid": path_to_image(
                            file_path(PATH_INVALID, _dir, img_file_name)
                        ),
                    }
                )

    ds.commit()
    ds.close()


if __name__ == "__main__":
    build_ds()
