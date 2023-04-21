from __future__ import annotations

import os
import json
import typing as t
from pathlib import Path

from starwhale.utils import gen_uniq_version
from starwhale.consts import ENV_BUILD_BUNDLE_FIXED_VERSION_FOR_TEST
from starwhale.base.uri import URI
from starwhale.base.type import URIType, DatasetChangeMode
from starwhale.core.model.view import ModelTermView
from starwhale.core.runtime.model import RuntimeConfig
from starwhale.base.uricomponents.project import Project

from . import CLI
from .base.invoke import invoke

_ENV_FIXED_VERSION = ENV_BUILD_BUNDLE_FIXED_VERSION_FOR_TEST


class BaseArtifact:
    def __init__(self, name: str):
        self.name = name

    def info(self, uri: str) -> t.Any:
        _ret_code, _res = invoke([CLI, "-o", "json", self.name, "info", uri])
        return json.loads(_res) if _ret_code == 0 else {}

    def list(
        self,
        project: str = "self",
        fullname: bool = False,
        show_removed: bool = False,
        page: int = 1,
        size: int = 20,
    ) -> t.Any:
        _args = [
            CLI,
            "-o",
            "json",
            self.name,
            "list",
            "--page",
            str(page),
            "--size",
            str(size),
        ]
        if project:
            _args.extend(["--project", project])
        if fullname:
            _args.append("--fullname")
        if show_removed:
            _args.append("--show-removed")

        _ret_code, _res = invoke(_args)
        return json.loads(_res) if _ret_code == 0 else []


class Model(BaseArtifact):
    def __init__(self) -> None:
        super().__init__("model")

    def run_in_host(
        self,
        model_uri: str,
        dataset_uris: t.List[str],
        runtime_uri: t.Optional[URI],
        run_handler: str,
    ) -> str:
        version = gen_uniq_version()
        cmd = [
            CLI,
            "-vvv",
            "model",
            "run",
            "--uri",
            model_uri,
            "--version",
            version,
            "--handler",
            run_handler,
        ]

        for dataset_uri in dataset_uris:
            cmd += ["--dataset", dataset_uri]

        if runtime_uri:
            cmd += ["--runtime", str(runtime_uri)]

        _ret_code, _res = invoke(cmd)
        assert _ret_code == 0, _res
        return version

    def run_in_server(
        self,
        model_uri: str,
        dataset_uris: t.List[str],
        runtime_uri: str,
        project: str,
        run_handler: str,
    ) -> t.Tuple[bool, str]:
        return ModelTermView.run_in_server(
            project_uri=Project(project),
            model_uri=model_uri,
            dataset_uris=dataset_uris,
            runtime_uri=runtime_uri,
            run_handler=run_handler,
            resource_pool="default",
        )

    @classmethod
    def build(cls, workdir: str, name: str) -> URI:
        version = gen_uniq_version()
        cmd = [CLI, "model", "build", workdir, "--name", name]
        _ret_code, _res = invoke(cmd, external_env={_ENV_FIXED_VERSION: version})
        assert _ret_code == 0, _res
        return URI(f"{name}/version/{version}", expected_type=URIType.MODEL)

    def copy(self, src_uri: str, target_project: str, force: bool) -> None:
        _args = [CLI, self.name, "copy", src_uri, target_project]
        if force:
            _args.append("--force")
        _ret_code, _res = invoke(_args, log=True)
        assert _ret_code == 0, _res


class Dataset(BaseArtifact):
    def __init__(self) -> None:
        super().__init__("dataset")

    @staticmethod
    def build(
        workdir: str,
        name: str,
        dataset_yaml: str = "dataset.yaml",
    ) -> t.Any:
        if not name:
            name = os.path.basename(workdir)

        cmd = [
            CLI,
            "dataset",
            "build",
            workdir,
            "--name",
            name,
            "--dataset-yaml",
            dataset_yaml,
        ]
        version = gen_uniq_version()
        ret_code, res = invoke(cmd, external_env={_ENV_FIXED_VERSION: version})
        assert ret_code == 0, res
        return URI(f"{name}/version/{version}", expected_type=URIType.DATASET)

    def copy(
        self,
        src_uri: str,
        target_project: str,
        force: bool = False,
        mode: DatasetChangeMode = DatasetChangeMode.PATCH,
    ) -> None:
        _args = [CLI, self.name, "copy", src_uri, target_project]
        if force:
            _args.append("--force")
        if mode == DatasetChangeMode.PATCH:
            _args.append("--patch")
        else:
            _args.append("--overwrite")
        _ret_code, _res = invoke(_args)
        assert _ret_code == 0, _res


class Runtime(BaseArtifact):
    def __init__(self) -> None:
        super().__init__("runtime")

    @classmethod
    def build(cls, workdir: str, runtime_yaml: str) -> URI:
        version = gen_uniq_version()
        yaml_path = os.path.join(workdir, runtime_yaml)
        config = RuntimeConfig.create_by_yaml(Path(yaml_path))
        cmd = [
            CLI,
            "runtime",
            "build",
            "--yaml",
            yaml_path,
            "--name",
            config.name,
            "--no-cache",
        ]
        ret_code, res = invoke(cmd, external_env={_ENV_FIXED_VERSION: version})
        assert ret_code == 0, res
        return URI(f"{config.name}/version/{version}", expected_type=URIType.RUNTIME)

    def copy(
        self,
        src_uri: str,
        target_project: str,
        force: bool,
        mode: DatasetChangeMode = DatasetChangeMode.PATCH,
    ) -> None:
        _args = [CLI, self.name, "copy", src_uri, target_project]
        if force:
            _args.append("--force")
        _ret_code, _res = invoke(_args)
        assert _ret_code == 0, _res
