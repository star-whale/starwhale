from __future__ import annotations

import threading
from typing import Any, Dict, Callable, Optional

from starwhale.consts import DecoratorInjectAttr, DEFAULT_FINETUNE_JOB_NAME


def fine_tune(*args: Any, **kw: Any) -> Any:
    """
    This function can be used as a decorator to define a fine-tune function.

    Argument:
        resources: [Dict, optional] Resources for the predict task, such as memory, gpu etc. Current only supports
            the cloud instance.
    Examples:
    ```python
    from starwhale import experiment

    @experiment.fine_tune
    def ft():
        ...

    ```

    Returns:
        The decorated function
    """

    if len(args) == 1 and len(kw) == 0 and callable(args[0]):
        return fine_tune()(args[0])
    else:

        def _wrap(func: Callable) -> Any:
            _register_ft(func, resources=kw.get("resources"))
            setattr(func, DecoratorInjectAttr.FineTune, True)
            return func

    return _wrap


_registered_ft_func = threading.local()


def _register_ft(
    func: Callable,
    resources: Optional[Dict[str, Any]] = None,
) -> None:
    from .job import step

    try:
        val = _registered_ft_func.value
    except AttributeError:
        val = None

    if val is not None:
        return

    _registered_ft_func.value = step(
        job_name=DEFAULT_FINETUNE_JOB_NAME,
        name="fine_tune",
        resources=resources,
        concurrency=1,
        task_num=1,
    )(func)
