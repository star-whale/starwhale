from __future__ import annotations

from typing import Any, Dict, List, Callable, Optional

from starwhale.consts import DecoratorInjectAttr


def fine_tune(*args: Any, **kw: Any) -> Any:
    """
    This function can be used as a decorator to define a fine-tune function.

    Argument:
        resources: [Dict, optional] Resources for the predict task, such as memory, gpu etc. Current only supports
            the cloud instance.
        needs: [List[Callable], optional] The list of functions that the fine-tune function depends on.

    Examples:
    ```python
    from starwhale import experiment

    @experiment.fine_tune
    def ft():
        ...

    @experiment.fine_tune(resources={"nvidia.com/gpu": 1}, needs=[prepare_handler])
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


def _register_ft(
    func: Callable,
    resources: Optional[Dict[str, Any]] = None,
    needs: Optional[List[Callable]] = None,
) -> None:
    from .job import Handler

    Handler.register(
        name="fine_tune",
        resources=resources,
        concurrency=1,
        replicas=1,
        needs=needs,
        require_dataset=True,
    )(func)
