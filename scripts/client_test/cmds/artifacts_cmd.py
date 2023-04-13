import json
import typing as t
from pathlib import Path

from starwhale.utils.load import import_object
from starwhale.core.model.view import ModelTermView
from starwhale.core.dataset.type import DatasetConfig
from starwhale.core.dataset.view import DatasetTermView
from starwhale.core.runtime.view import RuntimeTermView
from starwhale.api._impl.data_store import LocalDataStore

from . import CLI, DatasetExpl
from .base.invoke import invoke, invoke_with_react


class BaseArtifact:
    def __init__(self, name: str):
        self.name = name

    def info(self, uri: str) -> t.Any:
        """
        :param uri: mnist/version/latest
        :return:
            {
                "bundle_path": "/home/**/.starwhale/self/model/mnist/mv/mvswkodbgnrtsnrtmftdgyjznrxg65y.swmp",
                "config": {
                    "build": {
                        "os": "Linux",
                        "sw_version": "0.2.3a13"
                    },
                    "created_at": "2022-08-30 17:55:52 CST",
                    "name": "mnist",
                    "version": "mvswkodbgnrtsnrtmftdgyjznrxg65y"
                },
                "history": [],
                "name": "mnist",
                "project": "self",
                "snapshot_workdir": "/home/**/.starwhale/self/workdir/model/mnist/mv/mvswkodbgnrtsnrtmftdgyjznrxg65y",
                "tags": [
                    "latest"
                ],
                "uri": "local/project/self/model/mnist/version/latest",
                "version": "mvswkodbgnrtsnrtmftdgyjznrxg65y"
            }
        """
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

    def remove(self, uri: str, force: bool) -> bool:
        _args = [CLI, self.name, "remove", uri]
        if force:
            _args.append("--force")
        _ret_code, _res = invoke_with_react(_args)
        return bool(_ret_code == 0)

    def recover(self, uri: str, force: bool) -> bool:
        _args = [CLI, self.name, "remove", uri]
        if force:
            _args.append("--force")
        _ret_code, _res = invoke_with_react(_args)
        return bool(_ret_code == 0)

    def history(self, name: str, fullname: bool = False) -> t.Any:
        """
        :param name: mnist
        :param fullname: bool
        :return: list
             [
                {
                    "created_at": "2022-08-18 15:50:01 CST",
                    "runtime": "",
                    "size": 4864000,
                    "tags": [],
                    "version": "gvsdqmrtgu3d"
                },
                {
                    "created_at": "2022-08-23 16:58:25 CST",
                    "runtime": "",
                    "size": 4853760,
                    "tags": [],
                    "version": "mjtdkmrumuyt"
                }
            ]
        """
        _args = [CLI, "-o", "json", self.name, "history"]
        if fullname:
            _args.append("--fullname")
        _args.append(name)
        _ret_code, _res = invoke(_args)
        return json.loads(_res) if _ret_code == 0 else []

    def tag(self, uri: str, tags: t.List[str], remove: bool, quiet: bool) -> bool:
        _args = [CLI, self.name, "tag", uri]
        if remove:
            _args.extend("--remove")
        if quiet:
            _args.extend("--quiet")
        _args.extend(tags)
        _ret_code, _res = invoke(_args)
        return bool(_ret_code == 0)


class Model(BaseArtifact):
    def __init__(self) -> None:
        super().__init__("model")

    @staticmethod
    def build_with_api(
        workdir: str,
        project: str = "",
        model_yaml: str = "",
        runtime_uri: str = "",
    ) -> t.Any:
        yaml_path = model_yaml if model_yaml else Path(workdir) / "model.yaml"
        return ModelTermView.build(workdir, project, yaml_path, runtime_uri)

    def build(
        self,
        workdir: str,
        project: str = "",
        model_yaml: str = "",
        runtime_uri: str = "",
    ) -> bool:
        _args = _args = [CLI, self.name, "build"]
        if project:
            _args.extend(["--project", project])
        if model_yaml:
            _args.extend(["--model-yaml", model_yaml])
        if runtime_uri:
            _args.extend(["--runtime", runtime_uri])
        _args.append(workdir)
        _ret_code, _res = invoke(_args)
        # TODO use version match
        return bool(_ret_code == 0)

    def copy(self, src_uri: str, target_project: str, force: bool) -> bool:
        _args = [CLI, self.name, "copy", src_uri, target_project]
        if force:
            _args.append("--force")
        _ret_code, _res = invoke(_args, log=True)
        return bool(_ret_code == 0)

    def extract(self) -> t.Any:
        return invoke([CLI, self.name, "extract"])

    def eval(
        self,
        workdir: str,
        project: str = "",
        model_yaml: str = "",
        version: str = "",
        step: str = "",
        task_index: int = 0,
        runtime_uri: str = "",
        dataset_uri: str = "",
    ) -> bool:
        _args = [CLI, self.name, "eval"]
        if project:
            _args.extend(["--project", project])
        if model_yaml:
            _args.extend(["--model-yaml", model_yaml])
        if version:
            _args.extend(["--version", version])
        if step:
            _args.extend(["--step", step, "--task-index", str(task_index)])
        if runtime_uri:
            _args.extend(["--runtime", runtime_uri])
        if dataset_uri:
            _args.extend(["--dataset", dataset_uri])
        _args.append(workdir)
        _ret_code, _res = invoke(_args)
        return bool(_ret_code == 0)


class Dataset(BaseArtifact):
    def __init__(self) -> None:
        super().__init__("dataset")

    @staticmethod
    def build_with_api(
        workdir: str,
        ds_expl: DatasetExpl,
        dataset_yaml: str = "dataset.yaml",
    ) -> t.Any:
        yaml_path = Path(workdir) / dataset_yaml
        config = DatasetConfig()
        if yaml_path.exists():
            config = DatasetConfig.create_by_yaml(yaml_path)
        config.name = ds_expl.name or config.name
        config.handler = import_object(workdir, ds_expl.handler or config.handler)
        _uri = DatasetTermView.build(workdir, config)
        LocalDataStore.get_instance().dump()
        return _uri

    def build(
        self,
        workdir: str,
        project: str = "",
        dataset_yaml: str = "",
        runtime_uri: str = "",
    ) -> bool:
        _args = [CLI, self.name, "build"]

        if project:
            _args.extend(["--project", project])
        if dataset_yaml:
            _args.extend(["--dataset-yaml", dataset_yaml])
        if runtime_uri:
            _args.extend(["--runtime", runtime_uri])

        _args.append(workdir)
        _ret_code, _res = invoke(_args)
        # TODO use version match
        return bool(_ret_code == 0)

    def summary(self, uri: str) -> t.Any:
        """
        :param uri: mnist/version/latest
        :return:
            {
                'rows': 10000,
                'increased_rows': 10000,
                'unchanged_rows': 0,
                'data_byte_size': 9920000,
                'include_link': False,
                'include_user_raw': False,
                'annotations': [
                    'label'
                ]
            }
        """
        _ret_code, _res = invoke([CLI, "-o", "json", self.name, "summary", uri])
        return json.loads(_res) if _ret_code == 0 else {}

    def copy(self, src_uri: str, target_project: str) -> bool:
        _args = [CLI, self.name, "copy", src_uri, target_project]
        _ret_code, _res = invoke(_args)
        return bool(_ret_code == 0)

    def diff(self, base_uri: str, compare_uri: str) -> t.Any:
        """
        :param base_uri: mnist/version/meytgyrtgi4d
        :param compare_uri: mnist/version/latest
        :return:
            {
                "diff_merged_output": {
                    "added": "",
                    "deleted": "",
                    "updated": ""
                },
                "diff_rows": {
                    "added": 0,
                    "deleted": 0,
                    "unchanged": 10000,
                    "updated": 0
                },
                "summary": {
                    "base": {
                        "annotations": [
                            "label"
                        ],
                        "data_byte_size": 9920000,
                        "include_link": false,
                        "include_user_raw": false,
                        "increased_rows": 10000,
                        "rows": 10000,
                        "unchanged_rows": 0
                    },
                    "compare": {
                        "annotations": [
                            "label"
                        ],
                        "data_byte_size": 9920000,
                        "include_link": false,
                        "include_user_raw": false,
                        "increased_rows": 10000,
                        "rows": 10000,
                        "unchanged_rows": 0
                    }
                },
                "version": {
                    "base": "meytgyrtgi4dmnrtmftdgyjzgazgwzq",
                    "compare": "heztmzjrmjstanrtmftdgyjzgizwkoa"
                }
            }
        """
        _ret_code, _res = invoke(
            [CLI, "-o", "json", self.name, "diff", base_uri, compare_uri]
        )
        return json.loads(_res) if _ret_code == 0 else {}


class Runtime(BaseArtifact):
    def __init__(
        self,
    ) -> None:
        super().__init__("runtime")

    @staticmethod
    def build_with_api(
        workdir: str,
        project: str = "",
        runtime_yaml: str = "runtime.yaml",
        download_all_deps: bool = False,
        include_editable: bool = False,
    ) -> t.Any:
        return RuntimeTermView.build_from_runtime_yaml(
            workdir=workdir,
            yaml_path=Path(workdir) / runtime_yaml,
            project=project,
            download_all_deps=download_all_deps,
            include_editable=include_editable,
        )

    def build(
        self,
        workdir: str,
        project: str = "",
        runtime_yaml: str = "",
        download_all_deps: bool = False,
        include_editable: bool = False,
        enable_lock: bool = False,
        env_prefix_path: str = "",
        env_name: str = "",
    ) -> bool:
        _args = [CLI, self.name, "build"]

        if project:
            _args.extend(["--project", project])
        if runtime_yaml:
            _args.extend(["--runtime-yaml", runtime_yaml])
        if env_prefix_path:
            _args.extend(["--env-prefix-path", env_prefix_path])
        if env_name:
            _args.extend(["--env-name", env_name])
        if download_all_deps:
            _args.append("--gen-all-bundles")
        if include_editable:
            _args.append("--include-editable")
        if enable_lock:
            _args.append("--enable-lock")

        _args.append(workdir)

        _ret_code, _res = invoke(_args)
        # TODO use version match
        return bool(_ret_code == 0)

    def activate(self, uri: str, path: str) -> t.Any:
        """
        activate
        :param uri: Runtime uri which has already been restored
        :param path: User's runtime workdir
        :return:
        """
        return invoke([CLI, self.name, "activate", "--uri", uri, "--path", path])

    def copy(self, src_uri: str, target_project: str, force: bool) -> bool:
        _args = [CLI, self.name, "copy", src_uri, target_project]
        if force:
            _args.append("--force")
        _ret_code, _res = invoke(_args)
        return bool(_ret_code == 0)

    def extract(self, uri: str, force: bool = False, target_dir: str = "") -> t.Any:
        """
        extract
        :param uri:
        :param force:Force to extract runtime
        :param target_dir:Extract target dir.if omitted, sw will use starwhale  default workdir
        :return:
        """
        _args = [CLI, self.name, "extract", uri]
        if force:
            _args.append("--force")
        if target_dir:
            _args.extend(["--target-dir", target_dir])
        return invoke(_args)

    # TODO impl valid result logic
    def lock(self) -> t.Any:
        return invoke([CLI, self.name, "lock"])

    def restore(self) -> t.Any:
        return invoke([CLI, self.name, "restore"])

    def quick_start_shell(self) -> t.Any:
        return invoke([CLI, self.name, "quick-start", "shell"])

    def quick_start_uri(self) -> t.Any:
        return invoke([CLI, self.name, "quick-start", "uri"])

    def dockerize(self) -> t.Any:
        return invoke([CLI, self.name, "dockerize"])
