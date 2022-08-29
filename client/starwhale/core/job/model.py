from typing import List, Optional
from pathlib import Path

from loguru import logger

from starwhale.utils.load import (
    load_cls,
    load_module,
    get_func_from_module,
    get_func_from_object,
)
from starwhale.api._impl.job import Context


class STATUS:
    INIT = "init"
    START = "start"
    RUNNING = "running"
    SUCCESS = "success"
    FAILED = "failed"


class TaskResult:
    def __init__(self, task_id: int, status: str, exception: Optional[Exception] = None):
        self.task_id = task_id
        self.status = status
        self.exception = exception


class StepResult:
    def __init__(self, step_name: str, task_results: List[TaskResult]):
        self.step_name = step_name
        self.task_results = task_results
        self.status = self._status()

    def _status(self) -> str:
        _success = True
        for tr in self.task_results:
            _success = _success and tr.status == STATUS.SUCCESS
        return STATUS.SUCCESS if _success else STATUS.FAILED


class Task:
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
        self.exception = ""

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
