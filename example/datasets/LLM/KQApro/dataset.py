import requests

from starwhale import Dataset

if __name__ == "__main__":
    Dataset.from_json(
        "kqapro",
        requests.get(
            "https://starwhale-examples.oss-cn-beijing.aliyuncs.com/dataset/KQAPro.IID/train.json",
            timeout=10,
        ).text,
    )
