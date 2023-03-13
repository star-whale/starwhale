from typing import Any
from pathlib import Path

from loguru import logger

from starwhale import step, Context, pass_context, PipelineHandler
from starwhale.utils import console
from starwhale.consts import DefaultYAMLName
from starwhale.utils.load import import_object, get_func_from_object
from starwhale.core.model.model import StandaloneModel


def _get_cls(src_dir: Path) -> Any:
    mp = src_dir / DefaultYAMLName.MODEL
    model_config = StandaloneModel.load_model_config(mp, src_dir)
    handler_path = model_config.run.handler

    _cls = import_object(src_dir, handler_path)
    if not issubclass(_cls, PipelineHandler):
        raise RuntimeError(f"{handler_path} is not subclass of PipelineHandler")
    return _cls


def _invoke(context: Context, func: str) -> None:
    _cls = _get_cls(context.workdir)
    console.print(f":zap: start to run {context.step}-{context.index}...")
    logger.debug(f"-->[Running] start to run {context.step}-{context.index} .")
    with _cls() as _obj:
        _func = get_func_from_object(_obj, func)
        _func()

    console.print(
        f":clap: finished run {context.step}-{context.index}, more details can see:{_obj}"
    )

    logger.debug(f"-->[Finished] finished run {context.step}-{context.index} .")


@step(task_num=2, concurrency=2)
@pass_context
def ppl(context: Context) -> None:
    _invoke(context=context, func="_starwhale_internal_run_ppl")


@step(needs=["ppl"])
@pass_context
def cmp(context: Context) -> None:
    _invoke(context=context, func="_starwhale_internal_run_cmp")
