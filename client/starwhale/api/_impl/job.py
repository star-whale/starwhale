import copy
import typing as t
from pathlib import Path
from collections import defaultdict

import yaml
from loguru import logger

from starwhale.utils import load_yaml
from starwhale.consts import DEFAULT_EVALUATION_JOB_NAME, DEFAULT_EVALUATION_RESOURCE
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
            _step = dict(
                job_name=job_name,
                step_name=func.__qualname__,
                resources=_resources,
                concurrency=concurrency,
                task_num=task_num,
                needs=_needs,
            )
            Parser.add_job(job_name, _step)

        return func

    return decorator


# Runtime concept
class Context:
    def __init__(
        self,
        workdir: Path,
        src_dir: Path,
        step: str = "",
        total: int = 1,
        index: int = 0,
        dataset_uris: t.List[str] = [],
        version: str = "",
        project: str = "",
        kw: t.Dict[str, t.Any] = {},
    ):
        self.project = project
        self.version = version
        self.step = step
        self.total = total
        self.index = index
        self.dataset_uris = dataset_uris
        self.workdir = workdir
        self.src_dir = src_dir
        self.kw = copy.deepcopy(kw)

    def get_param(self, name: str) -> t.Any:
        return self.kw.get(name)

    def put_param(self, name: str, value: t.Any) -> None:
        if not self.kw:
            self.kw = {}
        self.kw.setdefault(name, value)

    def __repr__(self) -> str:
        return "step:{}, total:{}, index:{}".format(self.step, self.total, self.index)


class Step:
    def __init__(
        self,
        job_name: str,
        step_name: str,
        resources: t.List[str],
        needs: t.List[str],
        concurrency: int = 1,
        task_num: int = 1,
        status: str = "",
    ):
        self.job_name = job_name
        self.step_name = step_name
        self.resources = resources
        self.concurrency = concurrency
        self.task_num = task_num
        self.needs = needs
        self.status = status

    def __repr__(self) -> str:
        return (
            "%s(job_name=%r, step_name=%r, resources=%r, needs=%r, concurrency=%r, task_num=%r, status=%r)"
            % (
                self.__class__.__name__,
                self.job_name,
                self.step_name,
                self.resources,
                self.needs,
                self.concurrency,
                self.task_num,
                self.status,
            )
        )


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

    @staticmethod
    def check(jobs: t.Dict[str, t.List[t.Dict]]) -> bool:
        checks = []
        logger.debug(f"jobs:{jobs}")
        for job in jobs.items():
            all_steps = []
            needs = []
            for _step in job[1]:
                all_steps.append(_step["step_name"])
                for d in _step["needs"]:
                    if d:
                        needs.append(d)
            logger.debug(f"all steps:{all_steps}, length:{len(all_steps)}")
            _check = all(item in all_steps for item in needs)
            if not _check:
                logger.error(f"job:{job[0]} check error!")
            checks.append(_check)

        return all(checks)

    @staticmethod
    def parse_job_from_yaml(file_path: str) -> t.Dict[str, t.List[Step]]:
        _jobs = load_yaml(file_path)
        rt = defaultdict(list)
        for k, v in _jobs.items():
            rt[k] = [Step(**_v) for _v in v]
        return rt
