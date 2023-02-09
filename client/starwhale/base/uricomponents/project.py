from typing import Any, Optional
from dataclasses import dataclass

from starwhale.base.uri import URI
from starwhale.base.uricomponents.instance import Instance
from starwhale.base.uricomponents.exceptions import UriTooShortException


@dataclass
class Project:
    name: str
    instance: Instance
    path: str = ""

    def __init__(
        self,
        name: str = "",
        uri: Optional[str] = None,
        instance: Optional[Instance] = None,
        **kwargs: Any,
    ) -> None:
        if name and uri:
            raise Exception("name and uri can not both set")
        # init instance
        if "/" in name:
            uri = name
            name = ""
        self.instance = instance or Instance(uri=uri or "", **kwargs)

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

    def to_uri(self) -> URI:
        return URI.capsulate_uri(instance=str(self.instance), project=self.name)
