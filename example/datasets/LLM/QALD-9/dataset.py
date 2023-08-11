from starwhale import Dataset

if __name__ == "__main__":
    Dataset.from_json(
        name="qald9",
        path="https://raw.githubusercontent.com/ag-sc/QALD/master/9/data/qald-9-test-multilingual.json",
        field_selector="questions",
    )
