import os
from unittest.mock import patch, MagicMock

from pyfakefs.fake_filesystem_unittest import TestCase

from starwhale.utils import config as sw_config
from starwhale.utils import load_yaml
from starwhale.consts import (
    VERSION_PREFIX_CNT,
    DEFAULT_MANIFEST_NAME,
    CNTR_DEFAULT_PIP_CACHE_DIR, DefaultYAMLName,
)
from starwhale.base.uri import URI
from starwhale.utils.fs import ensure_dir, ensure_file
from starwhale.base.type import URIType
from starwhale.utils.config import SWCliConfigMixed
from starwhale.core.eval.executor import EvalExecutor

from .. import ROOT_DIR

_dataset_manifest = open(f"{ROOT_DIR}/data/dataset.yaml").read()
_model_data_dir = f"{ROOT_DIR}/data/model"
_model_yaml = open(f"{_model_data_dir}/model.yaml").read()


class StandaloneEvalExecutor(TestCase):
    def setUp(self) -> None:
        self.setUpPyfakefs()
        sw_config._config = {}

    @patch("starwhale.core.eval.executor.check_call")
    @patch("starwhale.core.job.scheduler.Scheduler.schedule")
    def test_run(self, m_scheduler: MagicMock, m_call: MagicMock) -> None:
        sw = SWCliConfigMixed()
        project_dir = sw.rootdir / "self"

        model_bundle_path = (
            project_dir
            / URIType.MODEL
            / "mnist"
            / "gn"
            / "gnstmntggi4t111111111111.swmp"
        )
        model_workdir_path = (
            project_dir
            / "workdir"
            / URIType.MODEL
            / "mnist"
            / "gn"
            / "gnstmntggi4t111111111111"
        )
        dataset_bundle_path = (
            project_dir
            / URIType.DATASET
            / "mnist"
            / "me"
            / "me4dczlegzswgnrtmftdgyjznqywwza.swds"
        )
        runtime_bundle_path = (
            project_dir
            / URIType.RUNTIME
            / "mnist"
            / "ga"
            / "ga4doztfg4yw11111111111111.swrt"
        )
        runtime_workdir_path = (
            project_dir
            / "workdir"
            / URIType.RUNTIME
            / "mnist"
            / "ga"
            / "ga4doztfg4yw11111111111111"
        )

        ensure_dir(model_bundle_path.parent)
        ensure_file(model_bundle_path, " ")
        ensure_dir(model_workdir_path)
        ensure_file(model_workdir_path / DEFAULT_MANIFEST_NAME, "{}")
        ensure_dir(model_workdir_path / "src")
        ensure_file(model_workdir_path / "src" / DefaultYAMLName.MODEL, _model_yaml)
        ensure_dir(model_workdir_path / "src" / "models")
        ensure_dir(model_workdir_path / "src" / "config")
        ensure_file(model_workdir_path / "src" / "models" / "mnist_cnn.pt", " ")
        ensure_file(model_workdir_path / "src" / "config" / "hyperparam.json", " ")

        ensure_dir(dataset_bundle_path)
        ensure_file(dataset_bundle_path / DEFAULT_MANIFEST_NAME, _dataset_manifest)
        ensure_dir(runtime_bundle_path.parent)
        ensure_file(runtime_bundle_path, " ")
        ensure_dir(runtime_workdir_path)
        ensure_file(runtime_workdir_path / DEFAULT_MANIFEST_NAME, "{}")
        # use docker
        ee = EvalExecutor(
            model_uri="mnist/version/gnstmntggi4t",
            dataset_uris=["mnist/version/me4dczleg"],
            project_uri=URI(""),
            runtime_uri="mnist/version/ga4doztfg4yw",
            use_docker=True,
        )

        ee.run()
        build_version = ee._version

        job_dir = (
            project_dir
            / URIType.EVALUATION
            / build_version[:VERSION_PREFIX_CNT]
            / build_version
        )

        assert m_call.call_count == 2
        pull_cmd = m_call.call_args_list[0][0][0]
        ppl_cmd = m_call.call_args_list[1][0][0]
        host_cache_dir = os.path.expanduser("~/.cache/starwhale-pip")
        assert pull_cmd == "docker pull ghcr.io/star-whale/starwhale:latest"

        assert ppl_cmd == " ".join(
            [
                f"docker run --net=host --rm --name {build_version}--0 -e DEBUG=1",
                f"-v {job_dir}:/opt/starwhale",
                f"-v {sw.rootdir}:/root/.starwhale",
                f"-v {sw.object_store_dir}:{sw.object_store_dir}",
                f"-v {project_dir}/workdir/model/mnist/gn/gnstmntggi4t111111111111/src:/opt/starwhale/swmp/src",
                f"-v {project_dir}/workdir/model/mnist/gn/gnstmntggi4t111111111111/src/model.yaml:/opt/starwhale/swmp/model.yaml",
                f"-v {project_dir}/workdir/runtime/mnist/ga/ga4doztfg4yw11111111111111:/opt/starwhale/swmp",
                f"-e SW_PROJECT=self",
                f"-e SW_EVALUATION_VERSION={build_version}",
                f"-e SW_INSTANCE_URI=local",
                f"-e SW_TOKEN=",
                f"-e SW_DATASET_URI=local/project/self/dataset/mnist/version/me4dczleg",
                f"-v {host_cache_dir}:{CNTR_DEFAULT_PIP_CACHE_DIR}",
                "ghcr.io/star-whale/starwhale:latest run",
            ]
        )

        # run on host
        ee_host = EvalExecutor(
            model_uri="mnist/version/gnstmntggi4t",
            dataset_uris=["mnist/version/me4dczleg"],
            project_uri=URI(""),
            runtime_uri="mnist/version/ga4doztfg4yw",
        )

        ee_host.run()
        build_version = ee_host._version

        m_scheduler.assert_called()

        job_dir = (
            project_dir
            / URIType.EVALUATION
            / build_version[:VERSION_PREFIX_CNT]
            / build_version
        )

        _manifest_path = job_dir / DEFAULT_MANIFEST_NAME
        _manifest = load_yaml(_manifest_path)
        assert _manifest_path.exists()
        # TODO: use refactor code
        assert _manifest["status"] == "failed"
        assert _manifest["project"] == "self"
        assert _manifest["version"] == build_version
        assert _manifest["model"] == "mnist/version/gnstmntggi4t"
