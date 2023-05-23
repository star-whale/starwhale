import sys
import datasets as hf_datasets
from starwhale import dataset as starwhale_dataset

datasets_map = {
    "belle-cn-mini": "BelleGroup/train_0.5M_CN",
    "belle-cn05": "BelleGroup/train_0.5M_CN",
    "belle-cn10": "BelleGroup/train_1M_CN",
    "belle-cn20": "BelleGroup/train_2M_CN",
}


def build_dataset(name: str):
    with starwhale_dataset(name) as ds:
        hd_ds = hf_datasets.load_dataset(datasets_map[name], split="train")
        for idx, features in enumerate(hd_ds):
            if name == "belle-cn-mini":
                if idx > 10:
                    break
            ds.append(features)
        ds.commit()


if __name__ == "__main__":
    if len(sys.argv) == 2:
        name = sys.argv[1]
    else:
        name = "belle-cn05"

    build_dataset(name)
