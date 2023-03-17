import random
from pathlib import Path
from unittest.mock import patch, MagicMock

import pytest
from pyfakefs.fake_filesystem_unittest import TestCase

from starwhale.api._impl.metric import multi_classification
from starwhale.core.job.context import Context


class TestMultiClassificationMetric(TestCase):
    def setUp(self) -> None:
        context = Context(
            workdir=Path("/home/starwhale"),
            version="12345",
            project="self",
        )
        Context.set_runtime_context(context)

    @pytest.mark.filterwarnings(
        "ignore::sklearn.metrics._classification.UndefinedMetricWarning"
    )
    @patch("starwhale.api._impl.wrapper.Evaluation.log_metrics")
    def test_multi_classification_metric(self, log_metric_mock: MagicMock) -> None:
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

        metric_call = log_metric_mock.call_args[0][0]
        assert "weighted avg/precision" in metric_call
        assert list(rt["labels"].keys()) == ["a", "b", "c", "d"]
        assert "confusion_matrix/binarylabel" not in rt

    @pytest.mark.filterwarnings(
        "ignore::sklearn.metrics._classification.UndefinedMetricWarning"
    )
    @patch("starwhale.api._impl.wrapper.Evaluation.log_metrics")
    @patch("starwhale.api._impl.wrapper.Evaluation.log")
    def test_multi_classification_metric_with_pa(
        self, log_mock: MagicMock, log_metric_mock: MagicMock
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

        metric_call = log_metric_mock.call_args[0][0]
        assert isinstance(metric_call, dict)
        assert metric_call["kind"] == rt["kind"]
        assert "macro avg/f1-score" in metric_call

        log_calls = set([args[1]["table_name"] for args in log_mock.call_args_list])
        assert "labels" in log_calls
        assert "confusion_matrix/binarylabel" in log_calls
        assert "roc_auc/9" in log_calls
        assert "roc_auc/8" in log_calls
        assert "roc_auc/7" in log_calls
        assert "roc_auc/6" in log_calls
        assert "roc_auc/5" in log_calls
        assert "roc_auc/4" in log_calls
        assert "roc_auc/3" in log_calls
        assert "roc_auc/2" in log_calls
        assert "roc_auc/1" in log_calls

        roc_1_calls = set(
            [
                f"{args[1]['table_name']},{args[1]['id']}"
                for args in log_mock.call_args_list
                if args[1]["table_name"] == "roc_auc/1"
            ]
        )
        assert len(roc_1_calls) > 1
