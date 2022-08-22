from typing import Any
from pathlib import Path

from loguru import logger

from starwhale.utils import console
from starwhale.consts import DefaultYAMLName
from starwhale.utils.load import import_cls
from starwhale.api._impl.job import step, Context
from starwhale.core.model.model import StandaloneModel

_CNTR_WORKDIR = "/opt/starwhale"


def _get_cls(src_dir: Path) -> Any:
    _mp = src_dir / DefaultYAMLName.MODEL
    _model_config = StandaloneModel.load_model_config(_mp)
    _handler = _model_config.run.ppl

    console.print(f"try to import {_handler}@{src_dir}...")
    _cls = import_cls(src_dir, _handler)
    return _cls


@step(concurrency=3, task_num=6)
def ppl(context: Context) -> None:
    logger.debug(f"src : {context.src_dir}")
    _cls = _get_cls(context.src_dir)
    with _cls(context=context) as _obj:
        _obj._starwhale_internal_run_ppl()

    console.print(f":clap: finish run {context.step}-{context.index}: {_obj}")


@step(needs=["ppl"])
def cmp(context: Context) -> None:
    _cls = _get_cls(context.src_dir)
    with _cls(context=context) as _obj:
        _obj._starwhale_internal_run_cmp()

    console.print(f":clap: finish run {context.step}-{context.index}: {_obj}")
