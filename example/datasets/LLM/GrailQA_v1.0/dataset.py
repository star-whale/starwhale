import requests

from starwhale import Dataset

if __name__ == "__main__":
    Dataset.from_json(
        "grailqav1",
        requests.get(
            "https://starwhale-examples.oss-cn-beijing.aliyuncs.com/dataset/GrailQA_v1.0/GrailQA_v1.0/grailqa_v1.0_train.json",
            timeout=10,
        ).text,
    )
