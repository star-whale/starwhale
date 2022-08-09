from typing import Any
from pathlib import Path

from loguru import logger

from starwhale.utils import console
from starwhale.consts import (
    DefaultYAMLName,
    DEFAULT_INPUT_JSON_FNAME,
)
from starwhale.base.type import RunSubDirType
from starwhale.utils.load import import_cls
from starwhale.api._impl.job import step
from starwhale.core.job.model import Context
from starwhale.core.model.model import StandaloneModel

_CNTR_WORKDIR = "/opt/starwhale"


class DefaultPipeline:
    @staticmethod
    def _get_cls(src_dir: Path) -> Any:
        _mp = src_dir / DefaultYAMLName.MODEL
        _model_config = StandaloneModel.load_model_config(_mp)
        _handler = _model_config.run.ppl

        console.print(f"try to import {_handler}@{src_dir}...")
        _cls = import_cls(src_dir, _handler)
        return _cls

    @step()
    def ppl(self, _context: Context):
        from starwhale.api._impl.model import _RunConfig

        # TODO: need new Dataset and Dataloader, so wo can instance dataloader by task index and total num.
        #  cool! this can be removed form here.
        _RunConfig.set_env(
            {
                "input_config": _context.workdir
                / RunSubDirType.CONFIG
                / DEFAULT_INPUT_JSON_FNAME,
            }
        )
        _cls = self._get_cls(_context.src_dir)
        logger.debug(f"cls path:{_context.src_dir}")
        with _cls(context=_context) as _obj:
            _obj._starwhale_internal_run_ppl()

        console.print(f":clap: finish run {_context.step}-{_context.index}: {_obj}")

    @step(dependency="DefaultPipeline.ppl")
    def cmp(self, _context: Context):
        _cls = self._get_cls(_context.src_dir)
        with _cls(context=_context) as _obj:
            _obj._starwhale_internal_run_cmp()
        console.print(f":clap: finish run {_context.step}: {_obj}")
