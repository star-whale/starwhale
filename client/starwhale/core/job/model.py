from pathlib import Path

import yaml
from loguru import logger

from starwhale.base.uri import URI
from starwhale.core.job.loader import load_module


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
        self.tasks = []

    def __repr__(self):
        return "step_name:{0}, dependency:{1}, status: {2}".format(
            self.step_name, self.dependency, self.status
        )

    def gen_task(self, index: int, module: str, workdir: Path, src_dir: Path, dataset_uris: list[URI]):
        self.tasks.append(
            Task(
                context=Context(
                    step=self.step_name,
                    total=self.task_num,
                    index=index,
                    dataset_uris=dataset_uris,
                    workdir=workdir,
                    src_dir=src_dir,
                ),
                status=STATUS.INIT,
                module=module,
                src_dir=src_dir,
            )
        )


# shared memory, not thread safe
parse_config = {"parse_stage": False, "jobs": {}}


class Parser:
    @staticmethod
    def set_parse_stage(parse_stage: bool):
        parse_config["parse_stage"] = parse_stage

    @staticmethod
    def is_parse_stage():
        return parse_config["parse_stage"]

    @staticmethod
    def add_job(job_name: str, step: Step) -> None:
        parse_config["jobs"].setdefault(job_name, {})

        logger.debug(step)
        parse_config["jobs"][job_name][step.step_name] = step

    @staticmethod
    def get_jobs():
        return parse_config["jobs"]

    # load is unique,so don't need to think multi load and clean
    @staticmethod
    def clear_config():
        global parse_config
        parse_config = {"parse_stage": False, "jobs": {}}

    @staticmethod
    def parse_job_from_module(module: str, path: Path):
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
    def check(jobs: dict[str, dict[str, Step]]) -> bool:
        # check
        checks = []
        for job in jobs.items():
            all_steps = []
            dependencies = []
            for item in job[1].items():
                step = item[1]
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
    def parse_job_from_yaml(file: str) -> dict:
        with open(file, "r") as file:
            return yaml.unsafe_load(file)


# Runtime concept
class Context:
    def __init__(
        self,
        workdir: Path,
        src_dir: Path,
        step: str = "",
        total: int = 0,
        index: int = 0,
        dataset_uris: list[URI] = None,
    ):
        self.step = step
        self.total = total
        self.index = index
        self.dataset_uris = dataset_uris
        self.workdir = workdir
        self.src_dir = src_dir

    def __repr__(self):
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

    def execute(self) -> bool:
        """
        call function from module
        :return: function results
        """
        logger.debug("execute step:{} start.", self.context)

        _module = load_module(self.module, self.src_dir)

        # instance method
        if "." in self.context.step:
            _cls_name, _func_name = self.context.step.split(".")
            _cls = getattr(_module, _cls_name, None)
            # need an instance(todo whether it's a staticmethod?)
            cls = _cls()
            func = getattr(cls, _func_name, None)
        else:
            _func_name = self.context.step
            func = getattr(_module, _func_name, None)

        try:
            self.status = STATUS.RUNNING
            # The standard implementation does not return results
            func(self.context)
        # except RuntimeError as e:
        #     self.status = STATUS.FAILED
        #     logger.error("execute step:{} error, {}", self.context, e)
        #     return False
        except Exception as e:
            self.status = STATUS.FAILED
            logger.error("execute step:{} error, {}", self.context, e)
            return False
        else:
            self.status = STATUS.SUCCESS
            logger.debug("execute step:{} success", self.context)
            return True
