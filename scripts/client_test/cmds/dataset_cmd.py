import json
from typing import Tuple, Dict, Any, List

from .base.invoke import invoke, invoke_with_react
from .base.environment import CLI


class Dataset:
    _cmd = "dataset"

    def build(self,
              workdir: str,
              project: str = "",
              dataset_yaml: str = "",
              append: bool = False,
              append_from: str = "",
              runtime_uri: str = "",) -> bool:
        _args = [CLI, self._cmd, "build"]

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
        return True if not _err else False

    def info(self, uri: str) -> Dict[str, Any]:
        """
        :param uri: mnist/version/latest
        :return:
            {
                "bundle_path": "/home/**/.starwhale/self/dataset/mnist/he/heztmzjrmjstanrtmftdgyjzgizwkoa.swds",
                "config": {
                    "build": {
                        "os": "Linux",
                        "sw_version": "0.0.0.dev0"
                    },
                    "created_at": "2022-09-15 11:36:25 CST",
                    "dataset_attr": {
                        "alignment_size": 1024,
                        "data_mime_type": "x/grayscale",
                        "volume_size": 4194304
                    },
                    "dataset_byte_size": 9920000,
                    "dataset_summary": {
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
                    "from": {
                        "append": false,
                        "version": ""
                    },
                    "name": "mnist",
                    "process": "mnist.process:DataSetProcessExecutor",
                    "signature": [
                        "4195168:blake2b:d6c2fe3412c0b1ddb2881f3a540c855c92c0ce74b112cb67dc2bb9bf6dca36e81c79445c7cc2e8b9be171aba7b653708fd5c7788aa3c8f9407c911c971ca38e8",
                        "1529664:blake2b:64af39b5e4525bd6dc4b217da566a9f91d16783b22e1908418dd38f3a0b9bedf4a1a8d3837996a571875c1472a7caa9dcc8a80d83af7b61462f55fd2260bd27d",
                        "4195168:blake2b:4797db9689055c83a471b85fe30b629b6c2c93d37f79ea43462d6825379bc17e5f4a3dae368cabdffa04a0de559a2f8c38b635ff0fc03d4266d8960c916ad880"
                    ],
                    "version": "heztmzjrmjstanrtmftdgyjzgizwkoa"
                },
                "history": [],
                "name": "mnist",
                "project": "self",
                "snapshot_workdir": "/home/**/.starwhale/self/dataset/mnist/he/heztmzjrmjstanrtmftdgyjzgizwkoa.swds",
                "tags": [
                    "latest"
                ],
                "uri": "local/project/self/dataset/mnist/version/latest",
                "version": "heztmzjrmjstanrtmftdgyjzgizwkoa"
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
        _res, _err = invoke([CLI, "-o", "json", self._cmd, "summary", uri])
        return json.loads(_res) if not _err else {}

    def copy(self, src_uri: str, target_project: str, with_auth: bool, force: bool) -> bool:
        _valid_str = "copy done"
        _args = [CLI, self._cmd, "copy", src_uri, target_project]
        if force:
            _args.append("--force")
        if with_auth:
            _args.append("--with-auth")
        _res, _err = invoke(_args)
        return True if not _err and _valid_str in _res else False

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
        _res, _err = invoke([CLI, "-o", "json", self._cmd, "diff", base_uri, compare_uri])
        return json.loads(_res) if not _err else {}

    def remove(self, uri: str, force: bool) -> bool:
        _valid_str = "do successfully"
        _args = [CLI, self._cmd, "remove", uri]
        if force:
            _args.append("--force")
        _res, _err = invoke_with_react(_args)
        return True if not _err and _valid_str in _res else False

    def recover(self) -> Tuple[str, str]:
        return invoke([CLI, self._cmd, "", "", "", "", "", "", "", ])

    def history(self) -> Tuple[str, str]:
        return invoke([CLI, self._cmd, "", "", "", "", "", "", "", ])

    def tag(self) -> Tuple[str, str]:
        return invoke([CLI, self._cmd, "", "", "", "", "", "", "", ])
