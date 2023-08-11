from starwhale import Dataset

if __name__ == "__main__":
    Dataset.from_json(
        name="kqapro",
        path="https://starwhale-examples.oss-cn-beijing.aliyuncs.com/dataset/KQAPro.IID/train.json",
    )
