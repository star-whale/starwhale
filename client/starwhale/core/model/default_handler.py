import math
from typing import Any, Tuple
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
    from starwhale.api._impl.model import _RunConfig

    _run_dir = context.workdir / context.step
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


def calculate_index(data_size: int, task_num: int, task_index: int) -> Tuple[int, int]:
    _batch_size = 1
    if data_size > task_num:
        _batch_size = math.ceil(data_size / task_num)
    _start_index = min(_batch_size * task_index, data_size - 1)
    _end_index = min(_batch_size * (task_index + 1), data_size - 1)
    return _start_index, _end_index


@step()
def ppl(context: Context) -> None:
    from starwhale.api._impl.model import _RunConfig

    # TODO: use dataset.size() and support multi dataset uris
    _dataset_uri = ""
    if context.dataset_uris:
        _dataset_uri = context.dataset_uris[0]
    dataset_row_start, dataset_row_end = calculate_index(
        200, context.total, context.index
    )

    _RunConfig.set_env(
        {
            "dataset_uri": _dataset_uri,
            "dataset_row_start": dataset_row_start,
            "dataset_row_end": dataset_row_end,
        }
    )
    # TODO: some env can be replaced by user param
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
        logger.debug("start to cmp")
        _obj._starwhale_internal_run_cmp()

    console.print(f":clap: finish run {context.step}-{context.index}: {_obj}")
