import os

from Bard import Chatbot

from starwhale import evaluation, multi_classification

# https://github.com/acheong08/Bard
# This evaluation shall be run in USA or UK, or errors would be raised
bard_key = os.getenv("BARD_TOKEN")
chatbot = Chatbot(bard_key)


_LABEL_NAMES = ["world", "sports", "business", "sci/tech"]


def label_number(raw: str) -> int:
    for idx, l in enumerate(_LABEL_NAMES):
        if l.upper() in raw.upper():
            return idx
    return 0


@evaluation.predict
def ppl(data):
    text = data["text"]
    result_raw = chatbot.ask(
        f"please tell me which class does the text below belongs to. world , sports , business  or sci/tech : {text}. Answer me as short as possible"
    ).get("content")
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
