import typing as t
from pathlib import Path
from multiprocessing import Pipe, connection

import yaml
from loguru import logger

from starwhale.core.job.loader import (
    load_cls,
    load_module,
    get_func_from_module,
    get_func_from_instance,
)


class Step:
    def __init__(
        self,
        job_name: str,
        step_name: str,
        resources: str = "cpu=1",
        concurrency: int = 1,
        task_num: int = 1,
        dependency: str = "",
    ):
        self.job_name = job_name
        self.step_name = step_name
        self.resources = resources.strip().split(",")
        self.concurrency = concurrency
        self.task_num = task_num
        self.dependency = dependency.strip().split(",")
        self.status = ""
        self.tasks: t.List[Task] = []

    def __repr__(self) -> str:
        return "step_name:{0}, dependency:{1}, status: {2}".format(
            self.step_name, self.dependency, self.status
        )

    def gen_task(
        self,
        index: int,
        module: str,
        workdir: Path,
        src_dir: Path,
        dataset_uris: t.List[str],
        version: str,
        project: str,
        **kw: t.Any,
    ) -> None:
        self.tasks.append(
            Task(
                context=Context(
                    project=project,
                    # todo id or version
                    version=version,
                    step=self.step_name,
                    total=self.task_num,
                    index=index,
                    dataset_uris=dataset_uris,
                    workdir=workdir,
                    src_dir=src_dir,
                    **kw,
                ),
                status=STATUS.INIT,
                module=module,
                src_dir=src_dir,
            )
        )


class ParseConfig:
    def __init__(self, is_parse_stage: bool, jobs: t.Dict[str, t.List[Step]]):
        self.parse_stage = is_parse_stage
        self.jobs = jobs

    def clear(self) -> None:
        self.jobs = {}


# shared memory, not thread safe
# parse_config = {"parse_stage": False, "jobs": {}}
parse_config = ParseConfig(False, {})


class Parser:
    @staticmethod
    def set_parse_stage(parse_stage: bool) -> None:
        parse_config.parse_stage = parse_stage

    @staticmethod
    def is_parse_stage() -> bool:
        return parse_config.parse_stage

    @staticmethod
    def add_job(job_name: str, step: Step) -> None:
        _jobs = parse_config.jobs
        if job_name not in _jobs:
            parse_config.jobs[job_name] = []

        parse_config.jobs[job_name].append(step)

    @staticmethod
    def get_jobs() -> t.Dict[str, t.List[Step]]:
        return parse_config.jobs

    # load is unique,so don't need to think multi load and clean
    @staticmethod
    def clear_config() -> None:
        global parse_config
        parse_config.clear()

    @staticmethod
    def parse_job_from_module(module: str, path: Path) -> t.Dict[str, t.List[Step]]:
        """
        parse @step from module
        :param module: module name
        :param path: abs path
        :return: jobs
        """
        Parser.set_parse_stage(True)
        # parse DAG
        logger.debug("parse @step for module:{}", module)
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
            # ensure_file(target_file, yaml.safe_dump(_jobs, default_flow_style=False))
            with open(target_file, "w") as file:
                yaml.dump(_jobs, file)
            logger.debug("generator DAG success!")
        else:
            logger.error("generator DAG error! reason:{}", "check is failed.")

    @staticmethod
    def check(jobs: t.Dict[str, t.List[Step]]) -> bool:
        # check
        checks = []
        for job in jobs.items():
            all_steps = []
            dependencies = []
            for step in job[1]:
                all_steps.append(step.step_name)
                for d in step.dependency:
                    if d:
                        dependencies.append(d)
            logger.debug("all steps:{},{}", all_steps[0], len(all_steps))
            _check = all(item in all_steps for item in dependencies)
            if not _check:
                logger.error("job:{} check error!", job[0])
            checks.append(_check)
        # all is ok
        if all(c is True for c in checks):
            logger.debug("check success! \n{}", yaml.dump(jobs))
            return True
        else:
            return False

    @staticmethod
    def parse_job_from_yaml(file_path: str) -> t.Any:
        with open(file_path, "r") as file:
            return yaml.unsafe_load(file)


# Runtime concept
class Context:
    def __init__(
        self,
        workdir: Path,
        src_dir: Path,
        step: str = "",
        total: int = 1,
        index: int = 0,
        dataset_uris: t.Optional[t.List[str]] = None,
        version: str = "",
        project: str = "",
        **kw: t.Any,
    ):
        self.project = project
        self.version = version
        self.step = step
        self.total = total
        self.index = index
        self.dataset_uris = dataset_uris
        self.workdir = workdir
        self.src_dir = src_dir
        self.kw = kw

    def get_param(self, name: str) -> t.Any:
        return self.kw.get(name)

    def put_param(self, name: str, value: t.Any) -> None:
        if not self.kw:
            self.kw = {}
        self.kw.setdefault(name, value)

    def __repr__(self) -> str:
        return "step:{}, total:{}, index:{}".format(self.step, self.total, self.index)


class STATUS:
    INIT = "init"
    RUNNING = "running"
    SUCCESS = "success"
    FAILED = "failed"


class Task:
    def __init__(self, context: Context, status: str, module: str, src_dir: Path):
        self.context = context
        self.status = status
        self.module = module
        self.src_dir = src_dir
        self._pipe = Pipe(True)
        self.main_conn: connection.Connection = self._pipe[0]
        self.sub_conn: connection.Connection = self._pipe[1]
        self.context.put_param("sub_conn", self.sub_conn)

    def execute(self) -> bool:
        """
        call function from module
        :return: function results
        """
        logger.debug("execute step:{} start.", self.context)

        _module = load_module(self.module, self.src_dir)

        try:
            # instance method
            if "." in self.context.step:
                logger.debug("hi, use class")
                _cls_name, _func_name = self.context.step.split(".")
                _cls = load_cls(_module, _cls_name)
                # need an instance
                cls = _cls()
                func = get_func_from_instance(cls, _func_name)
            else:
                logger.debug("hi, use func")
                _func_name = self.context.step
                func = get_func_from_module(_module, _func_name)

            self.status = STATUS.RUNNING
            # The standard implementation does not return results
            func(context=self.context)

        except Exception as e:
            self.status = STATUS.FAILED
            logger.error("execute step:{} error, {}", self.context, e)
            return False
        else:
            self.status = STATUS.SUCCESS
            logger.debug("execute step:{} success", self.context)
            return True
