import requests

from starwhale import Dataset

if __name__ == "__main__":
    Dataset.from_json(
        "qald9",
        requests.get(
            "https://raw.githubusercontent.com/ag-sc/QALD/master/9/data/qald-9-test-multilingual.json",
            timeout=10,
        ).text,
        field_selector="questions",
    )
