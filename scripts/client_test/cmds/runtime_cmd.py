import json
from typing import Tuple, Dict, Any, List

from .base.environment import CLI
from .base.invoke import invoke


class Runtime:
    runtime_cmd = "runtime"

    def build(self,
              workdir: str,
              project: str = "",
              runtime_yaml: str = "",
              gen_all_bundles: bool = False,
              include_editable: bool = False,
              enable_lock: bool = False,
              env_prefix_path: str = "",
              env_name: str = "", ) -> bool:
        _args = [CLI, self.runtime_cmd, "build"]

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
        return True if not _err else False

    def info(self, uri: str) -> Dict[str, Any]:
        """
        :param uri: pytorch/version/latest
        :return:
            {
                "bundle_path": "/home/star_g/.starwhale/self/runtime/pytorch/mj/mjqtinbtgjqtezjyg44tkzjwnm2gmny.swrt",
                "config": {
                    "artifacts": {},
                    "base_image": "ghcr.io/star-whale/starwhale:latest-cuda11.4",
                    "build": {
                        "os": "Linux",
                        "sw_version": "0.0.0.dev0"
                    },
                    "configs": {},
                    "created_at": "2022-09-14 17:58:05 CST",
                    "dependencies": {},
                    "environment": {},
                    "name": "pytorch",
                    "version": "mjqtinbtgjqtezjyg44tkzjwnm2gmny"
                },
                "history": [],
                "name": "pytorch",
                "project": "self",
                "snapshot_workdir": "/home/star_g/.starwhale/self/workdir/runtime/pytorch/mj/mjqtinbtgjqtezjyg44tkzjwnm2gmny",
                "tags": [
                    "latest"
                ],
                "uri": "local/project/self/runtime/pytorch/version/latest",
                "version": "mjqtinbtgjqtezjyg44tkzjwnm2gmny"
            }
        """
        _res, _err = invoke([CLI, "-o", "json", self.runtime_cmd, "info", uri])
        return json.loads(_res) if not _err else {}

    def list(self,
             project: str,
             fullname: bool = False,
             show_removed: bool = False,
             page: int = 1,
             size: int = 20,) -> List[Dict[str, Any]]:

        _args = [CLI, "-o", "json", self.runtime_cmd, "list", "--page", page, "--size", size]
        if project:
            _args.extend(["--project", project])
        if fullname:
            _args.append("--fullname")
        if show_removed:
            _args.append("--show-removed")

        _res, _err = invoke(_args)
        return json.loads(_res) if not _err else []

    def activate(self) -> Tuple[str, str]:
        return invoke([CLI, self.runtime_cmd, "", "", "", "", "", "", "", ])

    def copy(self) -> Tuple[str, str]:
        return invoke([CLI, self.runtime_cmd, "", "", "", "", "", "", "", ])

    def extract(self) -> Tuple[str, str]:
        return invoke([CLI, self.runtime_cmd, "", "", "", "", "", "", "", ])

    def lock(self) -> Tuple[str, str]:
        return invoke([CLI, self.runtime_cmd, "", "", "", "", "", "", "", ])

    def remove(self) -> Tuple[str, str]:
        return invoke([CLI, self.runtime_cmd, "", "", "", "", "", "", "", ])

    def recover(self) -> Tuple[str, str]:
        return invoke([CLI, self.runtime_cmd, "", "", "", "", "", "", "", ])

    def restore(self) -> Tuple[str, str]:
        return invoke([CLI, self.runtime_cmd, "", "", "", "", "", "", "", ])

    def history(self, uri: str, fullname: bool = False) -> List[Dict[str, Any]]:
        _args = [CLI, self.runtime_cmd, "history"]
        if fullname:
            _args.append("--fullname")
        _args.append(uri)
        _res, _err = invoke(_args)
        return json.loads(_res) if not _err else {}

    def tag(self) -> Tuple[str, str]:
        return invoke([CLI, self.runtime_cmd, "", "", "", "", "", "", "", ])

    def quick_start_shell(self) -> Tuple[str, str]:
        return invoke([CLI, self.runtime_cmd, "", "", "", "", "", "", "", ])

    def quick_start_uri(self) -> Tuple[str, str]:
        return invoke([CLI, self.runtime_cmd, "", "", "", "", "", "", "", ])

    def dockerize(self) -> Tuple[str, str]:
        return invoke([CLI, self.runtime_cmd, "dockerize", "", "", "", "", "", "", ])
