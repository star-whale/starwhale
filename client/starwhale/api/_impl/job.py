from __future__ import annotations

import sys
import typing as t
import inspect
import numbers
import threading
from copy import deepcopy
from pathlib import Path
from functools import wraps
from collections import defaultdict

import yaml
from loguru import logger

from starwhale.consts import DecoratorInjectAttr, DEFAULT_EVALUATION_JOB_NAME
from starwhale.core.job import dag
from starwhale.utils.fs import ensure_file
from starwhale.utils.load import load_module
from starwhale.utils.error import NoSupportError
from starwhale.core.job.step import Step
from starwhale.core.job.context import Context

_T_JOBS = t.Dict[str, t.List[Step]]
_jobs_global: _T_JOBS = defaultdict(list)
_jobs_global_lock = threading.Lock()


# TODO: refactor _do_resource_transform, move it into Step
def _do_resource_transform(resources: t.Optional[t.Dict[str, t.Any]]) -> t.List[t.Dict]:
    attribute_names = ["request", "limit"]
    resource_names: t.Dict[str, t.List] = {
        "cpu": [int, float],
        "nvidia.com/gpu": [int],
        "memory": [int, float],
    }

    resources = resources or {}
    results = []
    for _name, _resource in resources.items():
        if _name not in resource_names:
            raise RuntimeError(
                f"resources name is illegal, name must in {resource_names.keys()}"
            )

        if isinstance(_resource, numbers.Number):
            resources[_name] = {"request": _resource, "limit": _resource}
        elif isinstance(_resource, dict):
            if not all(n in attribute_names for n in _resource):
                raise RuntimeError(
                    f"resources value is illegal, attribute's name must in {attribute_names}"
                )
        else:
            raise RuntimeError(
                "resources value is illegal, attribute's type must be number or dict"
            )

        for _k, _v in resources[_name].items():
            if type(_v) not in resource_names[_name]:
                raise RuntimeError(
                    f"resource:{_name} only support type:{resource_names[_name]}, but now is {type(_v)}"
                )
            if _v <= 0:
                raise RuntimeError(
                    f"{_k} only supports non-negative number, but now is {_v}"
                )
        results.append(
            {
                "type": _name,
                "request": resources[_name]["request"],
                "limit": resources[_name]["limit"],
            }
        )
    return results


def step(
    job_name: str = DEFAULT_EVALUATION_JOB_NAME,
    resources: t.Optional[t.Dict[str, t.Any]] = None,
    concurrency: int = 1,
    task_num: int = 1,
    needs: t.Optional[t.List[str]] = None,
    extra_args: t.Optional[t.List] = None,
    extra_kwargs: t.Optional[t.Dict] = None,
    name: str = "",
) -> t.Callable:
    def decorator(func: t.Callable) -> t.Callable:
        module = inspect.getmodule(func)
        if module is None:
            raise RuntimeError(f"cannot get module name from func: {func}")

        if inspect.isclass(func):
            raise NoSupportError(f"step decorator no support class: {func}")

        cls_name, _, func_name = func.__qualname__.rpartition(".")
        if "." in cls_name:
            raise NoSupportError(
                f"step decorator no supports inner class method:{func.__qualname__}"
            )

        _step = Step(
            job_name=job_name,
            name=name or func_name,
            func_name=func_name,
            cls_name=cls_name,
            module_name=module.__name__,
            concurrency=concurrency,
            task_num=task_num,
            needs=needs,
            resources=_do_resource_transform(resources),
            extra_args=extra_args,
            extra_kwargs=extra_kwargs,
        )

        global _jobs_global, _jobs_global_lock
        with _jobs_global_lock:
            _jobs_global[job_name].append(_step)

        setattr(func, DecoratorInjectAttr.Step, True)
        return func

    return decorator


def _validate_jobs_dag(jobs: _T_JOBS) -> None:
    for steps in jobs.values():
        _vertices: t.List[str] = []
        _edges: t.Dict[str, str] = {}
        for _step in steps:
            _vertices.append(_step.name)
            for _pre in _step.needs:
                if _pre:
                    _edges[_pre] = _step.name

        dag.generate_dag(_vertices, _edges)


def _preload_to_register_jobs(run_handler: str, workdir: Path) -> None:
    """
    run_handler formats:
    - to.one.module
    - to.one.module:function  --> with @step, @predict decorator
    - to.one.module:ArbitraryClass  --> use @step or @predict to decorate some class methods
    - to.one.module:PipeHandlerSubClass
    """
    module_name, _, func_or_cls_name = run_handler.partition(":")

    # reload for the multi model.build in one python process
    if module_name in sys.modules:
        del sys.modules[module_name]

    module = load_module(module_name, workdir)
    func_or_cls = getattr(module, func_or_cls_name, None)

    if func_or_cls is None:
        logger.debug(f"preload module-{module_name}")
        return

    # TODO: raise exception for @step, @predict or @evaluate in the Pipeline methods
    # TODO: step decorate auto inject some flags into function builtin fields
    # TODO: need to handle DecoratorInjectAttr.Evaluate ?
    if inspect.isfunction(func_or_cls):
        if getattr(func_or_cls, DecoratorInjectAttr.Predict, False) or getattr(
            func_or_cls, DecoratorInjectAttr.Step, False
        ):
            logger.debug(
                f"preload function-{func_or_cls_name} from module-{module_name}"
            )
        else:
            raise RuntimeError(
                f"preload function-{func_or_cls_name} does not use step or predict decorator"
            )
    elif inspect.isclass(func_or_cls):
        from starwhale.api._impl.evaluation import PipelineHandler

        if issubclass(func_or_cls, PipelineHandler):
            ppl_func = getattr(func_or_cls, "ppl")
            cmp_func = getattr(func_or_cls, "cmp")
            step(task_num=2, name="ppl")(ppl_func)
            step(task_num=1, needs=["ppl"], name="cmp")(cmp_func)
            logger.debug(f"preload class-{func_or_cls_name} from Pipeline")
        else:
            logger.debug(f"preload user custom class-{func_or_cls_name}")
    else:
        raise NoSupportError(f"failed to preload for {run_handler}")


def generate_jobs_yaml(
    run_handler: str,
    workdir: t.Union[Path, str],
    yaml_path: t.Union[Path, str],
    job_name: str = DEFAULT_EVALUATION_JOB_NAME,
) -> None:
    workdir = Path(workdir)
    logger.debug(f"ingest steps from run_handler {run_handler} at {workdir}")

    _preload_to_register_jobs(run_handler, workdir)

    global _jobs_global, _jobs_global_lock
    with _jobs_global_lock:
        jobs = {job_name: deepcopy(_jobs_global.get(job_name))}

        _jobs_global.__delitem__(job_name)  # type: ignore[func-returns-value]

    if not jobs:
        raise RuntimeError(f"not found any jobs for {job_name}")

    _validate_jobs_dag(jobs)
    ensure_file(
        yaml_path,
        yaml.safe_dump(
            {job_name: [s.asdict() for s in steps] for job_name, steps in jobs.items()},
            default_flow_style=False,
        ),
        parents=True,
    )


def pass_context(func: t.Any) -> t.Any:
    @wraps(func)
    def wrap_func(*args: t.Any, **kwargs: t.Any) -> t.Any:
        kwargs["context"] = Context.get_runtime_context()
        return func(*args, **kwargs)

    return wrap_func
