from __future__ import annotations

import os
import json
import typing as t
from pathlib import Path

from starwhale.utils import gen_uniq_version
from starwhale.consts import ENV_BUILD_BUNDLE_FIXED_VERSION_FOR_TEST
from starwhale.base.type import DatasetChangeMode
from starwhale.core.model.view import ModelTermView
from starwhale.base.uri.project import Project
from starwhale.base.uri.resource import Resource, ResourceType
from starwhale.core.runtime.model import RuntimeConfig

from . import CLI
from .base.invoke import check_invoke, invoke_output

_ENV_FIXED_VERSION = ENV_BUILD_BUNDLE_FIXED_VERSION_FOR_TEST


class BaseArtifact:
    def __init__(self, name: str):
        self.name = name

    def info(self, uri: str) -> t.Any:
        _ret_code, _res = invoke_output([CLI, "-o", "json", self.name, "info", uri])
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

        _ret_code, _res = invoke_output(_args)
        return json.loads(_res) if _ret_code == 0 else []


class Model(BaseArtifact):
    def __init__(self) -> None:
        super().__init__("model")

    def run_in_host(
        self,
        model_uri: str,
        dataset_uris: t.List[str],
        runtime_uri: t.Optional[Resource],
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

        check_invoke(cmd, log=True)
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
    def build(cls, workdir: str, name: str, runtime: str = "") -> Resource:
        version = gen_uniq_version()
        cmd = [CLI, "-vvv", "model", "build", workdir, "--name", name]
        if runtime:
            cmd.extend(["--runtime", runtime])
        check_invoke(cmd, external_env={_ENV_FIXED_VERSION: version}, log=True)
        return Resource(f"{name}/version/{version}", typ=ResourceType.model)

    def copy(self, src_uri: str, target_project: str, force: bool) -> None:
        _args = [CLI, "-vvv", self.name, "copy", src_uri, target_project]
        if force:
            _args.append("--force")
        check_invoke(_args, log=True)


class Dataset(BaseArtifact):
    def __init__(self) -> None:
        super().__init__("dataset")

    @staticmethod
    def build(
        workdir: str,
        name: str,
        dataset_yaml: str = "dataset.yaml",
    ) -> Resource:
        if not name:
            name = os.path.basename(workdir)

        cmd = [
            CLI,
            "-vvv",
            "dataset",
            "build",
            "--name",
            name,
            "--dataset-yaml",
            os.path.join(workdir, dataset_yaml),
        ]
        version = gen_uniq_version()
        check_invoke(cmd, external_env={_ENV_FIXED_VERSION: version}, log=True)
        return Resource(f"{name}/version/{version}", typ=ResourceType.dataset)

    def copy(
        self,
        src_uri: str,
        target_project: str,
        force: bool = False,
        mode: DatasetChangeMode = DatasetChangeMode.PATCH,
    ) -> None:
        _args = [CLI, "-vvv", self.name, "copy", src_uri, target_project]
        if force:
            _args.append("--force")
        if mode == DatasetChangeMode.PATCH:
            _args.append("--patch")
        else:
            _args.append("--overwrite")
        check_invoke(_args)


class Runtime(BaseArtifact):
    def __init__(self) -> None:
        super().__init__("runtime")

    @classmethod
    def build(cls, workdir: str, runtime_yaml: str) -> Resource:
        version = gen_uniq_version()
        yaml_path = os.path.join(workdir, runtime_yaml)
        config = RuntimeConfig.create_by_yaml(Path(yaml_path))
        cmd = [
            CLI,
            "-vvv",
            "runtime",
            "build",
            "--yaml",
            yaml_path,
            "--name",
            config.name,
            "--no-cache",
        ]
        check_invoke(cmd, external_env={_ENV_FIXED_VERSION: version}, log=True)
        return Resource(f"{config.name}/version/{version}", typ=ResourceType.runtime)

    def copy(
        self,
        src_uri: str,
        target_project: str,
        force: bool,
        mode: DatasetChangeMode = DatasetChangeMode.PATCH,
    ) -> None:
        _args = [CLI, "-vvv", self.name, "copy", src_uri, target_project]
        if force:
            _args.append("--force")
        check_invoke(_args)
