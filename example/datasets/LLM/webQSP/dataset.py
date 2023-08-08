from starwhale import Dataset

if __name__ == "__main__":
    Dataset.from_json(
        name="webqsp",
        path="https://starwhale-examples.oss-cn-beijing.aliyuncs.com/dataset/WebQSP/data/WebQSP.test.json",
        field_selector="Questions",
    )
