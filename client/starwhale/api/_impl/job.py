import typing as t
import numbers
import threading
from pathlib import Path
from functools import wraps

import yaml
from loguru import logger

from starwhale.consts import DEFAULT_EVALUATION_JOB_NAME
from starwhale.core.job import dag
from starwhale.utils.fs import ensure_file
from starwhale.utils.load import load_module

context_holder = threading.local()
resource_names: t.Dict[str, t.List] = {
    "cpu": [int, float],
    "nvidia.com/gpu": [int],
    "memory": [int, float],
}
attribute_names = ["request", "limit"]


def do_resource_transform(resources: t.Dict[str, t.Any]) -> t.List[t.Dict]:
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
                "resources value is illegal, attribute's type must be numer or dict"
            )

        for _k, _v in resources[_name].items():
            if type(_v) not in resource_names[_name]:
                raise RuntimeError(
                    f"resource:{_name} only support type:{resource_names[_name]}, but now is {type(_v)}"
                )
            if _v <= 0:
                raise RuntimeError(
                    f"{_k} only supports non-negative numbers, but now is {_v}"
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
) -> t.Any:
    _resources = resources or {}
    _needs = needs or []

    def decorator(func: t.Any) -> t.Any:
        if Parser.is_parse_stage():
            cls, _, func_name = func.__qualname__.rpartition(".")

            _step = dict(
                job_name=job_name,
                step_name=func_name,
                cls_name=cls,
                resources=do_resource_transform(_resources),
                concurrency=concurrency,
                task_num=task_num,
                needs=_needs,
            )
            Parser.add_job(job_name, _step)

        return func

    return decorator


def pass_context(func: t.Any) -> t.Any:
    @wraps(func)
    def wrap_func(*args: t.Any, **kwargs: t.Any) -> t.Any:
        kwargs["context"] = context_holder.context
        return func(*args, **kwargs)

    return wrap_func


# TODO: support __setattr__, __getattr__ function for Context
# TODO: use another Context name, such as JobRunContext
class Context:
    def __init__(
        self,
        workdir: Path,
        step: str = "",
        total: int = 1,
        index: int = 0,
        dataset_uris: t.List[str] = [],
        version: str = "",
        project: str = "",
    ):
        self.project = project
        self.version = version
        self.step = step
        self.total = total
        self.index = index
        self.dataset_uris = dataset_uris
        self.workdir = workdir

    def __str__(self) -> str:
        return f"step:{self.step}, index:{self.index}/{self.total}"

    def __repr__(self) -> str:
        return f"step:{self.step}, index:{self.index}/{self.total}, version:{self.version}, dataset_uris:{self.dataset_uris}"


class ParseConfig:
    def __init__(self, is_parse_stage: bool, jobs: t.Dict[str, t.List[t.Dict]]):
        self.parse_stage = is_parse_stage
        self.jobs = jobs

    def clear(self) -> None:
        self.jobs = {}


# shared memory, not thread safe
parse_config = ParseConfig(False, {})


class Parser:
    @staticmethod
    def set_parse_stage(parse_stage: bool) -> None:
        parse_config.parse_stage = parse_stage

    @staticmethod
    def is_parse_stage() -> bool:
        return parse_config.parse_stage

    @staticmethod
    def add_job(job_name: str, step: t.Dict) -> None:
        _jobs = parse_config.jobs
        if job_name not in _jobs:
            parse_config.jobs[job_name] = []

        parse_config.jobs[job_name].append(step)

    @staticmethod
    def get_jobs() -> t.Dict[str, t.List[t.Dict]]:
        return parse_config.jobs

    # load is unique,so don't need to think multi load and clean
    @staticmethod
    def clear_config() -> None:
        global parse_config
        parse_config.clear()

    @staticmethod
    def parse_job_from_module(module: str, path: Path) -> t.Dict[str, t.List[t.Dict]]:
        """
        parse @step from module
        :param module: module name
        :param path: abs path
        :return: jobs
        """
        Parser.set_parse_stage(True)
        # parse DAG
        logger.debug(f"parse @step for module:{module}")
        load_module(module, path)
        _jobs = Parser.get_jobs().copy()
        Parser.clear_config()
        return _jobs

    @staticmethod
    def generate_job_yaml(module: str, path: Path, target_file: Path) -> None:
        """
        generate job yaml
        :param target_file: yaml target path
        :param module: module name
        :param path: abs path
        :return: None
        """
        _jobs = Parser.parse_job_from_module(module, path)
        # generate DAG
        logger.debug("generate DAG")
        if Parser.check(_jobs):
            # dump to target
            ensure_file(target_file, yaml.safe_dump(_jobs, default_flow_style=False))
            logger.debug("generator DAG success!")
        else:
            logger.error("generator DAG error! reason: check is failed.")
            raise RuntimeError("generator DAG error!")

    @staticmethod
    def check(jobs: t.Dict[str, t.List[t.Dict]]) -> bool:
        checks = []
        logger.debug(f"check jobs:{jobs}")

        for name, steps in jobs.items():
            _vertices: t.List[str] = []
            _edges: t.Dict[str, str] = {}
            for _step in steps:
                _vertices.append(_step["step_name"])
                for _pre in _step["needs"]:
                    if _pre:
                        _edges[_pre] = _step["step_name"]
            try:
                dag.generate_dag(_vertices, _edges)
                checks.append(True)
            except RuntimeError as e:
                logger.error(f"check job:{name} failed, error:{e}")
                checks.append(False)

        return all(checks)
