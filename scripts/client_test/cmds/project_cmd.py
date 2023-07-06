import json
from typing import Any

from . import CLI
from .base.invoke import invoke_output, invoke_ret_code


class Project:
    project_cmd = "project"

    def create(self, name: str) -> bool:
        _code = invoke_ret_code([CLI, self.project_cmd, "create", name])
        return _code == 0

    def info(self, project: str) -> Any:
        """
        :return:
            local:
            {
                "created_at": "2022-08-23 17:16:39 CST",
                "location": "/home/star_g/.starwhale/self",
                "name": "self"
            }
            cloud:
            {
                "created_at": "2022-09-01 15:26:46",
                "datasets": [...],
                "location": "http://cloud.pre.intra.starwhale.ai/api/v1/project/starwhale",
                "models": [...],
                "name": "starwhale"
            }
        """
        _ret_code, _res = invoke_output(
            [CLI, "-o", "json", self.project_cmd, "info", project]
        )
        return json.loads(_res) if _ret_code == 0 else {}

    def list(self, instance: str = "local") -> Any:
        """
        :param instance:
        :return:
            [
                {
                    "created_at": "2022-08-23 17:16:39 CST",
                    "in_use": true,
                    "location": "/home/**/.starwhale/self",
                    "name": "self",
                    "owner": ""
                },
                {
                    "created_at": "2022-09-07 18:56:35 CST",
                    "in_use": false,
                    "location": "/home/**/.starwhale/test-o",
                    "name": "test-o",
                    "owner": ""
                }
            ]
        """
        _ret_code, _res = invoke_output(
            [CLI, "-o", "json", self.project_cmd, "list", "-i", instance]
        )
        return json.loads(_res) if _ret_code == 0 else []

    def remove(self, project: str) -> bool:
        """
        :return:
            res is: failed to remove project oooo, reason: src:/home/star_g/.starwhale/oooo not found
                or: remove project oooooo, you can recover it, don't panic.
            whether removed
        """
        _code = invoke_ret_code([CLI, self.project_cmd, "remove", project])
        _p = self.info(project)
        return _code == 0 and not _p

    def recover(self, project: str) -> bool:
        """
        :param project:
        :return:
            res is:failed to recover project ***, reason: dest:/home/**/.starwhale/*** existed
                or:recover project ***
        """
        _code = invoke_ret_code([CLI, self.project_cmd, "recover", project])
        _p = self.info(project)
        return _code == 0 and bool(_p)

    def select(self, project: str) -> bool:
        """
        :param project:
        :return:
            res is:select instance:local, project:self successfully
                or:failed to select self2, reason: need to create project self2
        """
        _ret_code = invoke_ret_code([CLI, self.project_cmd, "select", project])
        return _ret_code == 0
