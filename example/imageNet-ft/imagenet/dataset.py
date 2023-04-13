import os
import sys
from pathlib import Path

from starwhale import Image, dataset, MIMEType
from starwhale.consts.env import SWEnv

ROOT_DIR = Path(__file__).parent.parent
_LABEL_NAMES = ["hotdog", "not-hotdog"]


def build_ds(ds_uri: str):
    """
    build by sdk and with copy
    :param ds_uri: cloud://server/project/starwhale/dataset/hotdog_test
    """
    ds = dataset(ds_uri, create="empty")
    for idx, label in enumerate(_LABEL_NAMES):
        path = ROOT_DIR / "data" / tag / label
        for _fn in os.listdir(path):
            _f = path / _fn
            with open(_f, mode="rb") as image_file:
                ds.append(
                    {
                        "img": Image(
                            fp=image_file.read(),
                            display_name=_fn,
                            mime_type=MIMEType.PNG,
                        ),
                        "label": label,
                    }
                )
    ds.commit()
    ds.close()


if __name__ == "__main__":
    tag = sys.argv[1] or "test"
    # instance_uri cloud://server
    instance_uri = os.getenv(SWEnv.instance_uri)
    if instance_uri:
        _ds_uri = f"{instance_uri}/project/starwhale/dataset/hotdog_{tag}"
    else:
        _ds_uri = f"hotdog_{tag}"
    build_ds(_ds_uri)
