import random
from unittest import TestCase
from unittest.mock import MagicMock

from starwhale.api._impl.metric import multi_classification


class TestMultiClassificationMetric(TestCase):
    def test_multi_classification_metric(
        self,
    ) -> None:
        def _cmp(handler, data):
            return (
                ["a", "b", "c", "d", "a", "a", "a"],
                ["b", "b", "d", "d", "a", "a", "b"],
            )

        eval_handler = MagicMock()
        rt = multi_classification(
            confusion_matrix_normalize="all",
            show_hamming_loss=True,
            show_cohen_kappa_score=True,
            show_roc_auc=False,
            all_labels=["a", "b", "c", "d"],
        )(_cmp)(eval_handler, None)
        assert rt["kind"] == "multi_classification"

        metric_call = eval_handler.evaluation.log_metrics.call_args[0][0]
        assert "weighted avg/precision" in metric_call
        assert list(rt["labels"].keys()) == ["a", "b", "c", "d"]
        assert "confusion_matrix/binarylabel" not in rt

    def test_multi_classification_metric_with_pa(
        self,
    ) -> None:
        def _cmp(handler, data):
            return (
                [1, 2, 3, 4, 5, 6, 7, 8, 9],
                [1, 3, 2, 4, 5, 6, 7, 8, 9],
                [
                    [1 / random.randint(1, 10) for i in range(1, 10)]
                    for i in range(1, 10)
                ],
            )

        eval_handler = MagicMock()
        rt = multi_classification(
            confusion_matrix_normalize="all",
            show_hamming_loss=True,
            show_cohen_kappa_score=True,
            show_roc_auc=True,
            all_labels=[i for i in range(1, 10)],
        )(_cmp)(eval_handler, None)

        assert rt["kind"] == "multi_classification"
        assert "accuracy" in rt["summary"]
        assert "macro avg" in rt["summary"]
        assert len(rt["labels"]) == 9
        assert "binarylabel" in rt["confusion_matrix"]
        assert len(rt["roc_auc"]) == 9

        metric_call = eval_handler.evaluation.log_metrics.call_args[0][0]
        assert isinstance(metric_call, dict)
        assert metric_call["kind"] == rt["kind"]
        assert "macro avg/f1-score" in metric_call

        log_calls = set(
            [args[0][0] for args in eval_handler.evaluation.log.call_args_list]
        )
        assert "labels" in log_calls
        assert "confusion_matrix/binarylabel" in log_calls
        assert "roc_auc/9" in log_calls
        assert "roc_auc/1" in log_calls
