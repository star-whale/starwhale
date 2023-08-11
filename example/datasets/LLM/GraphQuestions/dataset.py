from starwhale import Dataset

if __name__ == "__main__":
    Dataset.from_json(
        "graph_questions_testing",
        path="https://raw.githubusercontent.com/ysu1989/GraphQuestions/master/freebase13/graphquestions.testing.json",
    )
