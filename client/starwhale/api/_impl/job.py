import typing as t
from pathlib import Path
from functools import wraps

import yaml
from loguru import logger

from starwhale.consts import (
    thread_local,
    DEFAULT_EVALUATION_JOB_NAME,
    DEFAULT_EVALUATION_RESOURCE,
)
from starwhale.core.job import dag
from starwhale.utils.fs import ensure_file
from starwhale.utils.load import load_module


def step(
    job_name: str = DEFAULT_EVALUATION_JOB_NAME,
    resources: t.Optional[t.List[str]] = None,
    concurrency: int = 1,
    task_num: int = 1,
    needs: t.Optional[t.List[str]] = None,
) -> t.Any:
    _resources = resources or [
        DEFAULT_EVALUATION_RESOURCE,
    ]
    _needs = needs or []

    def decorator(func: t.Any) -> t.Any:
        if Parser.is_parse_stage():
            cls, delim, func_name = func.__qualname__.rpartition(".")
            _step = dict(
                job_name=job_name,
                step_name=func_name,
                cls_name=cls,
                resources=_resources,
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
        kwargs["context"] = thread_local.context
        return func(*args, **kwargs)

    return wrap_func


# Runtime concept
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

    def __repr__(self) -> str:
        return "step:{}, total:{}, index:{}".format(self.step, self.total, self.index)


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
