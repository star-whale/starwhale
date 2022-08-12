from typing import Any
from pathlib import Path

from loguru import logger

from starwhale.utils import console
from starwhale.consts import DefaultYAMLName
from starwhale.utils.fs import ensure_dir
from starwhale.base.type import RunSubDirType
from starwhale.utils.load import import_cls
from starwhale.api._impl.job import step
from starwhale.core.job.model import Context
from starwhale.core.model.model import StandaloneModel
from starwhale.api._impl.wrapper import EvaluationForSubProcess

_CNTR_WORKDIR = "/opt/starwhale"


def _get_cls(src_dir: Path) -> Any:
    _mp = src_dir / DefaultYAMLName.MODEL
    _model_config = StandaloneModel.load_model_config(_mp)
    _handler = _model_config.run.ppl

    console.print(f"try to import {_handler}@{src_dir}...")
    _cls = import_cls(src_dir, _handler)
    return _cls


def set_up(base_workdir: Path, step: str) -> None:
    from starwhale.api._impl.model import _RunConfig

    _run_dir = base_workdir / step
    ensure_dir(_run_dir)

    for _w in (_run_dir,):
        for _n in (RunSubDirType.STATUS, RunSubDirType.LOG):
            ensure_dir(_w / _n)
    _RunConfig.set_env(
        {
            "status_dir": _run_dir / RunSubDirType.STATUS,
            "log_dir": _run_dir / RunSubDirType.LOG,
        }
    )


@step()
def ppl(context: Context) -> None:
    set_up(context.workdir, context.step)
    # TODO:
    _eval = EvaluationForSubProcess(context.get_param("sub_conn"))
    _cls = _get_cls(context.src_dir)
    with _cls(evaluation=_eval) as _obj:
        _obj._starwhale_internal_run_ppl()

    console.print(f":clap: finish run {context.step}-{context.index}: {_obj}")


@step(dependency="ppl")
def cmp(context: Context) -> None:
    set_up(context.workdir, context.step)
    # TODO:
    _eval = EvaluationForSubProcess(context.get_param("sub_conn"))
    _cls = _get_cls(context.src_dir)
    with _cls(evaluation=_eval) as _obj:
        logger.debug("start to cmp")
        _obj._starwhale_internal_run_cmp()

    console.print(f":clap: finish run {context.step}-{context.index}: {_obj}")
