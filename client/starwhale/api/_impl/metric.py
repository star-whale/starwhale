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
)

from starwhale.utils.flatten import do_flatten_dict

from .model import PipelineHandler


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
            handler: PipelineHandler = args[0]

            _rt = func(*args, **kwargs)
            if show_roc_auc:
                y_true, y_pred, y_pr = _rt
            else:
                y_true, y_pred = _rt

            _r: t.Dict[str, t.Any] = {"kind": MetricKind.MultiClassification.value}
            cr = classification_report(
                y_true, y_pred, output_dict=True, labels=all_labels
            )
            _summary_m = ["accuracy", "macro avg", "weighted avg"]
            _r["summary"] = {k: cr.get(k) for k in _summary_m}

            _record_summary = do_flatten_dict(_r["summary"])
            _record_summary["kind"] = _r["kind"]
            handler.evaluation.log_metrics(_record_summary)

            _r["labels"] = {}
            for k, v in cr.items():
                if k in _summary_m:
                    continue
                _r["labels"][k] = v
                handler.evaluation.log("labels", id=k, **v)

            # TODO: tune performance, use intermediated result
            cm = confusion_matrix(
                y_true, y_pred, labels=all_labels, normalize=confusion_matrix_normalize
            )

            _cm_list = cm.tolist()
            _r["confusion_matrix"] = {"binarylabel": _cm_list}

            for idx, _pa in enumerate(_cm_list):
                handler.evaluation.log(
                    "confusion_matrix/binarylabel",
                    id=idx,
                    **{str(_id): _v for _id, _v in enumerate(_pa)},
                )

            if show_hamming_loss:
                _r["summary"]["hamming_loss"] = hamming_loss(y_true, y_pred)
            if show_cohen_kappa_score:
                _r["summary"]["cohen_kappa_score"] = cohen_kappa_score(y_true, y_pred)

            if show_roc_auc and all_labels is not None and y_true and y_pr:
                _r["roc_auc"] = {}
                for _idx, _label in enumerate(all_labels):
                    _ra_value = _calculate_roc_auc(y_true, y_pr, _label, _idx)
                    _r["roc_auc"][_label] = _ra_value

                    for _fpr, _tpr, _threshold in zip(
                        _ra_value["fpr"], _ra_value["tpr"], _ra_value["thresholds"]
                    ):
                        handler.evaluation.log(
                            f"roc_auc/{_label}",
                            id=_idx,
                            fpr=_fpr,
                            tpr=_tpr,
                            threshold=_threshold,
                        )
                        handler.evaluation.log(
                            "roc_auc/summary", id=_label, auc=_ra_value["auc"]
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
