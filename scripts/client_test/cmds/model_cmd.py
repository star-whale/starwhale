import json
from typing import Tuple, List, Dict, Any

from .base.invoke import invoke, invoke_with_react
from .base.environment import CLI


class Model:
    _cmd = "model"

    def build(self, workdir: str, project: str = "", model_yaml: str = "", runtime_uri: str = "") -> bool:
        _args = _args = [CLI, self._cmd, "build"]
        if project:
            _args.extend(["--project", project])
        if model_yaml:
            _args.extend(["--model-yaml", model_yaml])
        if runtime_uri:
            _args.extend(["--runtime", runtime_uri])
        _args.append(workdir)
        _res, _err = invoke(_args)
        # TODO use version match
        return True if not _err else False

    def info(self, uri: str) -> Tuple[str, str]:
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
        _res, _err = invoke([CLI, "-o", "json", self._cmd, "info", uri])
        return json.loads(_res) if not _err else {}

    def list(self,
             project: str = "self",
             fullname: bool = False,
             show_removed: bool = False,
             page: int = 1,
             size: int = 20,) -> List[Dict[str, Any]]:
        _args = [CLI, "-o", "json", self._cmd, "list", "--page", str(page), "--size", str(size)]
        if project:
            _args.extend(["--project", project])
        if fullname:
            _args.append("--fullname")
        if show_removed:
            _args.append("--show-removed")

        _res, _err = invoke(_args)
        return json.loads(_res) if not _err else []

    def eval(self,
             workdir: str,
             project: str = "",
             model_yaml: str = "",
             version: str = "",
             step: str = "",
             task_index: int = 0,
             runtime_uri: str = "",
             dataset_uri: str = "",) -> bool:
        _valid_str = "finish run, success"
        _args = [CLI, self._cmd, "eval"]
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
        _res, _err = invoke([CLI, self._cmd, "eval", "", "", "", "", "", "", ])
        return True if not _err and _valid_str in _res else False

    def copy(self, src_uri: str, target_project: str, force: bool) -> bool:
        _valid_str = "copy done"
        _args = [CLI, self._cmd, "copy", src_uri, target_project]
        if force:
            _args.append("--force")
        _res, _err = invoke(_args)
        return True if not _err and _valid_str in _res else False

    def extract(self) -> Tuple[str, str]:
        return invoke([CLI, self._cmd, "extract", "", "", "", "", "", "", ])

    def remove(self, uri: str, force: bool) -> bool:
        _valid_str = "do successfully"
        _args = [CLI, self._cmd, "remove", uri]
        if force:
            _args.append("--force")
        _res, _err = invoke_with_react(_args)
        return True if not _err and _valid_str in _res else False

    def recover(self) -> Tuple[str, str]:
        return invoke([CLI, self._cmd, "recover", "", "", "", "", "", "", ])

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
        _args = [CLI, "-o", "json", self._cmd, "history"]
        if fullname:
            _args.append("--fullname")
        _args.append(name)
        _res, _err = invoke(_args)
        return json.loads(_res) if not _err else []

    def tag(self) -> Tuple[str, str]:
        return invoke([CLI, self._cmd, "tag", "", "", "", "", "", "", ])
