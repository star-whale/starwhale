import os
import time
import random
import typing as t
import os.path as osp
import dataclasses
from enum import Enum
from functools import wraps

import numpy

from starwhale import Text, Image, Context, argument, evaluation, multi_classification
from starwhale.utils import in_container

try:
    from .util import random_image
except ImportError:
    from util import random_image


class IntervalStrategy(Enum):
    NO = "no"
    STEPS = "steps"
    EPOCH = "epoch"


@dataclasses.dataclass
class TestArguments:
    learning_rate: float = dataclasses.field(default=0.01, metadata={"help": "lr"})
    epoch: int = dataclasses.field(default=10, metadata={"help": "epoch"})
    labels: t.Optional[t.List[str]] = dataclasses.field(
        default=None, metadata={"help": "labels"}
    )
    debug: bool = dataclasses.field(default=False, metadata={"help": "debug"})
    evaluation_strategy: t.Union[IntervalStrategy, str] = dataclasses.field(
        default="no", metadata={"help": "evaluation strategy"}
    )
    default_value: str = dataclasses.field(default="default value")


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

    _check_argument_values(argument)

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
    _check_argument_values(argument)

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


def _check_argument_values(argument: TestArguments) -> None:
    # TODO: support to specify model run arguments to server instance
    if os.environ.get("SW_INSTANCE_URI", "local") != "local":
        return
    assert isinstance(argument, TestArguments)
    # the values are configured in `scripts/client_test/cli_test.py`
    assert argument.default_value == "default value"
    assert argument.learning_rate == 0.1
    assert argument.epoch == 100
    assert argument.labels == ["l1", "l2", "l3"]
    assert argument.debug is True
    assert argument.evaluation_strategy == "steps"
