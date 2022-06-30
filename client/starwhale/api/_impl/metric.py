from __future__ import annotations

import typing as t
from functools import wraps

from sklearn.metrics import (  # type: ignore
    auc,
    roc_curve,
    hamming_loss,
    confusion_matrix,
    cohen_kappa_score,
    classification_report,
    multilabel_confusion_matrix,
)


class MetricKind:
    MultiClassification = "multi_classification"


def multi_classification(
    confusion_matrix_normalize: str = "all",
    show_hamming_loss: bool = True,
    show_cohen_kappa_score: bool = True,
    show_roc_auc: bool = True,
    all_labels: t.Optional[t.List[t.Any]] = None,
) -> t.Any:
    def _decorator(func: t.Any) -> t.Any:
        @wraps(func)
        def _wrapper(*args: t.Any, **kwargs: t.Any) -> t.Dict[str, t.Any]:
            y_pr: t.Any = None

            _rt = func(*args, **kwargs)
            if show_roc_auc:
                y_true, y_pred, y_pr = _rt
            else:
                y_true, y_pred = _rt

            _r: t.Dict[str, t.Any] = {"kind": MetricKind.MultiClassification}
            cr = classification_report(
                y_true, y_pred, output_dict=True, labels=all_labels
            )
            _summary_m = ["accuracy", "macro avg", "weighted avg"]
            _r["summary"] = {k: cr.get(k) for k in _summary_m}
            _r["labels"] = {k: v for k, v in cr.items() if k not in _summary_m}

            # TODO: tune performace, use intermediated result
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

            if show_roc_auc and all_labels is not None and y_true and y_pr:
                _r["roc_auc"] = {}
                for _idx, _label in enumerate(all_labels):
                    _r["roc_auc"][_label] = _calculate_roc_auc(
                        y_true, y_pr, _label, _idx
                    )
            return _r

        return _wrapper

    return _decorator


def _calculate_roc_auc(
    y_pred: t.List[t.Any],
    y_pr: t.List[t.Any],
    label: t.Any,
    idx: int,
) -> t.Dict[str, t.Any]:
    # TODO: add sample rate
    y_bin_true = [int(i == label) for i in y_pred]
    y_label_pr = [m[idx] for m in y_pr]
    fpr, tpr, thresholds = roc_curve(y_bin_true, y_label_pr)
    return dict(
        fpr=fpr.tolist(),
        tpr=tpr.tolist(),
        auc=auc(fpr, tpr),
        thresholds=thresholds.tolist(),
    )
