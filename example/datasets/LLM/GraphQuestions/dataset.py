import requests

from starwhale import Dataset

if __name__ == "__main__":
    Dataset.from_dict(
        name="graph_questions_testing",
        data=requests.get(
            "https://raw.githubusercontent.com/ysu1989/GraphQuestions/master/freebase13/graphquestions.testing.json",
            timeout=10,
        ).json(),
    )
