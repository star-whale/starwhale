import json
from typing import Tuple, Dict, Any, List

from . import CLI
from .base.invoke import invoke, invoke_with_react


class BaseArtifact:
    def __init__(self, name: str):
        self.name = name

    def info(self, uri: str) -> Dict[str, Any]:
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
        _res, _err = invoke([CLI, "-o", "json", self.name, "info", uri])
        return json.loads(_res) if not _err else {}

    def list(self,
             project: str = "self",
             fullname: bool = False,
             show_removed: bool = False,
             page: int = 1,
             size: int = 20, ) -> List[Dict[str, Any]]:
        _args = [CLI, "-o", "json", self.name, "list", "--page", str(page), "--size", str(size)]
        if project:
            _args.extend(["--project", project])
        if fullname:
            _args.append("--fullname")
        if show_removed:
            _args.append("--show-removed")

        _res, _err = invoke(_args)
        return json.loads(_res) if not _err else []

    def remove(self, uri: str, force: bool) -> bool:
        _valid_str = "do successfully"
        _args = [CLI, self.name, "remove", uri]
        if force:
            _args.append("--force")
        _res, _err = invoke_with_react(_args)
        return True if not _err and _valid_str in _res else False

    def recover(self, uri: str, force: bool) -> bool:
        _valid_str = "do successfully"
        _args = [CLI, self.name, "remove", uri]
        if force:
            _args.append("--force")
        _res, _err = invoke_with_react(_args)
        return not _err and _valid_str in _res

    def history(self, name: str, fullname: bool = False) -> List[Dict[str, Any]]:
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
        _res, _err = invoke(_args)
        return json.loads(_res) if not _err else []

    def tag(self, uri: str, tags: List[str], remove: bool, quiet: bool) -> bool:
        _args = [CLI, self.name, "tag", uri]
        if remove:
            _args.extend("--remove")
        if quiet:
            _args.extend("--quiet")
        _args.extend(tags)
        _res, _err = invoke(_args)
        return not _err


class Model(BaseArtifact):
    def __init__(self):
        super().__init__("model")

    def build(self, workdir: str, project: str = "", model_yaml: str = "", runtime_uri: str = "") -> bool:
        _args = _args = [CLI, self.name, "build"]
        if project:
            _args.extend(["--project", project])
        if model_yaml:
            _args.extend(["--model-yaml", model_yaml])
        if runtime_uri:
            _args.extend(["--runtime", runtime_uri])
        _args.append(workdir)
        _res, _err = invoke(_args)
        # TODO use version match
        return not _err

    def copy(self, src_uri: str, target_project: str, force: bool) -> bool:
        _valid_str = "copy done"
        _args = [CLI, self.name, "copy", src_uri, target_project]
        if force:
            _args.append("--force")
        _res, _err = invoke(_args)
        return not _err and _valid_str in _res

    def extract(self) -> Tuple[str, str]:
        return invoke([CLI, self.name, "extract"])

    def eval(self,
             workdir: str,
             project: str = "",
             model_yaml: str = "",
             version: str = "",
             step: str = "",
             task_index: int = 0,
             runtime_uri: str = "",
             dataset_uri: str = "", ) -> bool:
        _valid_str = "finish run, success"
        _args = [CLI, self.name, "eval"]
        if project:
            _args.extend(["--project", project])
        if model_yaml:
            _args.extend(["--model-yaml", model_yaml])
        if version:
            _args.extend(["--version", version])
        if step:
            _args.extend(["--step", step, "--task-index", task_index])
        if runtime_uri:
            _args.extend(["--runtime", runtime_uri])
        if dataset_uri:
            _args.extend(["--dataset", dataset_uri])
        _args.append(workdir)
        _res, _err = invoke(_args)
        return not _err and _valid_str in _res


class Dataset(BaseArtifact):
    def __init__(self):
        super().__init__("dataset")

    def build(self,
              workdir: str,
              project: str = "",
              dataset_yaml: str = "",
              append: bool = False,
              append_from: str = "",
              runtime_uri: str = "", ) -> bool:
        _args = [CLI, self.name, "build"]

        if project:
            _args.extend(["--project", project])
        if dataset_yaml:
            _args.extend(["--dataset-yaml", dataset_yaml])
        if append:
            _args.extend(["--append", "--append-from", append_from])
        if runtime_uri:
            _args.extend(["--runtime", runtime_uri])

        _args.append(workdir)
        _res, _err = invoke(_args)
        # TODO use version match
        return not _err

    def summary(self, uri: str) -> Dict[str, Any]:
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
        _res, _err = invoke([CLI, "-o", "json", self.name, "summary", uri])
        return json.loads(_res) if not _err else {}

    def copy(self, src_uri: str, target_project: str, with_auth: bool, force: bool) -> bool:
        _valid_str = "copy done"
        _args = [CLI, self.name, "copy", src_uri, target_project]
        if force:
            _args.append("--force")
        if with_auth:
            _args.append("--with-auth")
        _res, _err = invoke(_args)
        return not _err and _valid_str in _res

    def diff(self, base_uri: str, compare_uri: str) -> Dict[str, Any]:
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
        _res, _err = invoke([CLI, "-o", "json", self.name, "diff", base_uri, compare_uri])
        return json.loads(_res) if not _err else {}


class Runtime(BaseArtifact):
    def __init__(self, ):
        super().__init__("runtime")

    def build(self,
              workdir: str,
              project: str = "",
              runtime_yaml: str = "",
              gen_all_bundles: bool = False,
              include_editable: bool = False,
              enable_lock: bool = False,
              env_prefix_path: str = "",
              env_name: str = "", ) -> bool:
        _args = [CLI, self.name, "build"]

        if project:
            _args.extend(["--project", project])
        if runtime_yaml:
            _args.extend(["--runtime-yaml", runtime_yaml])
        if env_prefix_path:
            _args.extend(["--env-prefix-path", env_prefix_path])
        if env_name:
            _args.extend(["--env-name", env_name])
        if gen_all_bundles:
            _args.append("--gen-all-bundles")
        if include_editable:
            _args.append("--include-editable")
        if enable_lock:
            _args.append("--enable-lock")

        _args.append(workdir)

        _res, _err = invoke(_args)
        # TODO use version match
        return not _err

    def activate(self, uri: str, path: str) -> Tuple[str, str]:
        """
        activate
        :param uri: Runtime uri which has already been restored
        :param path: User's runtime workdir
        :return:
        """
        return invoke([CLI, self.name, "activate", "--uri", uri, "--path", path])

    def copy(self, src_uri: str, target_project: str, force: bool) -> bool:
        _valid_str = "copy done"
        _args = [CLI, self.name, "copy", src_uri, target_project]
        if force:
            _args.append("--force")
        _res, _err = invoke(_args)
        return not _err and _valid_str in _res

    def extract(self, uri: str, force: bool = False, target_dir: str = "") -> Tuple[str, str]:
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
    def lock(self) -> Tuple[str, str]:
        return invoke([CLI, self.name, "lock"])

    def restore(self) -> Tuple[str, str]:
        return invoke([CLI, self.name, "restore"])

    def quick_start_shell(self) -> Tuple[str, str]:
        return invoke([CLI, self.name, "quick-start", "shell"])

    def quick_start_uri(self) -> Tuple[str, str]:
        return invoke([CLI, self.name, "quick-start", "uri"])

    def dockerize(self) -> Tuple[str, str]:
        return invoke([CLI, self.name, "dockerize"])
