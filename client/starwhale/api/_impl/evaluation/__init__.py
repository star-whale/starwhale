from __future__ import annotations

import typing as t

from starwhale.consts import DecoratorInjectAttr
from starwhale.base.type import PredictLogMode

from .log import EvaluationLogStore
from .pipeline import PipelineHandler


def predict(*args: t.Any, **kw: t.Any) -> t.Any:
    """Defines a predict function.

    This function can be used as a decorator to define an evaluation-predict function that maps the dataset data into
    the different predict workers.

    Arguments:
        datasets: [Union[List[str], None], optional] Use the datasets rows as the input data for the predict function.
        resources: [Dict, optional] Resources for the predict task, such as memory, gpu etc. Current only supports
            the cloud instance.
        concurrency: [int, optional] The concurrency of the predict tasks. Default is 1.
        replicas: [int, optional] The number of the predict tasks. Default is 1.
        batch_size: [int, optional] Number of samples per batch. Default is 1.
        fail_on_error: [bool, optional] Fast fail on the exceptions in the predict function. Default is True.
        auto_log: [bool, optional] Auto log the return values of the predict function and the according dataset rows. Default is True.
        log_mode: [str, optional] When auto_log=True, the log_mode can be specified to control the log behavior. Options are `pickle` and `plain`. Default is `pickle`.
        log_dataset_features: [List[str], optional] When auto_log=True, the log_dataset_features can be specified to control the log dataset features behavior.
            Default is None, all dataset features will be logged. If the list is empty, no dataset features will be logged.
        needs: [List[Callable], optional] The list of the functions that need to be executed before the predict function.

    Examples:
    ```python

    from starwhale import evaluation

    @evaluation.predict
    def predict_image(data):
        ...

    @evaluation.predict(
        dataset="mnist/version/latest",
        batch_size=32,
        replicas=4,
    )
    def predict_batch_images(batch_data)
        ...
    ```

    Returns:
        The decorated function.
    """

    # TODO: support runtime

    if len(args) == 1 and len(kw) == 0 and callable(args[0]):
        return predict()(args[0])
    else:

        def _wrap(func: t.Callable) -> t.Any:
            _register_predict(func, **kw)
            setattr(func, DecoratorInjectAttr.Predict, True)
            return func

        return _wrap


def _register_predict(
    func: t.Callable,
    datasets: t.Optional[t.List[str]] = None,
    resources: t.Optional[t.Dict[str, t.Any]] = None,
    needs: t.Optional[t.List[t.Callable]] = None,
    concurrency: int = 1,
    replicas: int = 1,
    batch_size: int = 1,
    fail_on_error: bool = True,
    auto_log: bool = True,
    log_mode: str = PredictLogMode.PICKLE.value,
    log_dataset_features: t.Optional[t.List[str]] = None,
) -> None:
    from .job import Handler

    Handler.register(
        name="predict",
        resources=resources,
        concurrency=concurrency,
        needs=needs,
        replicas=replicas,
        extra_kwargs=dict(
            predict_batch_size=batch_size,
            ignore_error=not fail_on_error,
            predict_auto_log=auto_log,
            predict_log_mode=log_mode,
            predict_log_dataset_features=log_dataset_features,
            dataset_uris=datasets,
        ),
    )(func)


def evaluate(*args: t.Any, **kw: t.Any) -> t.Any:
    """Defines an evaluate function.

    This function can be used as a decorator to define an evaluation-evaluate function that reduces the results of the
    predict function.

    Argument:
        needs: [List[Callable], required] The list of the functions that need to be executed before the evaluate function.
        use_predict_auto_log: [bool, optional] Passing the iterator of the predict auto-log results into the evaluate function.
            Default is True.
        resources: [Dict, optional] Resources for the predict task, such as memory, gpu etc. Current only supports
            the cloud instance.

    Examples:
    ```python
    from starwhale import evaluation

    @evaluation.evaluate(needs=[predict_image])
    def evaluate_results(predict_result_iter):
        ...

    @evaluation.evaluate(
        use_predict_auto_log=False,
        needs=[predict_image],
    )
    def evaluate_results():
        ...
    ```

    Returns:
        The decorated function.
    """

    def _wrap(func: t.Callable) -> t.Any:
        _register_evaluate(func, **kw)
        setattr(func, DecoratorInjectAttr.Evaluate, True)
        return func

    return _wrap


def _register_evaluate(
    func: t.Callable,
    needs: t.Optional[t.List[t.Callable]] = None,
    resources: t.Optional[t.Dict[str, t.Any]] = None,
    use_predict_auto_log: bool = True,
) -> None:
    from .job import Handler

    if not needs:
        raise ValueError("needs is required for evaluate function")

    Handler.register(
        name="evaluate",
        resources=resources,
        concurrency=1,
        replicas=1,
        needs=needs,
        extra_kwargs=dict(
            predict_auto_log=use_predict_auto_log,
        ),
    )(func)


__all__ = ["EvaluationLogStore", "PipelineHandler", "predict", "evaluate"]
