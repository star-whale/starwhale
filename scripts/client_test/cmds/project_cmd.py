import json
from typing import Tuple, List, Dict, Any

from . import CLI
from .base.invoke import invoke


class Project:
    project_cmd = "project"

    def create(self, name: str) -> bool:
        """
        :param name:
        :return:
        """
        _valid_str = "do successfully"
        res, err = invoke([CLI, self.project_cmd, "create", name])
        return True if not err and _valid_str in res else False

    def info(self, project: str) -> Dict[str, Any]:
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
        res, err = invoke([CLI, "-o", "json", self.project_cmd, "info", project])
        return json.loads(res) if not err else {}

    def list(self, instance: str = "local") -> List[Dict[str, Any]]:
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
        res, err = invoke([CLI, "-o", "json", self.project_cmd, "list", "-i", instance])
        return json.loads(res) if not err else []

    def remove(self, project: str) -> bool:
        """
        :return:
            res is: failed to remove project oooo, reason: src:/home/star_g/.starwhale/oooo not found
                or: remove project oooooo, you can recover it, don't panic.
            whether removed
        """
        res, err = invoke([CLI, self.project_cmd, "remove", project])
        _p = self.info(project)
        return not err and not _p

    def recover(self, project: str) -> bool:
        """
        :param project:
        :return:
            res is:failed to recover project ***, reason: dest:/home/**/.starwhale/*** existed
                or:recover project ***
        """
        res, err = invoke([CLI, self.project_cmd, "recover", project])
        _p = self.info(project)
        return not err and _p

    def select(self, project: str) -> bool:
        """
        :param project:
        :return:
            res is:select instance:local, project:self successfully
                or:failed to select self2, reason: need to create project self2
        """
        _valid_str = "successfully"
        res, err = invoke([CLI, self.project_cmd, "select", project])
        return not err and _valid_str in res
