#  Copyright 2022 Starwhale, Inc. All Rights Reserved.
#
#  Licensed under the Apache License, Version 2.0 (the "License");
#  you may not use this file except in compliance with the License.
#  You may obtain a copy of the License at
#
#  http://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License.

import os
import sys
from pathlib import Path

from starwhale.consts.env import SWEnv

from starwhale import Image, MIMEType, dataset

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
