import json
from typing import Any
from pathlib import Path

from starwhale.utils import console
from starwhale.consts import (
    JSON_INDENT,
    CURRENT_FNAME,
    DataLoaderKind,
    DefaultYAMLName,
    SWDSBackendType,
    DEFAULT_INPUT_JSON_FNAME,
)
from starwhale.utils.fs import ensure_dir, ensure_file
from starwhale.base.type import RunSubDirType, URIType
from starwhale.utils.load import import_cls
from starwhale.api._impl.job import step
from starwhale.core.job.model import Context
from starwhale.core.model.model import StandaloneModel

_CNTR_WORKDIR = "/opt/starwhale"


def _gen_jsonl_fuse_json(_context: Context) -> Path:
    # if self.use_docker:
    #     _base_dir = f"{_CNTR_WORKDIR}/{RunSubDirType.PPL_RESULT}"
    # else:
    # TODO :replace by datastore
    _base_dir = str(_context.workdir / "ppl" / RunSubDirType.RESULT)
    _fuse = dict(
        backend=SWDSBackendType.FUSE,
        kind=DataLoaderKind.JSONL,
        swds=[
            dict(
                bucket=_base_dir,
                key=dict(
                    data=CURRENT_FNAME,
                ),
            )
        ],
    )
    _f = _context.workdir / "cmp" / RunSubDirType.CONFIG / DEFAULT_INPUT_JSON_FNAME
    ensure_file(_f, json.dumps(_fuse, indent=JSON_INDENT))
    return _f


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
                "input_config": _context.workdir / RunSubDirType.CONFIG / DEFAULT_INPUT_JSON_FNAME,
            }
        )
        _cls = self._get_cls(_context.src_dir)
        with _cls() as _obj:
            _obj._starwhale_internal_run_ppl()

        console.print(f":clap: finish run {_context.step}-{_context.index}: {_obj}")

    @step(dependency="DefaultPipeline.ppl")
    def cmp(self, _context: Context):
        # ensure cmp dir
        from starwhale.api._impl.model import _RunConfig

        # todo
        _RunConfig.set_env(
            {
                "input_config": _context.workdir / "cmp" / RunSubDirType.CONFIG / DEFAULT_INPUT_JSON_FNAME,
            }
        )

        # TODO: generate input json for the time being, to be replaced by new dataset
        _gen_jsonl_fuse_json(_context)

        _cls = self._get_cls(_context.src_dir)

        with _cls() as _obj:
            _obj._starwhale_internal_run_cmp()

        console.print(f":clap: finish run {_context.step}: {_obj}")
