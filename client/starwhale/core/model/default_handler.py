from pathlib import Path
from typing import Any

from starwhale.api._impl.job import step
from starwhale.consts import DefaultYAMLName
from starwhale.core.job.model import Context
from starwhale.core.model.model import StandaloneModel
from starwhale.utils import console
from starwhale.utils.load import import_cls


class DefaultPipeline:
    @staticmethod
    def _get_cls(workdir: Path) -> Any:
        _mp = workdir / DefaultYAMLName.MODEL
        _model_config = StandaloneModel.load_model_config(_mp)
        _handler = _model_config.run.ppl

        console.print(f"try to import {_handler}@{workdir}...")
        _cls = import_cls(workdir, _handler)
        return _cls

    @step()
    def ppl(self, _context: Context):
        from starwhale.api._impl.model import _RunConfig

        # _RunConfig.set_env(kw)

        _cls = self._get_cls(_context.workdir)

        with _cls() as _obj:
            _obj._starwhale_internal_run_ppl()

        console.print(f":clap: finish run {_context.step}: {_obj}")


    @step(dependency="ppl")
    def cmp(self, _context: Context):
        # from starwhale.api._impl.model import _RunConfig

        _cls = self._get_cls(_context.workdir)

        with _cls() as _obj:
            _obj._starwhale_internal_run_cmp()

        console.print(f":clap: finish run {_context.step}: {_obj}")
