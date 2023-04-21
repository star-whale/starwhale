import typing as t
from pathlib import Path

from starwhale.consts import RECOVER_DIRNAME, VERSION_PREFIX_CNT, DEFAULT_MANIFEST_NAME
from starwhale.utils.fs import guess_real_path
from starwhale.base.type import URIType
from starwhale.base.store import BaseStorage
from starwhale.utils.config import SWCliConfigMixed
from starwhale.base.uricomponents.project import Project


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
        name = self.uri.version
        _p, _v, _ok = guess_real_path(
            self.project_dir / URIType.JOB / name[:VERSION_PREFIX_CNT], name
        )
        return _p, _v

    @property
    def uri_type(self) -> str:
        return URIType.JOB

    @property
    def manifest_path(self) -> Path:
        return self.loc / DEFAULT_MANIFEST_NAME

    @staticmethod
    def iter_all_jobs(
        project_uri: Project,
    ) -> t.Generator[t.Tuple[Path, bool], None, None]:
        sw = SWCliConfigMixed()
        _job_dir = sw.rootdir / project_uri.name / URIType.JOB
        for _path in _job_dir.glob(f"**/**/{DEFAULT_MANIFEST_NAME}"):
            yield _path, RECOVER_DIRNAME in _path.parts

    @staticmethod
    def local_run_dir(project_name: str, version: str) -> Path:
        # TODO: tune SWCliConfigMixed
        sw = SWCliConfigMixed()
        return (
            sw.rootdir
            / project_name
            / URIType.JOB
            / version[:VERSION_PREFIX_CNT]
            / version
        )
