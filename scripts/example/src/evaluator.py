import os
import random
import typing as t

import numpy

from starwhale import PipelineHandler, multi_classification

_ROOT_DIR = os.path.dirname(os.path.dirname(__file__))


class SimplePipeline(PipelineHandler):
    def ppl(self, data: dict, **kw):
        return (
            data["txt"].content,
            numpy.exp([random.uniform(-10, 1) for i in range(0, 100)]).tolist(),
        )

    @multi_classification(
        confusion_matrix_normalize="all",
        show_hamming_loss=True,
        show_cohen_kappa_score=True,
        show_roc_auc=True,
        all_labels=[f"label-{i}" for i in range(0, 100)],
    )
    def cmp(self, ppl_result: t.Iterator):
        result, label, pr = [], [], []
        for _data in ppl_result:
            label.append(_data["ds_data"]["label"])
            result.append(_data["result"][0])
            pr.append(_data["result"][1])
        return label, result, pr
