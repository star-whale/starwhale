from __future__ import annotations

import typing as t
from enum import Enum, unique
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

from starwhale.api import evaluation
from starwhale.utils.dict_util import flatten as flatten_dict


@unique
class MetricKind(Enum):
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

            _r: t.Dict[str, t.Any] = {"kind": MetricKind.MultiClassification.value}
            cr = classification_report(
                y_true, y_pred, output_dict=True, labels=all_labels
            )
            _summary_m = ["accuracy", "micro avg", "weighted avg", "macro avg"]
            _r["summary"] = {}
            for k in _summary_m:
                v = cr.get(k)
                if not v:
                    continue
                _r["summary"][k] = v

            if show_hamming_loss:
                _r["summary"]["hamming_loss"] = hamming_loss(y_true, y_pred)
            if show_cohen_kappa_score:
                _r["summary"]["cohen_kappa_score"] = cohen_kappa_score(y_true, y_pred)

            _record_summary = flatten_dict(_r["summary"], extract_sequence=True)
            _record_summary["kind"] = _r["kind"]

            evaluation.log_summary(_record_summary)

            _r["labels"] = {}
            mcm = multilabel_confusion_matrix(
                y_true, y_pred, labels=all_labels
            ).tolist()

            labels = all_labels or sorted([k for k in cr.keys() if k not in _summary_m])
            for _label, matrix in zip(labels, mcm):
                _label = str(_label)
                _report = cr.get(_label)
                if not _report:
                    continue

                _report.update(
                    {
                        "TN-True Negative": matrix[0][0],
                        "FP-False Positive": matrix[0][1],
                        "FN-False Negative": matrix[1][0],
                        "TP-True Positive": matrix[1][1],
                    }
                )

                _r["labels"][_label] = _report
                evaluation.log("labels", id=_label, metrics=_report)

            # TODO: tune performance, use intermediated result
            cm = confusion_matrix(
                y_true, y_pred, labels=all_labels, normalize=confusion_matrix_normalize
            )
            _cm_list = cm.tolist()
            _r["confusion_matrix"] = {"binarylabel": _cm_list}

            for _idx, _pa in enumerate(_cm_list):
                evaluation.log(
                    "confusion_matrix/binarylabel",
                    id=_idx,
                    metrics={str(_id): _v for _id, _v in enumerate(_pa)},
                )

            if show_roc_auc and all_labels is not None and y_true and y_pr:
                _r["roc_auc"] = {}
                for _idx, _label in enumerate(all_labels):
                    _ra_value = _calculate_roc_auc(y_true, y_pr, _label, _idx)
                    _r["roc_auc"][str(_label)] = _ra_value

                    for _id, (_fpr, _tpr, _threshold) in enumerate(
                        zip(_ra_value["fpr"], _ra_value["tpr"], _ra_value["thresholds"])
                    ):
                        evaluation.log(
                            f"roc_auc/{_label}",
                            id=_id,
                            metrics=dict(
                                tpr=_tpr,
                                fpr=_fpr,
                                threshold=_threshold,
                            ),
                        )

                        evaluation.log(
                            "labels", id=str(_label), metrics=dict(auc=_ra_value["auc"])
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
