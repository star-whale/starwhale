from pathlib import Path

from transformers import pipeline, AutoTokenizer, AutoModelForSequenceClassification

from starwhale import PipelineHandler, multi_classification

ROOTDIR = Path(__file__).parent.parent
_LABEL_NAMES = ["LABEL_0", "LABEL_1", "LABEL_2", "LABEL_3"]


class TextClassificationHandler(PipelineHandler):
    def __init__(self) -> None:
        super().__init__()
        tokenizer = AutoTokenizer.from_pretrained(str(ROOTDIR / "models"))
        model = AutoModelForSequenceClassification.from_pretrained(
            str(ROOTDIR / "models")
        )
        self.mode = pipeline(
            task="text-classification", model=model, tokenizer=tokenizer
        )

    def ppl(self, data):
        _r = self.mode(data["text"])
        return _LABEL_NAMES.index(_r[0]["label"])

    @multi_classification(
        confusion_matrix_normalize="all",
        show_hamming_loss=True,
        show_cohen_kappa_score=True,
        show_roc_auc=False,
        all_labels=[i for i in range(0, 4)],
    )
    def cmp(self, ppl_result):
        result, label = [], []
        for _data in ppl_result:
            label.append(_data["ds_data"]["label"])
            result.append(_data["result"])

        return label, result
