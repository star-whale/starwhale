import typing as t
from pathlib import Path

from starwhale.base.type import EvalTaskType, URIType, RunSubDirType
from starwhale.utils.config import SWCliConfigMixed
from starwhale.utils.fs import guess_real_path
from starwhale.consts import (
    DEFAULT_MANIFEST_NAME,
    VERSION_PREFIX_CNT,
    CURRENT_FNAME,
    RECOVER_DIRNAME,
)
from starwhale.base.uri import URI
from starwhale.base.store import BaseStorage


class JobStorage(BaseStorage):
    @property
    def recover_loc(self) -> Path:
        return (
            self.project_dir
            / URIType.JOB
            / RECOVER_DIRNAME
            / self.id[:VERSION_PREFIX_CNT]
            / self.id
        )

    def _guess(self) -> t.Tuple[Path, str]:
        name = self.uri.object.name
        return guess_real_path(
            self.project_dir / URIType.JOB / name[:VERSION_PREFIX_CNT], name
        )

    @property
    def eval_report_path(self) -> Path:
        return self.cmp_dir / RunSubDirType.RESULT / CURRENT_FNAME

    @property
    def ppl_dir(self) -> Path:
        return self.loc / EvalTaskType.PPL

    @property
    def cmp_dir(self) -> Path:
        return self.loc / EvalTaskType.CMP

    @staticmethod
    def iter_all_jobs(project_uri: URI) -> t.Generator[Path, None, None]:
        # TODO: tune SWCliConfigMixed
        sw = SWCliConfigMixed()
        _job_dir = sw.rootdir / project_uri.project / URIType.JOB
        for _mf in _job_dir.glob(f"**/**/{DEFAULT_MANIFEST_NAME}"):
            yield _mf
