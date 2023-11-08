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
from pathlib import Path

from starwhale import dataset
from starwhale.consts.env import SWEnv

ROOT_DIR = Path(__file__).parent


if __name__ == "__main__":
    instance_uri = os.getenv(SWEnv.instance_uri)
    if instance_uri:
        ds_uri = f"{instance_uri}/project/starwhale/dataset/pokemon-blip-captions-eval"
    else:
        ds_uri = "pokemon-blip-captions-eval"
    ds = dataset(ds_uri, create="empty")
    print("preparing data...")
    lines = open(ROOT_DIR / "eval.txt", encoding="utf-8").read().strip().split("\n")

    for line in lines:
        ds.append(
            {
                "text": line,
            }
        )
    ds.commit()
    ds.close()
    print("build done!")
