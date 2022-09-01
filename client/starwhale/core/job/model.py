import time
import typing as t
import concurrent.futures
from typing import List, Optional
from pathlib import Path

from loguru import logger
from typing_extensions import Protocol

from starwhale.utils.load import (
    load_cls,
    load_module,
    get_func_from_module,
    get_func_from_object,
)
from starwhale.api._impl.job import Step, Context


class STATUS:
    INIT = "init"
    START = "start"
    RUNNING = "running"
    SUCCESS = "success"
    FAILED = "failed"


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
    ):
        self.index = index
        self.context = context
        self.status = status
        self.module = module
        self.work_dir = workdir
        self.exception: Optional[Exception] = None

    def execute(self) -> TaskResult:
        """
        call function from module
        :return: function results
        """
        logger.debug("execute step:{} start.", self.context)

        _module = load_module(self.module, self.work_dir)

        try:
            # instance method
            if "." in self.context.step:
                logger.debug("hi, use class")
                _cls_name, _func_name = self.context.step.split(".")
                _cls = load_cls(_module, _cls_name)
                # need an instance
                cls = _cls()
                func = get_func_from_object(cls, _func_name)
            else:
                logger.debug("hi, use func")
                _func_name = self.context.step
                func = get_func_from_module(_module, _func_name)

            self.status = STATUS.RUNNING
            # The standard implementation does not return results
            func(context=self.context)

        except Exception as e:
            self.exception = e
            self.status = STATUS.FAILED
        else:
            self.status = STATUS.SUCCESS
        finally:
            logger.debug(
                f"execute step:{self.context}, status:{self.status}, error:{self.exception}"
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
        kw: t.Dict[str, t.Any] = {},
    ) -> None:
        self.step = step
        self.project = project
        self.dataset_uris = dataset_uris
        self.module = module
        self.workdir = workdir
        self.version = version
        self.kw = kw

    def execute(self) -> StepResult:
        logger.debug(f"start execute step:{self.step}")
        processor = MultiThreadProcessor(
            self.step.step_name, self.step.concurrency, self._split_tasks()
        )
        task_results = processor.execute()
        logger.debug(f"finish execute step:{self.step}")
        return StepResult(step_name=self.step.step_name, task_results=task_results)

    def _split_tasks(self) -> t.List[BaseExecutor]:
        return list(
            TaskExecutor(
                index=index,
                context=Context(
                    project=self.project,
                    version=self.version,
                    step=self.step.step_name,
                    total=self.step.task_num,
                    index=index,
                    dataset_uris=self.dataset_uris,
                    workdir=self.workdir,
                    kw=self.kw,
                ),
                status=STATUS.INIT,
                module=self.module,
                workdir=self.workdir,
            )
            for index in range(self.step.task_num)
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
            start_time = time.time()
            futures = [pool.submit(executor.execute) for executor in self.executors]
            _results: t.List[t.Any] = [
                future.result() for future in concurrent.futures.as_completed(futures)
            ]

            exec_time = time.time() - start_time
            logger.debug(f"execute:{self.name} time:{exec_time}")
        return _results
