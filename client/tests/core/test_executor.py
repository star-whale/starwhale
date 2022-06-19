import os
import json
from unittest.mock import patch, MagicMock

import yaml
from pyfakefs.fake_filesystem_unittest import TestCase

from starwhale.utils import config as sw_config
from starwhale.consts import (
    VERSION_PREFIX_CNT,
    DEFAULT_MANIFEST_NAME,
    CNTR_DEFAULT_PIP_CACHE_DIR,
)
from starwhale.base.uri import URI
from starwhale.utils.fs import ensure_dir, ensure_file
from starwhale.base.type import URIType
from starwhale.utils.config import SWCliConfigMixed
from starwhale.core.job.executor import EvalExecutor

from .. import ROOT_DIR

_dataset_manifest = open(f"{ROOT_DIR}/data/dataset.yaml").read()


class StandaloneEvalExecutor(TestCase):
    def setUp(self) -> None:
        self.setUpPyfakefs()
        sw_config._config = {}

    @patch("starwhale.core.job.executor.check_call")
    def test_run(self, m_call: MagicMock) -> None:
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
        ensure_dir(dataset_bundle_path)
        ensure_file(dataset_bundle_path / DEFAULT_MANIFEST_NAME, _dataset_manifest)
        ensure_dir(runtime_bundle_path.parent)
        ensure_file(runtime_bundle_path, " ")
        ensure_dir(runtime_workdir_path)
        ensure_file(runtime_workdir_path / DEFAULT_MANIFEST_NAME, "{}")

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
            / URIType.JOB
            / build_version[:VERSION_PREFIX_CNT]
            / build_version
        )
        ppl_dir = job_dir / "ppl"
        cmp_dir = job_dir / "cmp"
        _manifest_path = job_dir / DEFAULT_MANIFEST_NAME
        _manifest = yaml.safe_load(_manifest_path.open())

        assert _manifest["version"] == build_version
        assert ppl_dir.exists()
        assert cmp_dir.exists()

        _input_json_path = ppl_dir / "config" / "input.json"
        assert _input_json_path.exists()
        _input_json = json.load(_input_json_path.open())
        assert _input_json["backend"] == "fuse"
        assert _input_json["kind"] == "swds"
        assert _input_json["swds"][0]["bucket"] == "/opt/starwhale/dataset"
        assert _input_json["swds"][0]["ext_attr"]["ds_name"] == "mnist"
        assert (
            _input_json["swds"][0]["ext_attr"]["ds_version"]
            == "me4dczlegzswgnrtmftdgyjznqywwza"
        )
        assert (
            _input_json["swds"][0]["key"]["data"]
            == "mnist/me/me4dczlegzswgnrtmftdgyjznqywwza.swds/data/data_ubyte_0.swds_bin"
        )

        assert m_call.call_count == 4
        pull_cmd = m_call.call_args_list[0][0][0]
        ppl_cmd = m_call.call_args_list[1][0][0]
        cmp_cmd = m_call.call_args_list[3][0][0]
        host_cache_dir = os.path.expanduser("~/.cache/starwhale-pip")
        assert pull_cmd == "docker pull ghcr.io/star-whale/starwhale:latest"
        assert ppl_cmd == " ".join(
            [
                f"docker run --net=host --rm --name {build_version}-ppl -e DEBUG=1",
                f"-v {project_dir}/job/{build_version[:VERSION_PREFIX_CNT]}/{build_version}/ppl:/opt/starwhale",
                f"-v {project_dir}/workdir/model/mnist/gn/gnstmntggi4t111111111111/src:/opt/starwhale/swmp/src",
                f"-v {project_dir}/workdir/model/mnist/gn/gnstmntggi4t111111111111/src/model.yaml:/opt/starwhale/swmp/model.yaml",
                f"-v {project_dir}/workdir/runtime/mnist/ga/ga4doztfg4yw11111111111111/dep:/opt/starwhale/swmp/dep",
                f"-v {project_dir}/workdir/runtime/mnist/ga/ga4doztfg4yw11111111111111/_manifest.yaml:/opt/starwhale/swmp/_manifest.yaml",
                f"-v {project_dir}/dataset:/opt/starwhale/dataset",
                f"-v {host_cache_dir}:{CNTR_DEFAULT_PIP_CACHE_DIR}",
                "ghcr.io/star-whale/starwhale:latest ppl",
            ]
        )
        assert cmp_cmd == " ".join(
            [
                f"docker run --net=host --rm --name {build_version}-cmp -e DEBUG=1",
                f"-v {project_dir}/job/{build_version[:VERSION_PREFIX_CNT]}/{build_version}/cmp:/opt/starwhale",
                f"-v {project_dir}/workdir/model/mnist/gn/gnstmntggi4t111111111111/src:/opt/starwhale/swmp/src",
                f"-v {project_dir}/workdir/model/mnist/gn/gnstmntggi4t111111111111/src/model.yaml:/opt/starwhale/swmp/model.yaml",
                f"-v {project_dir}/workdir/runtime/mnist/ga/ga4doztfg4yw11111111111111/dep:/opt/starwhale/swmp/dep",
                f"-v {project_dir}/workdir/runtime/mnist/ga/ga4doztfg4yw11111111111111/_manifest.yaml:/opt/starwhale/swmp/_manifest.yaml",
                f"-v {project_dir}/job/{build_version[:VERSION_PREFIX_CNT]}/{build_version}/ppl/result:/opt/starwhale/ppl_result",
                f"-v {host_cache_dir}:{CNTR_DEFAULT_PIP_CACHE_DIR}",
                "ghcr.io/star-whale/starwhale:latest cmp",
            ]
        )

        _manifest_path = job_dir / DEFAULT_MANIFEST_NAME
        _manifest = yaml.safe_load(_manifest_path.open())
        assert _manifest_path.exists()
        assert _manifest["phase"] == "all"
        assert _manifest["version"] == build_version
        assert _manifest["model"] == "mnist/version/gnstmntggi4t"
