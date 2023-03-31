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

from starwhale import Image, dataset, MIMEType

ROOT_DIR = Path(__file__).parent.parent
_LABEL_NAMES = ["hotdog", "not-hotdog"]


def build_ds(push_to: str, tag: str = "test"):
    """
    build by sdk and with copy
    :param push_to: cloud://server/project/starwhale
    :param tag: dataset tag
    """
    ds = dataset(f"cloud://server/project/starwhale/dataset/hotdog_{tag}")
    for idx, label in enumerate(_LABEL_NAMES):
        path = ROOT_DIR / "data" / tag / label
        for _f in os.listdir(path):
            ds.append({
                "img": Image(fp=path / _f, display_name=_f, mime_type=MIMEType.PNG),
                "label": label,
            })
    ds.commit()
    ds.close()
    # if push_to:
    #     ds.copy(push_to)


if __name__ == "__main__":
    build_ds(tag=sys.argv[1], push_to=os.getenv("STARWHALE_SERVER_PROJECT_URI", "cloud://server/project/starwhale"))
