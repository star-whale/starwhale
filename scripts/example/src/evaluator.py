import time
import random
import typing as t
import os.path as osp
from functools import wraps

import numpy

from starwhale import Text, Image, Context, evaluation, multi_classification


def timing(func: t.Callable) -> t.Any:
    @wraps(func)
    def wrapper(*args: t.Any, **kwargs: t.Any) -> t.Any:
        start = time.time()
        result = func(*args, **kwargs)
        print(f"Time elapsed: {time.time() - start}")
        return result

    return wrapper


@timing
@evaluation.predict(
    replicas=1,
    log_mode="plain",
    log_dataset_features=["txt", "img", "label"],
)
def predict(data: t.Dict, external: t.Dict) -> t.Any:
    # Test relative path case
    file_name = osp.join("templates", "data.json")
    assert osp.exists(file_name)
    assert isinstance(external["context"], Context)
    assert external["dataset_uri"].name
    assert external["dataset_uri"].version
    return {
        "txt": data["txt"].to_str(),
        "value": numpy.exp([random.uniform(-10, 1) for i in range(0, 5)]).tolist(),
    }


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
        assert _data["_mode"] == "plain"
        assert "placeholder" not in _data["input"]
        assert isinstance(_data["input"]["img"], Image)
        assert _data["input"]["img"].owner
        assert isinstance(_data["input"]["txt"], Text)

        label.append(_data["input"]["label"])
        result.append(_data["output/txt"])
        pr.append(_data["output/value"])
    return label, result, pr
