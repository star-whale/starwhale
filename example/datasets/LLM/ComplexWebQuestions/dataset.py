from starwhale import Dataset

if __name__ == "__main__":
    Dataset.from_json(
        name="compwebq",
        path="https://starwhale-examples.oss-cn-beijing.aliyuncs.com/dataset/compwebq/ComplexWebQuestions_test.json",
    )
