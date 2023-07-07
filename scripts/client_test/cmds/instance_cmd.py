import json
from typing import Any

from starwhale.core.instance.view import InstanceTermView

from . import CLI
from .base.invoke import invoke_output, invoke_with_react


class Instance:
    instance_cmd = "instance"

    def login(
        self,
        user: str = "starwhale",
        password: str = "abcd1234",
        url: str = "http://console.pre.intra.starwhale.ai",
        alias: str = "server",
    ) -> bool:
        """
        :param alias:
        :param user:
        :param password:
        :param url: Instance URI, if ignore it, swcli will login current selected instance
        :return:
            res is:login http://console.pre.intra.starwhale.ai successfully
                or: anything else
        """
        kw = {"username": user, "password": password}
        InstanceTermView().login(url, alias, **kw)
        return True

    def info(self, instance: str = "") -> Any:
        """
        :param instance: instance alias name or uri, if ignore it, swcli will use current selected instance.
        :return:
            local:
                {
                    "instance": "local",
                    "root_dir": "/home/star_g/.starwhale"
                }
            server:
                {
                    "agents": [
                        {
                            "ip": "10.0.127.1",
                            "status": "OFFLINE",
                            "version": "KUBELET:v1.21.11"
                        },
                        ...
                    ],
                    "instance": "server...",
                    "version": "0.1.0:8c82767b60686f3e2bfea9dafe8c8cce5dd34f52"
                }
        """
        _ret_code, _res = invoke_output(
            [CLI, self.instance_cmd, "-o", "json", "info", instance]
        )
        return json.loads(_res) if _ret_code == 0 else {}

    def list(self) -> Any:
        """
        :return:
            [
                {
                    "current_project": "",
                    "in_use": false,
                    "name": "server",
                    "updated_at": "2022-09-09 10:45:30 CST",
                    "uri": "http://server.pre.intra.starwhale.ai",
                    "user_name": "lijing_test",
                    "user_role": "normal"
                },
                {
                    "current_project": "self",
                    "in_use": true,
                    "name": "local",
                    "updated_at": "2022-06-08 16:05:35 CST",
                    "uri": "local",
                    "user_name": "star_g",
                    "user_role": ""
                },
                {
                    "current_project": "project_for_test1",
                    "in_use": false,
                    "name": "pre-k8s",
                    "updated_at": "2022-08-25 19:59:24 CST",
                    "uri": "http://console.pre.intra.starwhale.ai",
                    "user_name": "starwhale",
                    "user_role": "normal"
                },
                ...
            ]
        """
        _ret_code, _res = invoke_output([CLI, "-o", "json", self.instance_cmd, "list"])
        return json.loads(_res) if _ret_code == 0 else []

    def logout(self, instance: str = "") -> bool:
        """
        :param instance: instance alias name or uri, if ignore it, swcli will logout current selected instance.
            then, the instance will remove from list
        :return:
            res is:bye
                or:skip local instance logout
                or:
        """
        _ret_code, _res = invoke_with_react(
            [CLI, self.instance_cmd, "logout", instance]
        )
        return bool(_ret_code == 0)

    def select(self, instance: str) -> bool:
        """
        :param instance: instance alias name or uri
        :return:
            res is:select local instance
                or:failed to select local2, reason: need to login instance local2
        """
        InstanceTermView().select(instance)
        return True
