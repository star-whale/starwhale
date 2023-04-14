import requests

from starwhale import Dataset

if __name__ == "__main__":
    Dataset.from_json(
        "compwebq",
        requests.get(
            "https://starwhale-examples.oss-cn-beijing.aliyuncs.com/dataset/compwebq/ComplexWebQuestions_test.json",
            timeout=10,
        ).text,
    )
