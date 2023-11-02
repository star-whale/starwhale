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
from io import BytesIO

from datasets import load_dataset, get_dataset_split_names
from starwhale import Image, dataset, MIMEType
from starwhale.consts.env import SWEnv

ROOT_DIR = Path(__file__).parent


def build_ds_from_local_fs(ds_uri):
    """
    build by sdk and with copy
    """
    ds = dataset(ds_uri, create="empty")
    print("preparing data...")
    data_path = ROOT_DIR / "data"
    lines = open(data_path / "meta.txt", encoding="utf-8").read().strip().split("\n")

    for line in lines:
        v = line.split("\t")
        img_path = data_path / v[0]
        with open(img_path, mode="rb") as image_file:
            ds.append(
                {
                    "image": Image(
                        fp=image_file.read(),
                        display_name=v[0],
                        mime_type=MIMEType.PNG,
                    ),
                    "text": v[1],
                }
            )
    ds.commit()
    ds.close()
    print("build done!")


def build_ds_from_hf(ds_uri, dataset_name: str = "lambdalabs/pokemon-blip-captions"):
    ds = dataset(ds_uri, create="empty")
    hf_ds = load_dataset(dataset_name, cache_dir="cache")
    max_number = 100
    index = 0
    print("preparing data...")
    for row in hf_ds["train"]:
        print(f"{index}/{max_number}")
        if max_number < 0:
            break
        bytes = BytesIO()
        row.get("image").save(bytes, format='PNG')
        ds.append(
            {
                "image": Image(
                    fp=bytes.getvalue(),
                    mime_type=MIMEType.PNG,
                ),
                "text": row.get("text"),
            }
        )
        max_number -= 1
        index += 1
    ds.commit()
    ds.close()
    print("build done!")


if __name__ == "__main__":
    instance_uri = os.getenv(SWEnv.instance_uri)
    if instance_uri:
        _ds_uri = f"{instance_uri}/project/starwhale/dataset/pokemon-blip-captions-train"
    else:
        _ds_uri = f"pokemon-blip-captions-train"
    # build_ds_from_local_fs(_ds_uri)
    build_ds_from_hf(_ds_uri)
