from pathlib import Path

from pyfakefs.fake_filesystem_unittest import TestCase

from starwhale.mngt import gc, _get_workdir_path
from starwhale.utils import config as sw_config
from starwhale.consts import RECOVER_DIRNAME, DEFAULT_MANIFEST_NAME
from starwhale.utils.fs import ensure_dir, ensure_file
from starwhale.utils.config import SWCliConfigMixed
from starwhale.base.uri.resource import ResourceType


class GCTestCase(TestCase):
    def setUp(self) -> None:
        self.setUpPyfakefs()
        sw_config._config = {}

    def test_gc(self) -> None:
        sw = SWCliConfigMixed()
        project_dir = sw.rootdir / "self"
        saved_model_path = (
            project_dir
            / ResourceType.model.value
            / "mnist"
            / "mm"
            / "mm4wmmrrgazwknrtmftdgyjzmfwxczi.swmp"
        )
        model_path = (
            project_dir
            / ResourceType.model.value
            / RECOVER_DIRNAME
            / "mnist"
            / "gq"
            / "gq4wmmrrgazwknrtmftdgyjzmfwxczi.swmp"
        )
        model_workdir_path = (
            project_dir
            / "workdir"
            / ResourceType.model.value
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
            / ResourceType.dataset.value
            / RECOVER_DIRNAME
            / "mnist"
            / "me"
            / "me4dczlegzswgnrtmftdgyjznqywwza.swds"
        )
        ensure_dir(dataset_path)
        ensure_file(dataset_path / DEFAULT_MANIFEST_NAME, " ")

        runtime_path = (
            project_dir
            / ResourceType.runtime.value
            / RECOVER_DIRNAME
            / "mnist"
            / "g4"
            / "g43tsodfg5sdqnrtmftdgyjzo5zdgoi.swrt"
        )
        runtime_workdir_path = (
            project_dir
            / "workdir"
            / ResourceType.runtime.value
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
            / ResourceType.job.value
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
                "type": ResourceType.model.value,
                "bundle_path": (
                    project_dir
                    / ResourceType.model.value
                    / ".recover/mnist/gq/gq4wmmrrgazwknrtmftdgyjzmfwxczi.swmp"
                ),
                "result": (
                    project_dir
                    / "workdir"
                    / ResourceType.model.value
                    / "mnist/gq/gq4wmmrrgazwknrtmftdgyjzmfwxczi"
                ),
            },
            {
                "type": ResourceType.dataset.value,
                "bundle_path": (
                    project_dir
                    / ResourceType.dataset.value
                    / ".recover/mnist/gq/gq4wmmrrgazwknrtmftdgyjzmfwxczi.swds"
                ),
                "result": (
                    project_dir
                    / "workdir"
                    / ResourceType.dataset.value
                    / "mnist/gq/gq4wmmrrgazwknrtmftdgyjzmfwxczi"
                ),
            },
            {
                "type": ResourceType.runtime.value,
                "bundle_path": (
                    project_dir
                    / ResourceType.runtime.value
                    / ".recover/mnist/gq/gq4wmmrrgazwknrtmftdgyjzmfwxczi.swrt"
                ),
                "result": (
                    project_dir
                    / "workdir"
                    / ResourceType.runtime.value
                    / "mnist/gq/gq4wmmrrgazwknrtmftdgyjzmfwxczi"
                ),
            },
            {
                "type": ResourceType.job.value,
                "bundle_path": (
                    project_dir
                    / ResourceType.job.value
                    / ".recover/gq/gq4wmmrrgazwknrtmftdgyjzmfwxczi"
                ),
                "result": (
                    project_dir
                    / "workdir"
                    / ResourceType.job.value
                    / "gq/gq4wmmrrgazwknrtmftdgyjzmfwxczi"
                ),
            },
        ]

        for _t in ts:
            assert _t["result"] == _get_workdir_path(
                project_dir, _t["type"], _t["bundle_path"]
            )
