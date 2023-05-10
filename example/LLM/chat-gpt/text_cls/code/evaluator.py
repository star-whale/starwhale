import os

import openai

from starwhale import evaluation, multi_classification

openai.api_key = os.getenv("OPENAI_API_KEY")
_LABEL_NAMES = ["world", "sports", "business", "sci/tech"]


def label_number(raw: str) -> int:
    for idx, l in enumerate(_LABEL_NAMES):
        if l.upper() in raw.upper():
            return idx
    return 0


@evaluation.predict(replicas=2)
def ppl(data):
    # create a completion
    text = data["text"]
    chat_result = openai.ChatCompletion.create(
        model="gpt-3.5-turbo",
        messages=[
            {
                "role": "user",
                "content": f"please tell me which class does the text below belongs to. world , sports , business  or sci/tech : {text}. Answer me as short as possible",
            }
        ],
    )
    result_raw = chat_result.choices[0].message.content
    print(f"the class for : {text} is \n {result_raw}")
    return label_number(result_raw)


@evaluation.evaluate(
    use_predict_auto_log=True,
)
@multi_classification(
    confusion_matrix_normalize="all",
    show_hamming_loss=True,
    show_cohen_kappa_score=True,
    show_roc_auc=False,
    all_labels=[i for i in range(0, 4)],
)
def cmp(ppl_result):
    result, label = [], []
    for _data in ppl_result:
        label.append(_data["input"]["label"])
        result.append(_data["output"])

    return label, result
