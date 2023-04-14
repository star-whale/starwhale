import requests

from starwhale import Dataset

if __name__ == "__main__":
    Dataset.from_json(
        "webqsp",
        requests.get(
            "https://starwhale-examples.oss-cn-beijing.aliyuncs.com/dataset/WebQSP/data/WebQSP.test.json",
            timeout=10,
        ).text,
        field_selector="Questions",
    )
