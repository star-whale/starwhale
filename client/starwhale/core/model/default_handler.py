from typing import Any
from pathlib import Path

from loguru import logger

from starwhale.utils import console
from starwhale.consts import DefaultYAMLName
from starwhale.utils.load import import_cls
from starwhale.api._impl.job import step
from starwhale.core.job.model import Context
from starwhale.core.model.model import StandaloneModel

_CNTR_WORKDIR = "/opt/starwhale"


def _get_cls(src_dir: Path) -> Any:
    _mp = src_dir / DefaultYAMLName.MODEL
    _model_config = StandaloneModel.load_model_config(_mp)
    _handler = _model_config.run.ppl

    console.print(f"try to import {_handler}@{src_dir}...")
    _cls = import_cls(src_dir, _handler)
    return _cls


@step()
def ppl(context: Context):
    _cls = _get_cls(context.src_dir)
    logger.debug(f"cls path:{context.src_dir}")
    with _cls(context=context) as _obj:
        _obj._starwhale_internal_run_ppl()

    console.print(f":clap: finish run {context.step}-{context.index}: {_obj}")


@step(dependency="ppl")
def cmp(context: Context):
    _cls = _get_cls(context.src_dir)
    with _cls(context=context) as _obj:
        logger.debug("start to cmp")
        _obj._starwhale_internal_run_cmp()

    console.print(f":clap: finish run {context.step}: {_obj}")
