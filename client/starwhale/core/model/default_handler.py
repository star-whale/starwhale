from typing import Any
from pathlib import Path

from loguru import logger

from starwhale.utils import console
from starwhale.consts import DefaultYAMLName
from starwhale.utils.fs import ensure_dir
from starwhale.base.type import RunSubDirType
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


def setup(context: Context) -> None:
    _run_dir = context.workdir / context.step / str(context.index)
    ensure_dir(_run_dir)

    for _w in (_run_dir,):
        for _n in (RunSubDirType.STATUS, RunSubDirType.LOG):
            ensure_dir(_w / _n)
    context.kw["status_dir"] = _run_dir / RunSubDirType.STATUS
    context.kw["log_dir"] = _run_dir / RunSubDirType.LOG


@step(concurrency=3, task_num=6)
def ppl(context: Context) -> None:
    setup(context)
    logger.debug(f"src : {context.src_dir}")
    _cls = _get_cls(context.src_dir)
    with _cls(context=context) as _obj:
        _obj._starwhale_internal_run_ppl()

    console.print(f":clap: finish run {context.step}-{context.index}: {_obj}")


@step(dependency="ppl")
def cmp(context: Context) -> None:
    setup(context)
    _cls = _get_cls(context.src_dir)
    with _cls(context=context) as _obj:
        _obj._starwhale_internal_run_cmp()

    console.print(f":clap: finish run {context.step}-{context.index}: {_obj}")
