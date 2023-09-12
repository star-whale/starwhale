import time
import random

from starwhale import Context, dataset, evaluation
from starwhale.utils.debug import console

try:
    from .utils import random_text
except ImportError:
    from utils import random_text


@evaluation.predict(
    replicas=100,
    resources={
        "memory": {"request": 100 * 1024, "limit": 1000 * 1024 * 1024},
        "cpu": {"request": 0.1, "limit": 1},
    },
    log_mode="plain",
)
def predict_text(data: dict, external: dict) -> str:
    seconds = random.randint(10, 300) / 1000
    time.sleep(seconds)
    out = random_text()
    ctx = Context.get_runtime_context()
    evaluation.log(
        category=f"task/{ctx.index}",
        id=f"{external['index_with_dataset']}",
        metrics={
            "dataset": str(external["dataset_uri"]),
            "seconds": seconds,
        },
    )
    return out


@evaluation.evaluate(needs=[predict_text], use_predict_auto_log=False)
def evaluation_results() -> None:
    results_cnt = 0
    for _ in evaluation.iter("results"):
        results_cnt += 1

    dataset_rows = 0
    ctx = Context.get_runtime_context()
    for dataset_uri in ctx.dataset_uris:
        console.log(f"dataset: {dataset_uri}")
        with dataset(dataset_uri) as ds:
            dataset_rows += len(ds)

    from starwhale.api.evaluation import EvaluationLogStore

    e_store = EvaluationLogStore._get_instance()
    table_names = e_store._datastore.get_tables()

    received_data_tasks = 0
    total_predict_count = 0
    for table_name in table_names:
        if not table_name.startswith("task/"):
            continue

        received_data_tasks += 1

        _cnt = len(list(evaluation.iter(table_name)))
        total_predict_count += _cnt

        evaluation.log(
            category="tasks-summary",
            id=table_name,
            metrics={"predict_count": _cnt},
        )

    metrics = {
        "dataset_rows": dataset_rows,
        "results_count": results_cnt,
        "received_data_tasks": received_data_tasks,
        "total_predict_count": total_predict_count,
    }
    console.log(metrics)
    evaluation.log_summary(metrics)

    if received_data_tasks <= 1:
        raise RuntimeError(f"received_data_tasks:{received_data_tasks} <= 1")

    if not (dataset_rows == total_predict_count == results_cnt):
        raise RuntimeError(
            f"dataset_rows:{dataset_rows} != total_predict_count:{total_predict_count} != results_cnt:{results_cnt},"
            "maybe some data rows are not executed or some data rows are executed more than once"
        )
