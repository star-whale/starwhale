import io

import requests
import jsonlines

from starwhale import dataset
from starwhale.utils.debug import init_logger

init_logger(4)

hf_url = (
    "https://huggingface.co/datasets/timdettmers/openassistant-guanaco/resolve/main"
)

dataset_map = {
    "train": f"{hf_url}/openassistant_best_replies_train.jsonl",
    "eval": f"{hf_url}/openassistant_best_replies_eval.jsonl",
}


def build_dataset(name: str) -> None:
    print(f"Building {name} dataset...")
    with dataset(f"oasst-guanaco-{name}") as ds:
        text = requests.get(dataset_map[name], timeout=90).text
        reader = jsonlines.Reader(io.StringIO(text))
        for line in reader:
            ds.append(line)

        reader.close()
        ds.commit()


if __name__ == "__main__":
    build_dataset("train")
    build_dataset("eval")
