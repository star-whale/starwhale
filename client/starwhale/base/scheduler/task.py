from __future__ import annotations

import typing as t
import asyncio
from pathlib import Path
from functools import wraps

from loguru import logger

from starwhale.consts import RunStatus, DecoratorInjectAttr
from starwhale.utils.load import load_module
from starwhale.utils.error import NoSupportError
from starwhale.base.context import Context

if t.TYPE_CHECKING:
    from .step import Step


class TaskResult:
    def __init__(self, id: int, status: str, exception: t.Optional[Exception] = None):
        self.id = id
        self.status = status
        self.exception = exception

    def __repr__(self) -> str:
        return f"id:{self.id}, status:{self.status}, exception:{self.exception}"


class TaskExecutor:
    def __init__(
        self,
        index: int,
        context: Context,
        workdir: Path,
        step: Step,
    ):
        self.index = index
        self.context = context
        self.workdir = workdir
        self.exception: t.Optional[Exception] = None
        self.step = step
        self.__status = RunStatus.INIT

    def __str__(self) -> str:
        return f"TaskExecutor[{self.index}]: step-{self.step}"

    def __repr__(self) -> str:
        return f"TaskExecutor[{self.index}]: step-{self.step}, workdir-{self.workdir}, context-{self.context}"

    @property
    def status(self) -> str:
        return self.__status

    def _get_internal_func_name(self, func_name: str) -> str:
        if func_name == "ppl":
            return "_starwhale_internal_run_ppl"
        elif func_name == "cmp":
            return "_starwhale_internal_run_cmp"
        else:
            raise RuntimeError(
                f"failed to map func name({func_name}) into PipelineHandler internal func name"
            )

    def _run_in_pipeline_handler_cls(
        self,
        func: t.Callable,
        func_name: str,
    ) -> None:
        from starwhale.api._impl.evaluation import PipelineHandler

        patch_func_map = {
            "ppl": lambda *args, **kwargs: ...,
            "cmp": lambda *args, **kwargs: ...,
        }

        if func_name not in patch_func_map:
            raise RuntimeError(
                f"func_name({func_name}) is not in patch func map:{patch_func_map.keys()}"
            )

        def _pop_self_args_func(f: t.Callable) -> t.Callable:
            @wraps(f)
            def _wrap(*args: t.Any, **kwargs: t.Any) -> t.Any:
                if len(args) >= 1 and isinstance(args[0], PipelineHandler):
                    args = args[1:]
                return f(*args, **kwargs)

            return _wrap

        patch_func_map[func_name] = _pop_self_args_func(func)

        cls_ = type(
            "GenericPipelineHandler",
            (PipelineHandler,),
            patch_func_map,
        )

        handler_func_name = self._get_internal_func_name(func_name)
        with cls_(*self.step.extra_args, **self.step.extra_kwargs) as instance:
            getattr(instance, handler_func_name)()

    def _do_execute(self) -> None:
        from starwhale.api._impl.evaluation import PipelineHandler

        module = load_module(self.step.module_name, self.workdir)
        cls_ = getattr(module, self.step.cls_name, None)

        if cls_ is None:
            func = getattr(module, self.step.func_name)
            if getattr(func, DecoratorInjectAttr.Evaluate, False):
                self._run_in_pipeline_handler_cls(func, "cmp")
            elif getattr(func, DecoratorInjectAttr.Predict, False):
                self._run_in_pipeline_handler_cls(func, "ppl")
            elif getattr(func, DecoratorInjectAttr.Step, False):
                func()
            else:
                raise NoSupportError(
                    f"func({self.step.module_name}.{self.step.func_name}) should use @handler, @predict or @evaluate decorator"
                )
        else:
            # TODO: support user custom class and function with arguments
            if issubclass(cls_, PipelineHandler):
                func_name = self._get_internal_func_name(self.step.func_name)
            else:
                func_name = self.step.func_name

            if hasattr(cls_, "__enter__") and hasattr(cls_, "__exit__"):
                with cls_() as instance:
                    func = getattr(instance, func_name)
                    if getattr(func, DecoratorInjectAttr.Evaluate, False):
                        self._run_in_pipeline_handler_cls(func, "cmp")
                    elif getattr(func, DecoratorInjectAttr.Predict, False):
                        self._run_in_pipeline_handler_cls(func, "ppl")
                    else:
                        func()
            else:
                func = getattr(cls_(), func_name)
                if getattr(func, DecoratorInjectAttr.Evaluate, False):
                    self._run_in_pipeline_handler_cls(func, "cmp")
                elif getattr(func, DecoratorInjectAttr.Predict, False):
                    self._run_in_pipeline_handler_cls(func, "ppl")
                else:
                    func()

    def execute(self) -> TaskResult:
        logger.info(f"start to execute task with context({self.context}) ...")
        try:
            loop = asyncio.get_event_loop()
        except RuntimeError as ex:
            logger.warning("get event loop error, try to new one", ex)
            loop = asyncio.new_event_loop()
        asyncio.set_event_loop(loop)
        try:
            self.__status = RunStatus.RUNNING
            Context.set_runtime_context(self.context)
            self._do_execute()
        except Exception as e:
            logger.exception(e)
            self.exception = e
            self.__status = RunStatus.FAILED
        else:
            self.__status = RunStatus.SUCCESS
        finally:
            logger.info(
                f"finish {self.context}, status:{self.status}, error:{self.exception}"
            )
            return TaskResult(
                id=self.index, status=self.status, exception=self.exception
            )
