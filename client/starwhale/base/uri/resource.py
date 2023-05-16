import re
from enum import Enum
from glob import glob
from typing import Any, Dict, Optional
from pathlib import Path
from dataclasses import dataclass

import requests

from starwhale.utils import console, load_yaml
from starwhale.consts import SW_API_VERSION
from starwhale.utils.config import load_swcli_config
from starwhale.base.uri.project import Project
from starwhale.base.uri.instance import Instance
from starwhale.base.uri.exceptions import (
    VerifyException,
    NoMatchException,
    UriTooShortException,
)

rc_url_regex = re.compile(
    r"(?P<scheme>https*)://"
    r"(?P<host>.*)/projects/"
    r"(?P<project>.+)/"
    r"(?P<rc_type>models|datasets|runtimes)"
    r"(/(?P<rc_id>.+)/versions)?"  # optional
    r"(/(?P<rc_version>.+)/.*)?",  # optional
    re.UNICODE,
)

# TODO split job and resources
job_url_regex = re.compile(
    r"(?P<scheme>https*)://"
    r"(?P<host>.*)/projects/"
    r"(?P<project>.+)/"
    r"(?P<rc_type>evaluations|jobs)"
    r"(/(?P<rc_id>.+))?",
    re.UNICODE,
)


class ResourceType(Enum):
    runtime = "runtime"
    model = "model"
    dataset = "dataset"
    evaluation = "evaluation"
    job = "job"


@dataclass(unsafe_hash=True)
class Resource:
    """
    Resource holds the DataSet, Model, Runtime, Eval etc.
    """

    typ: ResourceType
    project: Project
    name: str
    version: str

    def __init__(
        self,
        uri: str,
        typ: Optional[ResourceType] = None,
        project: Optional[Project] = None,
        token: Optional[str] = None,
        refine: bool = True,
        **kwargs: Any,
    ) -> None:
        """
        :param uri: resource name or uri or version
            accept patterns:
                * resource name without type, e.g. mnist
                * version e.g. gntdqm3fgeydqnrtmftdgyjzpj4hamy
                * full uri without version e.g. local/project/self/dataset/mnist
                * full uri e.g. local/project/self/dataset/mnist/version/latest
                * resource name and version e.g. mnist/version/latest
            typically unacceptable cases:
                * resource type and name under project e.g. self/dataset/mnist
                * resource type, name and version under project e.g. self/dataset/mnist/version/latest
        :param typ: resource type: (dataset, model, runtime etc)
        :param project: project which the resource belongs to (optional)
        :param token: token for remote resource
        :param refine: whether to refine the uri
        :return: Resource instance
        """
        self._remote_info: Dict[str, Any] = {}
        self.name = ""  # job has no name, init with empty string
        self.version = ""  # some resource has no version, init with empty string

        # check if it is url from console
        m = rc_url_regex.match(uri)
        if m is None:
            m = job_url_regex.match(uri)
        if m is not None:
            info: Dict[str, str] = m.groupdict()
            ins = Instance(f'{info["scheme"]}://{info["host"]}', token=token)
            self.project = Project(name=info["project"], instance=ins)
            self.typ = ResourceType(info["rc_type"][:-1])  # remove the last 's'
            self.name = info.get("rc_id") or ""
            self.version = info.get("rc_version") or ""
            self._refine_remote_rc_info()
            return

        if project:
            self.project = project
        else:
            try:
                self.project = Project.parse_from_full_uri(
                    uri, ignore_rc_type=typ is not None
                )
                uri = self.project.path
            except UriTooShortException:
                self.project = Project()

        if "://" in uri:
            uri = uri.split("://", 1)[-1]

        if "//" in uri:
            raise Exception(f"wrong uri({uri}) format with '//'")

        parts = len(uri.split("/"))
        # 3 == len('mnist/version/latest'.split(/))
        # means that there is no type info in the uri
        if typ and parts <= 3:
            self._parse_with_type(typ, uri)
        else:
            # uri is long enough to parse type
            self._parse_without_type(uri)
        if (typ is not None) and self.typ != typ:
            raise Exception(f"type mismatch: {self.typ} vs {typ}")

        if refine:
            try:
                self.refine()
            except (NoMatchException, VerifyException) as e:
                console.warning(f"refine resource[{typ}] {uri} failed: {e}")

    def refine(self) -> "Resource":
        if not self.project.instance.is_local:
            self._refine_remote_rc_info()
        else:
            self._refine_local_rc_info()
        return self

    def _parse_with_type(self, typ: ResourceType, uri: str) -> None:
        """
        :param typ:
        :param uri:
            i.e.
            * mnist
            * dataset/mnist
            * mnist/latest
            * version/latest
            * mnist/version/latest
        """
        self.typ = typ
        parts = uri.split("/")
        if len(parts) > 3:
            raise Exception(f"invalid uri {uri} when parse with type {typ}")

        if parts[0] == typ.value:
            parts = parts[1:]

        if len(parts) == 1:
            try:
                # try version first
                self._parse_by_version(parts[0])
            except NoMatchException:
                self.name = parts[0]
                self.version = ""
        elif len(parts) == 2:
            if parts[0] != "version":
                # name/version
                self.name = parts[0]
            self.version = parts[-1]
        else:
            if parts[1] != "version":
                raise Exception(f"invalid uri without version field: {uri}")
            self.name = parts[0]
            self.version = parts[-1]

    def _parse_without_type(self, uri: str) -> None:
        p = uri.split("/", 1)
        if len(p) == 1:
            # version only (we do not support name without type parsing)
            # there is no scenario for 'name without type' for now
            return self._parse_by_version(uri)
        typ = ResourceType[p[0]]
        return self._parse_with_type(typ, p[1])

    def _parse_by_version(self, ver: str) -> None:
        if self.instance.is_local:
            root = Path(load_swcli_config()["storage"]["root"]) / self.project.name
            # storage-root/project/type/name/prefix/full-version
            p = f"{root.absolute()}/*/*/*/{ver}*"
            m = glob(p)
            if len(m) == 1:
                _, typ, self.name, _, version = m[0].rsplit("/", 4)
                self.version = self.path_to_version(version)
                self.typ = ResourceType[typ]
            else:
                # job list has no name
                m = glob(f"{root.absolute()}/job/*/{ver}*")
                if len(m) == 1:
                    _, typ, _, version = m[0].rsplit("/", 3)
                    self.version = self.path_to_version(version)
                    self.typ = ResourceType[typ]
                else:
                    raise NoMatchException(ver, list(m))
        else:
            # TODO use api to check if it is a name or version
            # assume is is name for now
            self.name = ver

    def _refine_remote_rc_info(self) -> None:
        if self.project.instance.is_local:
            raise VerifyException("only used for remote resources")
        if not self.name or not self.version:
            return
        if not self.name.isnumeric() and not self.version.isnumeric():
            # both are not numeric, assume it is already refined
            return

        base_path = f"{self.instance.url}/api/{SW_API_VERSION}/project/{self.project.name}/{self.typ.value}/{self.name}"
        headers = {"Authorization": self.instance.token}
        resp = requests.get(
            base_path, timeout=60, params={"versionUrl": self.version}, headers=headers
        )
        resp.raise_for_status()
        self._remote_info = resp.json().get("data", {})
        self.name = self._remote_info.get("name", self.name)
        self.version = self._remote_info.get("versionName", self.version)

    def _refine_local_rc_info(self) -> None:
        root = Path(load_swcli_config()["storage"]["root"]) / self.project.name
        ver = self.version
        if ver == "":
            ver = "latest"
        if ver == "latest" or (ver.startswith("v") and ver[1:].isnumeric()):
            if not self.name:
                raise VerifyException("name is required for latest version")
            # get version from manifest
            manifest = root / self.typ.name / self.name / "_manifest.yaml"
            if not manifest.exists():
                raise VerifyException(f"manifest file not found: {manifest}")
            content = load_yaml(manifest)
            self.version = content.get("tags", {}).get(ver, "")
            return

        if self.typ == ResourceType.job:
            p = f"{root.absolute()}/{self.typ.name}/*/{self.version}*"
            m = glob(p)
            if len(m) == 1:
                _, _, _, version = m[0].rsplit("/", 3)
                self.version = self.path_to_version(version)
            else:
                raise NoMatchException(self.version, list(m))
        else:
            # storage-root/project/type/name/prefix/full-version
            p = f"{root.absolute()}/{self.typ.name}/*/*/{self.version}*"
            m = glob(p)
            if len(m) == 1:
                _, _, self.name, _, version = m[0].rsplit("/", 4)
                self.version = self.path_to_version(version)
            else:
                raise NoMatchException(self.version, list(m))

    @staticmethod
    def path_to_version(path: str) -> str:
        # foobarbaz.swrt -> foobarbaz
        return path.split(".")[0]

    def info(self) -> Dict[str, Any]:
        # TODO: support local resource
        return self._remote_info or {}

    @property
    def instance(self) -> Instance:
        return self.project.instance

    def asdict(self) -> Dict:
        return {
            "project": self.project.name,
            "name": self.name,
            "version": self.version,
            "type": self.typ.name,
        }

    @property
    def full_uri(self) -> str:
        return "/".join(
            [
                self.instance.url,
                "project",
                self.project.name,
                self.typ.value,
                self.name,
                "version",
                self.version or "latest",
            ]
        )

    def __str__(self) -> str:
        return self.full_uri

    __repr__ = __str__
