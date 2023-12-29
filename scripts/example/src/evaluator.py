import time
import random
import typing as t
import os.path as osp
import dataclasses
from functools import wraps

import numpy

from starwhale import (
    Text,
    Image,
    Context,
    Dataset,
    handler,
    argument,
    IntInput,
    ListInput,
    evaluation,
    ContextInput,
    DatasetInput,
    HandlerInput,
    multi_classification,
)
from starwhale.utils import in_container

try:
    from .util import random_image
except ImportError:
    from util import random_image


@dataclasses.dataclass
class TestArguments:
    epoch: int = dataclasses.field(default=10, metadata={"help": "epoch"})


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
@argument(TestArguments)
def predict(data: t.Dict, external: t.Dict, argument) -> t.Any:
    # Test relative path case
    file_name = osp.join("templates", "data.json")
    assert osp.exists(file_name)
    assert isinstance(external["context"], Context)
    assert external["dataset_uri"].name
    assert external["dataset_uri"].version
    assert isinstance(argument, TestArguments)
    assert argument.epoch == 10

    if in_container():
        assert osp.exists("/tmp/runtime-command-run.flag")

    return {
        "txt": data["txt"].to_str(),
        "value": numpy.exp([random.uniform(-10, 1) for i in range(0, 5)]).tolist(),
        "image": random_image(),
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
@argument(TestArguments)
def evaluate(ppl_result: t.Iterator, argument: TestArguments) -> t.Any:
    assert isinstance(argument, TestArguments)
    assert argument.epoch == 10

    result, label, pr = [], [], []
    for _data in ppl_result:
        assert _data["_mode"] == "plain"
        assert "placeholder" not in _data["input"]
        assert isinstance(_data["input"]["img"], Image)
        assert len(_data["input"]["img"].to_bytes()) > 0
        assert isinstance(_data["input"]["txt"], Text)
        assert isinstance(_data["output/image"], Image)
        assert len(_data["output/image"].to_bytes()) > 0

        label.append(_data["input"]["label"])
        result.append(_data["output/txt"])
        pr.append(_data["output/value"])
    return label, result, pr


class MyInput(HandlerInput):
    def parse(self, user_input):
        return f"MyInput {user_input}"


class X:
    def __init__(self) -> None:
        self.a = 1

    @handler()
    def f(
        self,
        x=ListInput(IntInput),
        y=2,
        mi=MyInput(),
        ds=DatasetInput(required=True),
        ctx=ContextInput(),
    ):
        assert self.a + x[0] == 3
        assert self.a + x[1] == 2
        assert y == 2
        assert mi == "MyInput blab-la"
        assert isinstance(ds, Dataset)
        assert isinstance(ctx, Context)


@handler()
def f(
    x=ListInput(IntInput()),
    y=2,
    mi=MyInput(),
    ds=DatasetInput(required=True),
    ctx=ContextInput(),
):
    assert x[0] == 2
    assert x[1] == 1
    assert y == 2
    assert mi == "MyInput blab-la"

    assert isinstance(ds, Dataset)
    assert isinstance(ctx, Context)
