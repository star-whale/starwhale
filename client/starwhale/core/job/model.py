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


class Task:
    def __init__(
        self,
        context: Context,
        status: str,
        module: str,
        workdir: Path,
    ):
        self.context = context
        self.status = status
        self.module = module
        self.work_dir = workdir

    def execute(self) -> bool:
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
            self.status = STATUS.FAILED
            logger.error(f"execute step:{self.context}, error:{e}")
            return False
        else:
            self.status = STATUS.SUCCESS
            logger.debug(f"execute step:{self.context} success")
            return True
