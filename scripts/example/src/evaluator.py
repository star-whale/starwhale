import os
import random
import typing as t
import os.path as osp

import numpy

from starwhale import evaluation, multi_classification


@evaluation.predict(
    replicas=1,
    log_mode="plain",
)
def predict(data: t.Dict) -> t.Any:
    # Test relative path case
    file_name = osp.join("templates", "data.json")
    assert osp.exists(file_name)
    return (
        data["txt"].content,
        numpy.exp([random.uniform(-10, 1) for i in range(0, 5)]).tolist(),
    )


@evaluation.evaluate(
    use_predict_auto_log=True,
    needs=[predict],
)
@multi_classification(
    confusion_matrix_normalize="all",
    show_hamming_loss=True,
    show_cohen_kappa_score=True,
    show_roc_auc=True,
    all_labels=[f"label-{i}" for i in range(0, 5)],
)
def evaluate(ppl_result: t.Iterator):
    result, label, pr = [], [], []
    for _data in ppl_result:
        label.append(_data["input"]["label"])
        result.append(_data["output"][0])
        pr.append(_data["output"][1])
    return label, result, pr
