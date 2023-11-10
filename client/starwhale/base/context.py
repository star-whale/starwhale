from __future__ import annotations

import typing as t
import threading
from pathlib import Path
from functools import wraps

from starwhale.utils import console
from starwhale.utils.error import ParameterError
from starwhale.base.uri.project import Project


# TODO: support __setattr__, __getattr__ function for Context
class Context:
    _context_holder = threading.local()

    def __init__(
        self,
        workdir: t.Optional[Path] = None,
        step: str = "",
        total: int = 1,
        index: int = 0,
        dataset_uris: t.Optional[t.List[str]] = None,
        version: str = "",
        run_project: t.Optional[Project] = None,
        log_project: t.Optional[Project] = None,
        dataset_head: int = 0,
        finetune_val_dataset_uris: t.List[str] | None = None,
        model_name: str = "",
    ):
        self.run_project = run_project or Project()
        self.log_project = log_project or self.run_project
        self.version = version
        self.step = step
        self.total = total
        self.index = index
        self.dataset_uris = dataset_uris or []
        self.workdir = workdir or Path(".")
        self.dataset_head = dataset_head
        self.finetune_val_dataset_uris = finetune_val_dataset_uris or []
        self.model_name = model_name

    def __str__(self) -> str:
        return f"step:{self.step}, index:{self.index}/{self.total}"

    def __repr__(self) -> str:
        return f"step:{self.step}, index:{self.index}/{self.total}, version:{self.version}, dataset_uris:{self.dataset_uris}"

    def __eq__(self, other: t.Any) -> bool:
        return isinstance(other, Context) and other.__dict__ == self.__dict__

    @classmethod
    def get_runtime_context(cls) -> Context:
        try:
            val: Context = cls._context_holder.value  # type: ignore
        except AttributeError as e:
            raise RuntimeError(
                "Starwhale does not set Context yet, please check if the get_runtime_context function is used at the right time."
            ) from e

        if not isinstance(val, Context):
            raise RuntimeError(
                f"The value of _context_holder is not Context type: {val}"
            )

        return val

    @classmethod
    def set_runtime_context(cls, ctx: Context) -> None:
        if not isinstance(ctx, Context):
            raise ParameterError(
                f"set_runtime_context function only accepts context: {ctx}"
            )

        try:
            val: Context = cls._context_holder.value  # type: ignore
            if val and isinstance(val, Context):
                # TODO: _context_holder set only once?
                cls._context_holder.value = ctx
                console.warning(f"runtime context has already be set: {val}")
        except AttributeError:
            cls._context_holder.value = ctx


def pass_context(func: t.Any) -> t.Any:
    @wraps(func)
    def wrap_func(*args: t.Any, **kwargs: t.Any) -> t.Any:
        kwargs["context"] = Context.get_runtime_context()
        return func(*args, **kwargs)

    return wrap_func
