import json
from typing import Any
from pathlib import Path

from loguru import logger

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
from starwhale.base.type import RunSubDirType
from starwhale.utils.load import import_cls
from starwhale.api._impl.job import step
from starwhale.core.job.model import Context
from starwhale.core.model.model import StandaloneModel
from starwhale.core.dataset.model import Dataset
from starwhale.core.dataset.store import DatasetStorage

_CNTR_WORKDIR = "/opt/starwhale"


def _gen_swds_fuse_json(_context: Context) -> Path:
    _fuse_jsons = []
    for _uri in _context.dataset_uris:
        _store = DatasetStorage(_uri)
        fname = Dataset.render_fuse_json(_store.loc, force=False)
        _fuse_jsons.append(fname)
        logger.debug(f"[gen fuse input.json]{fname}")

    _base = json.load(open(_fuse_jsons[0], "r"))
    # TODO: by task num
    for _f in _fuse_jsons[1:]:
        _config = json.load(open(_f, "r"))
        _base["swds"].extend(_config["swds"])

    # TODO: for docker
    # if self.use_docker:
    #     _bucket = f"{_CNTR_WORKDIR}/{RunSubDirType.DATASET}"
    #     for i in range(len(_base["swds"])):
    #         _base["swds"][i]["bucket"] = _bucket

    _json_f: Path = (
        _context.workdir / "ppl" / RunSubDirType.CONFIG / DEFAULT_INPUT_JSON_FNAME
    )
    ensure_file(_json_f, json.dumps(_base, indent=JSON_INDENT))
    return _json_f


def _gen_jsonl_fuse_json(_context: Context) -> Path:
    # if self.use_docker:
    #     _base_dir = f"{_CNTR_WORKDIR}/{RunSubDirType.PPL_RESULT}"
    # else:
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


def _set_env(dir: Path):
    from starwhale.api._impl.model import _RunConfig

    _RunConfig.set_env(
        {
            "status_dir": dir / RunSubDirType.STATUS,
            "log_dir": dir / RunSubDirType.LOG,
            "result_dir": dir / RunSubDirType.RESULT,
            "input_config": dir / RunSubDirType.CONFIG / DEFAULT_INPUT_JSON_FNAME,
        }
    )


def gen_run_dir(workdir: Path):
    for _w in (workdir,):
        for _n in (
            RunSubDirType.RESULT,
            RunSubDirType.DATASET,
            RunSubDirType.PPL_RESULT,
            RunSubDirType.STATUS,
            RunSubDirType.LOG,
            RunSubDirType.SWMP,
            RunSubDirType.CONFIG,
        ):
            ensure_dir(_w / _n)
    _set_env(workdir)
    logger.info(f"[step:prepare]eval workdir: {workdir}")


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

        # ensure ppl dir

        gen_run_dir(_context.workdir / "ppl")

        # TODO: generate input json for the time being, to be replaced by new dataset uri
        _gen_swds_fuse_json(_context)

        _cls = self._get_cls(_context.src_dir)

        with _cls() as _obj:
            _obj._starwhale_internal_run_ppl()

        console.print(f":clap: finish run {_context.step}: {_obj}")

    @step(dependency="DefaultPipeline.ppl")
    def cmp(self, _context: Context):
        # ensure cmp dir
        gen_run_dir(_context.workdir / "cmp")

        # TODO: generate input json for the time being, to be replaced by new dataset
        _gen_jsonl_fuse_json(_context)

        _cls = self._get_cls(_context.src_dir)

        with _cls() as _obj:
            _obj._starwhale_internal_run_cmp()

        console.print(f":clap: finish run {_context.step}: {_obj}")
