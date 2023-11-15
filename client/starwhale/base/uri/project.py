import urllib
from typing import Any, Optional
from functools import lru_cache

import requests

from starwhale.utils.retry import http_retry
from starwhale.base.uri.instance import Instance
from starwhale.base.uri.exceptions import UriTooShortException


class Project:
    id: str
    name: str
    instance: Instance
    path: str = ""

    def __init__(
        self,
        name: str = "",
        uri: Optional[str] = None,
        instance: Optional[Instance] = None,
    ) -> None:
        """

        :param name: When uri belongs to standalone, it is the name. When uri belongs to cloud, name is the id.
        :param uri: project uri, like "local/project/starwhale" "http://127.0.0.1:8000/project/sw:p-1"
        :param instance: instance object, default is the current selected.
        """
        if name and uri:
            raise Exception("name and uri can not both set")
        # init instance
        if "/" in name:
            uri = name
            name = ""
        self.instance = instance or Instance(uri=uri or "")

        if not name:
            # use project name in path, the path must at least contain project/{name}
            p = self.instance.path.split("/")
            if len(p) >= 2 and p[0] == "project":
                name = p[1]
                self.path = "/".join(p[2:])
            elif len(p) == 1:
                # Compatible with features of URI
                name = p[0]

        if not name:
            # use default project
            name = self.instance.info.get("current_project", "")
        if not name:
            raise Exception("can not init project with empty name")
        self.name = name
        # TODO check if project exists for local and remote
        if self.instance.is_cloud:
            # TODO check whether contains namespace in name(like 'sw:project')?
            self.id = (
                self.name
                if self.name.isdigit()
                else str(
                    get_remote_project_id(
                        self.instance.url, self.instance.token, self.name
                    )
                )
            )
        else:
            self.id = self.name

    @classmethod
    def parse_from_full_uri(cls, uri: str, ignore_rc_type: bool) -> "Project":
        """
        Parse project from full uri.
        we do not parse instance and project info from uri less than 5 parts.
        we prefer that users use `dataset copy mnist -dlp project` rather than `dataset copy project/dataset/mnist`.
        the second is difficult to write correctly at once and the semantics are not very clear.
        and the long uri usually copied from the website.
        """
        if "://" in uri:
            no_schema_uri = uri.split("://", 1)[-1]
        else:
            no_schema_uri = uri

        if "//" in no_schema_uri:
            raise Exception(f"wrong format uri({uri}) with '//'")

        parts = no_schema_uri.split("/")
        exp = len("local/project/self/dataset/mnist".split("/"))
        if ignore_rc_type:
            # ignore type in uri like dataset
            exp = exp - 1

        if len(parts) < exp:
            raise UriTooShortException(
                exp, len(parts), f"can not parse project info from {uri}"
            )
        return cls(uri=uri)

    @property
    def full_uri(self) -> str:
        return "/".join([self.instance.url, "project", self.id])

    def __str__(self) -> str:
        return self.full_uri

    def __repr__(self) -> str:
        return f"<Project {self.full_uri}>"

    def __eq__(self, other: object) -> bool:
        if not isinstance(other, Project):
            return False
        return self.full_uri == other.full_uri


@lru_cache(maxsize=None)
@http_retry
def get_remote_project_id(instance_uri: str, token: str, project: str) -> Any:
    resp = requests.get(
        urllib.parse.urljoin(instance_uri, f"/api/v1/project/{project}"),
        headers={
            "Content-Type": "application/json; charset=utf-8",
            "Authorization": token,
        },
        timeout=60,
    )
    resp.raise_for_status()
    return resp.json().get("data", {})["id"]
