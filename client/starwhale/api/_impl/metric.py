from __future__ import annotations
import typing as t
from functools import wraps

from sklearn.metrics import (
    classification_report,
    multilabel_confusion_matrix,
    confusion_matrix,
    hamming_loss,
    cohen_kappa_score,
)


class MetricKind:
    MultiClassification = "multi_classification"


def multi_classification(
    confusion_matrix_normalize: str = "all",
    show_hamming_loss: bool = True,
    show_cohen_kappa_score: bool = True,
    all_labels: t.Union[list[t.Any], None] = None,
):
    def _decorator(func):
        @wraps(func)
        def _wrapper(*args, **kwargs):
            y_true, y_pred = func(*args, **kwargs)

            _r: t.Dict[str, t.Any] = {"kind": MetricKind.MultiClassification}
            cr = classification_report(
                y_true, y_pred, output_dict=True, labels=all_labels
            )
            _summary_m = ["accuracy", "macro avg", "weighted avg"]
            _r["summary"] = {k: cr.get(k) for k in _summary_m}
            _r["labels"] = {k: v for k, v in cr.items() if k not in _summary_m}

            # TODO: tune performace, use intermediate result
            cm = confusion_matrix(
                y_true, y_pred, labels=all_labels, normalize=confusion_matrix_normalize
            )
            mcm = multilabel_confusion_matrix(y_true, y_pred, labels=all_labels)
            _r["confusion_matrix"] = {
                "binarylabel": cm.tolist(),
                "multilabel": mcm.tolist(),
            }
            if show_hamming_loss:
                _r["summary"]["hamming_loss"] = hamming_loss(y_true, y_pred)
            if show_cohen_kappa_score:
                _r["summary"]["cohen_kappa_score"] = cohen_kappa_score(y_true, y_pred)

            # TODO: add roc/auc metric
            return _r

        return _wrapper

    return _decorator
