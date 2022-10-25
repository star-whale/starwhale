import typing as t
import concurrent.futures
from typing import List, Optional
from pathlib import Path
from collections import defaultdict

from loguru import logger
from typing_extensions import Protocol

from starwhale import Context
from starwhale.utils import load_yaml
from starwhale.utils.load import (
    load_cls,
    load_module,
    get_func_from_module,
    get_func_from_object,
)
from starwhale.core.job.dag import DAG, generate_dag
from starwhale.api._impl.job import context_holder


class STATUS:
    INIT = "init"
    START = "start"
    RUNNING = "running"
    SUCCESS = "success"
    FAILED = "failed"


class Step:
    def __init__(
        self,
        job_name: str,
        step_name: str,
        resources: t.List[t.Dict],
        needs: t.List[str],
        concurrency: int = 1,
        task_num: int = 1,
        status: str = "",
        cls_name: str = "",
        **kw: t.Any,
    ):
        self.job_name = job_name
        self.cls_name = cls_name
        self.step_name = step_name
        self.resources = resources
        self.concurrency = concurrency
        self.task_num = task_num
        self.needs = needs
        self.status = status
        self.kw = kw

    def __repr__(self) -> str:
        return (
            "%s(job_name=%r, cls_name=%r, step_name=%r, resources=%r, needs=%r, concurrency=%r, task_num=%r, status=%r)"
            % (
                self.__class__.__name__,
                self.job_name,
                self.cls_name,
                self.step_name,
                self.resources,
                self.needs,
                self.concurrency,
                self.task_num,
                self.status,
            )
        )


class Generator:
    @staticmethod
    def generate_job_from_yaml(file_path: str) -> t.Dict[str, t.List[Step]]:
        _jobs = load_yaml(file_path)
        rt = defaultdict(list)
        for k, v in _jobs.items():
            rt[k] = [Step(**_v) for _v in v]
        return rt

    @staticmethod
    def generate_dag_from_steps(steps: t.List[Step]) -> DAG:
        _vertices: t.List[str] = []
        _edges: t.Dict[str, str] = {}
        for step in steps:
            _vertices.append(step.step_name)
            if not step.needs:
                continue
            for _pre in step.needs:
                _edges[_pre] = step.step_name

        return generate_dag(_vertices, _edges)


class TaskResult:
    def __init__(
        self, task_id: int, status: str, exception: Optional[Exception] = None
    ):
        self.task_id = task_id
        self.status = status
        self.exception = exception

    def __repr__(self) -> str:
        return f"id:{self.task_id}, status:{self.status}, exception:{self.exception}"


class StepResult:
    def __init__(self, step_name: str, task_results: List[TaskResult]):
        self.step_name = step_name
        self.task_results = task_results
        self.status = self._status()

    def _status(self) -> str:
        return (
            STATUS.SUCCESS
            if all(tr.status == STATUS.SUCCESS for tr in self.task_results)
            else STATUS.FAILED
        )

    def __repr__(self) -> str:
        return f"step:{self.step_name}, taskResults:{self.task_results}, status:{self.status}"


class BaseExecutor(Protocol):
    def execute(self) -> t.Any:
        ...


class TaskExecutor:
    def __init__(
        self,
        index: int,
        context: Context,
        status: str,
        module: str,
        workdir: Path,
        func: str = "",
        cls_name: str = "",
    ):
        self.index = index
        self.context = context
        self.status = status
        self.cls_name = cls_name
        self.func = func
        self.module = module
        self.work_dir = workdir
        self.exception: Optional[Exception] = None

    def execute(self) -> TaskResult:
        logger.info(f"start to execute {self.context} ...")

        _module = load_module(self.module, self.work_dir)

        try:
            self.status = STATUS.RUNNING
            context_holder.context = self.context
            # instance method
            if not self.cls_name:
                func = get_func_from_module(_module, self.func)
                # The standard implementation does not return results
                func()
            else:
                _cls = load_cls(_module, self.cls_name)
                # need an instance
                func = get_func_from_object(_cls(), self.func)
                # The standard implementation does not return results
                func()
        except Exception as e:
            logger.exception(e)
            self.exception = e
            self.status = STATUS.FAILED
        else:
            self.status = STATUS.SUCCESS
        finally:
            logger.info(
                f"finish {self.context}, status:{self.status}, error:{self.exception}"
            )
            return TaskResult(
                task_id=self.index, status=self.status, exception=self.exception
            )


class StepExecutor:
    def __init__(
        self,
        step: Step,
        project: str,
        version: str,
        module: str,
        workdir: Path,
        dataset_uris: t.List[str],
        task_num: int = 0,
    ) -> None:
        self.step = step
        self.task_num = step.task_num if task_num <= 0 else task_num
        self.project = project
        self.dataset_uris = dataset_uris
        self.module = module
        self.workdir = workdir
        self.version = version

    def execute(self) -> StepResult:
        logger.info(f"start to execute step:{self.step}")
        processor = MultiThreadProcessor(
            self.step.step_name, self.step.concurrency, self._split_tasks()
        )
        task_results = processor.execute()
        logger.info(f"finish to execute step:{self.step}")
        return StepResult(step_name=self.step.step_name, task_results=task_results)

    def _split_tasks(self) -> t.List[BaseExecutor]:
        return list(
            TaskExecutor(
                index=index,
                context=Context(
                    project=self.project,
                    version=self.version,
                    step=self.step.step_name,
                    total=self.task_num,
                    index=index,
                    dataset_uris=self.dataset_uris,
                    workdir=self.workdir,
                ),
                status=STATUS.INIT,
                module=self.module,
                func=self.step.step_name,
                cls_name=self.step.cls_name,
                workdir=self.workdir,
            )
            for index in range(self.task_num)
        )


class MultiThreadProcessor:
    def __init__(
        self,
        name: str,
        concurrency: int,
        executors: t.List[BaseExecutor],
    ) -> None:
        self.name = name
        self.concurrency = concurrency
        self.executors = executors

    def execute(self) -> t.List[t.Any]:
        with concurrent.futures.ThreadPoolExecutor(
            max_workers=self.concurrency
        ) as pool:
            futures = [pool.submit(executor.execute) for executor in self.executors]
            _results: t.List[t.Any] = [
                future.result() for future in concurrent.futures.as_completed(futures)
            ]
        return _results
