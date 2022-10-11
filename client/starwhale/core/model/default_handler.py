from typing import Any
from pathlib import Path

from loguru import logger

from starwhale import step, Context, pass_context, PipelineHandler
from starwhale.utils import console
from starwhale.consts import DefaultYAMLName
from starwhale.utils.load import import_cls, get_func_from_object
from starwhale.core.model.model import StandaloneModel


def _get_cls(src_dir: Path) -> Any:
    _mp = src_dir / DefaultYAMLName.MODEL
    _model_config = StandaloneModel.load_model_config(_mp)
    _handler = _model_config.run.handler

    logger.debug(f"try to import {_handler}@{src_dir}...")
    _cls = import_cls(src_dir, _handler, PipelineHandler)
    return _cls


def _invoke(context: Context, func: str) -> None:
    logger.debug(f"workdir : {context.workdir}")
    _cls = _get_cls(context.workdir)
    console.print(f":zap: start run {context.step}-{context.index}...")
    with _cls() as _obj:
        _func = get_func_from_object(_obj, func)
        _func()

    console.print(
        f":clap: finish run {context.step}-{context.index}, more details can see:{_obj}"
    )


@step()
@pass_context
def ppl(context: Context) -> None:
    _invoke(context=context, func="_starwhale_internal_run_ppl")


@step(needs=["ppl"])
@pass_context
def cmp(context: Context) -> None:
    _invoke(context=context, func="_starwhale_internal_run_cmp")
