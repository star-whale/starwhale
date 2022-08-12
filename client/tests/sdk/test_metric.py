import random
from unittest import skip

from starwhale.api._impl.metric import multi_classification


@skip
def test_multi_classification_metric():
    def _cmp():
        return (
            [1, 2, 3, 4, 5, 6, 7, 8, 9],
            [1, 3, 2, 4, 5, 6, 7, 8, 9],
            [[1 / random.randint(1, 10) for i in range(1, 10)] for i in range(1, 10)],
        )

    rt = multi_classification(
        confusion_matrix_normalize="all",
        show_hamming_loss=True,
        show_cohen_kappa_score=True,
        show_roc_auc=True,
        all_labels=[i for i in range(1, 10)],
    )(_cmp)()

    assert rt["kind"] == "multi_classification"
    assert "accuracy" in rt["summary"]
    assert "macro_avg" in rt["summary"]
    assert len(rt["labels"]) == 9
    assert "binarylabel" in rt["confusion_matrix"]
    assert "multilabel" in rt["confusion_matrix"]
    assert len(rt["roc_auc"]) == 9
