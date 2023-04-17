from pathlib import Path

from pyfakefs.fake_filesystem_unittest import TestCase

from starwhale.mngt import gc, _get_workdir_path
from starwhale.utils import config as sw_config
from starwhale.consts import RECOVER_DIRNAME, DEFAULT_MANIFEST_NAME
from starwhale.utils.fs import ensure_dir, ensure_file
from starwhale.base.type import URIType
from starwhale.utils.config import SWCliConfigMixed


class GCTestCase(TestCase):
    def setUp(self) -> None:
        self.setUpPyfakefs()
        sw_config._config = {}

    def test_gc(self) -> None:
        sw = SWCliConfigMixed()
        project_dir = sw.rootdir / "self"
        saved_model_path = (
            project_dir
            / URIType.MODEL
            / "mnist"
            / "mm"
            / "mm4wmmrrgazwknrtmftdgyjzmfwxczi.swmp"
        )
        model_path = (
            project_dir
            / URIType.MODEL
            / RECOVER_DIRNAME
            / "mnist"
            / "gq"
            / "gq4wmmrrgazwknrtmftdgyjzmfwxczi.swmp"
        )
        model_workdir_path = (
            project_dir
            / "workdir"
            / URIType.MODEL
            / RECOVER_DIRNAME
            / "mnist"
            / "gq"
            / "gq4wmmrrgazwknrtmftdgyjzmfwxczi"
        )
        ensure_dir(saved_model_path.parent)
        ensure_file(saved_model_path, " ")
        ensure_dir(model_path.parent)
        ensure_file(model_path, " ")
        ensure_dir(model_workdir_path)
        ensure_file(model_workdir_path / DEFAULT_MANIFEST_NAME, " ")

        dataset_path = (
            project_dir
            / URIType.DATASET
            / RECOVER_DIRNAME
            / "mnist"
            / "me"
            / "me4dczlegzswgnrtmftdgyjznqywwza.swds"
        )
        ensure_dir(dataset_path)
        ensure_file(dataset_path / DEFAULT_MANIFEST_NAME, " ")

        runtime_path = (
            project_dir
            / URIType.RUNTIME
            / RECOVER_DIRNAME
            / "mnist"
            / "g4"
            / "g43tsodfg5sdqnrtmftdgyjzo5zdgoi.swrt"
        )
        runtime_workdir_path = (
            project_dir
            / "workdir"
            / URIType.RUNTIME
            / RECOVER_DIRNAME
            / "mnist"
            / "g4"
            / "g43tsodfg5sdqnrtmftdgyjzo5zdgoi"
        )
        ensure_dir(runtime_path.parent)
        ensure_file(runtime_path, " ")
        ensure_dir(runtime_workdir_path)
        ensure_file(runtime_workdir_path / DEFAULT_MANIFEST_NAME, " ")

        job_path = (
            project_dir
            / URIType.JOB
            / RECOVER_DIRNAME
            / "gi"
            / "giztgyldgmzggytegftdszlbof4dq3i"
        )
        ensure_dir(job_path)
        ensure_file(job_path / DEFAULT_MANIFEST_NAME, " ")

        removed_project = sw.rootdir / RECOVER_DIRNAME / "myproject"
        ensure_dir(removed_project)

        gc(dry_run=True, yes=True)
        assert model_path.exists()
        assert model_workdir_path.exists()
        assert dataset_path.exists()
        assert runtime_path.exists()
        assert runtime_workdir_path.exists()
        assert job_path.exists()
        assert removed_project.exists()
        assert saved_model_path.exists()

        gc(dry_run=False, yes=True)
        assert not model_path.exists()
        assert not model_workdir_path.exists()
        assert not dataset_path.exists()
        assert not runtime_path.exists()
        assert not runtime_workdir_path.exists()
        assert not job_path.exists()
        assert not removed_project.exists()
        assert saved_model_path.exists()

    def test_get_workdir_path(self):
        project_dir = Path("~/.cache/starwhale/self")
        ts = [
            {
                "type": URIType.MODEL,
                "bundle_path": (
                    project_dir
                    / URIType.MODEL
                    / ".recover/mnist/gq/gq4wmmrrgazwknrtmftdgyjzmfwxczi.swmp"
                ),
                "result": (
                    project_dir
                    / "workdir"
                    / URIType.MODEL
                    / "mnist/gq/gq4wmmrrgazwknrtmftdgyjzmfwxczi"
                ),
            },
            {
                "type": URIType.DATASET,
                "bundle_path": (
                    project_dir
                    / URIType.DATASET
                    / ".recover/mnist/gq/gq4wmmrrgazwknrtmftdgyjzmfwxczi.swds"
                ),
                "result": (
                    project_dir
                    / "workdir"
                    / URIType.DATASET
                    / "mnist/gq/gq4wmmrrgazwknrtmftdgyjzmfwxczi"
                ),
            },
            {
                "type": URIType.RUNTIME,
                "bundle_path": (
                    project_dir
                    / URIType.RUNTIME
                    / ".recover/mnist/gq/gq4wmmrrgazwknrtmftdgyjzmfwxczi.swrt"
                ),
                "result": (
                    project_dir
                    / "workdir"
                    / URIType.RUNTIME
                    / "mnist/gq/gq4wmmrrgazwknrtmftdgyjzmfwxczi"
                ),
            },
            {
                "type": URIType.JOB,
                "bundle_path": (
                    project_dir
                    / URIType.JOB
                    / ".recover/gq/gq4wmmrrgazwknrtmftdgyjzmfwxczi"
                ),
                "result": (
                    project_dir
                    / "workdir"
                    / URIType.JOB
                    / "gq/gq4wmmrrgazwknrtmftdgyjzmfwxczi"
                ),
            },
        ]

        for _t in ts:
            assert _t["result"] == _get_workdir_path(
                project_dir, _t["type"], _t["bundle_path"]
            )
