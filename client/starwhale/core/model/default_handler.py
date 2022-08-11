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


class DefaultPipeline:
    @staticmethod
    def _get_cls(src_dir: Path) -> Any:
        _mp = src_dir / DefaultYAMLName.MODEL
        _model_config = StandaloneModel.load_model_config(_mp)
        _handler = _model_config.run.ppl

        console.print(f"try to import {_handler}@{src_dir}...")
        _cls = import_cls(src_dir, _handler)
        return _cls

    @step(concurrency=1, task_num=1)
    def ppl(self, _context: Context):
        _cls = self._get_cls(_context.src_dir)
        logger.debug(f"cls path:{_context.src_dir}")
        with _cls(context=_context) as _obj:
            _obj._starwhale_internal_run_ppl()

        console.print(f":clap: finish run {_context.step}-{_context.index}: {_obj}")

    @step(dependency="DefaultPipeline.ppl")
    def cmp(self, _context: Context):
        _cls = self._get_cls(_context.src_dir)
        with _cls(context=_context) as _obj:
            logger.debug("start to cmp")
            _obj._starwhale_internal_run_cmp()

        console.print(f":clap: finish run {_context.step}: {_obj}")
