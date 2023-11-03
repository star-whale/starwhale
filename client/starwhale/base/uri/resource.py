from __future__ import annotations

import re
from enum import Enum
from glob import glob
from http import HTTPStatus
from typing import Any, Dict, Optional
from pathlib import Path

import requests
from requests import HTTPError

from starwhale.utils import console, load_yaml
from starwhale.consts import SW_API_VERSION
from starwhale.base.type import BundleType
from starwhale.utils.error import NoSupportError
from starwhale.utils.config import load_swcli_config
from starwhale.base.uri.project import Project
from starwhale.base.uri.instance import Instance
from starwhale.base.uri.exceptions import (
    VerifyException,
    NoMatchException,
    UriTooShortException,
    MultipleMatchException,
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
        self.name = ""  # job/evaluation has no name, init with empty string
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
            if refine:
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

        self._parse_resource(uri, refine, typ)

    def _parse_resource(self, uri: str, refine: bool, typ: ResourceType | None) -> None:
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
            except HTTPError as e:
                if e.response.status_code == HTTPStatus.NOT_FOUND:
                    console.warning(f"refine remote resource[{typ}] {uri} failed: {e}")
                else:
                    raise

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
                self._parse_by_version(parts[0], detect_typ=False)
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
            return self._parse_by_version(uri, detect_typ=True)
        typ = ResourceType[p[0]]
        return self._parse_with_type(typ, p[1])

    def _parse_by_version(self, ver: str, detect_typ: bool = False) -> None:
        if self.instance.is_local:
            root = Path(load_swcli_config()["storage"]["root"]) / self.project.id
            # storage-root/project/type/name/prefix/full-version
            p = f"{root.absolute()}/*/*/*/{ver}*"
            m = glob(p)
            if len(m) == 1:
                _, typ, self.name, _, version = m[0].rsplit("/", 4)
                self.version = self.path_to_version(version)
                if detect_typ:
                    self.typ = ResourceType[typ]
            elif len(m) > 1:
                raise MultipleMatchException(ver, m)
            else:
                # job/evaluation list has no name
                # evaluations are stored in job dir
                m = glob(f"{root.absolute()}/job/*/{ver}*")
                if len(m) == 1:
                    _, typ, _, version = m[0].rsplit("/", 3)
                    self.version = self.path_to_version(version)
                    if detect_typ:
                        self.typ = ResourceType[typ]
                elif len(m) > 1:
                    raise MultipleMatchException(ver, m)
                else:
                    raise NoMatchException(ver, list(m))
        else:
            # TODO use api to check if it is a name or version
            # assume it is name for now
            self.name = ver

    def _refine_remote_rc_info(self) -> None:
        if self.project.instance.is_local:
            raise VerifyException("only used for remote resources")
        if not self.name or self.typ in (ResourceType.job, ResourceType.evaluation):
            return
        ver = self.version or "latest"
        if self._remote_info:
            # have remote info, assume it is already refined
            return

        base_path = f"{self.instance.url}/api/{SW_API_VERSION}/project/{self.project.id}/{self.typ.value}/{self.name}"
        headers = {"Authorization": self.instance.token}
        resp = requests.get(
            base_path, timeout=60, params={"versionUrl": ver}, headers=headers
        )
        resp.raise_for_status()
        self._remote_info = resp.json().get("data", {})
        self.name = self._remote_info.get("name", self.name)
        self.version = self._remote_info.get("versionName", self.version)

    def _refine_local_rc_info(self) -> None:
        root = Path(load_swcli_config()["storage"]["root"]) / self.project.id

        if self.typ in (ResourceType.job, ResourceType.evaluation):
            p = f"{root.absolute()}/{self.typ.name}/*/{self.version}*"
            m = glob(p)
            if len(m) == 1:
                _, _, _, version = m[0].rsplit("/", 3)
                self.version = self.path_to_version(version)
            else:
                raise NoMatchException(self.version, list(m))
            return

        if not self.name:
            raise VerifyException("name is required for local resource")

        # get version from manifest
        manifest = root / self.typ.name / self.name / "_manifest.yaml"
        if manifest.exists():
            content = load_yaml(manifest)
            tags = content.get("tags", {})
        else:
            tags = {}

        ver = self.version
        if ver == "":
            ver = "latest"

        if ver in tags:
            self.version = tags[ver]
            return

        # storage-root/project/type/name/prefix/full-version
        p = f"{root.absolute()}/{self.typ.name}/{self.name}/*/{self.version}*"
        m = glob(p)
        if len(m) == 1:
            _, _, self.name, _, version = Path(m[0]).as_posix().rsplit("/", 4)
            self.version = self.path_to_version(version)
        else:
            raise NoMatchException(self.version, list(m))

    @staticmethod
    def path_to_version(path: str) -> str:
        # foobarbaz.swrt -> foobarbaz
        return path.split(".")[0]

    @staticmethod
    def get_bundle_type_by_uri(uri_type: ResourceType) -> str:
        if uri_type == ResourceType.dataset:
            return BundleType.DATASET
        elif uri_type == ResourceType.model:
            return BundleType.MODEL
        elif uri_type == ResourceType.runtime:
            return BundleType.RUNTIME
        else:
            raise NoSupportError(uri_type)

    def info(self) -> Dict[str, Any]:
        # TODO: support local resource
        return self._remote_info or {}

    @property
    def instance(self) -> Instance:
        return self.project.instance

    def asdict(self) -> Dict:
        return {
            "project": self.project.id,
            "name": self.name,
            "version": self.version,
            "type": self.typ.name,
        }

    @property
    def full_uri(self) -> str:
        parts = [
            self.instance.url,
            "project",
            self.project.id,
            self.typ.value,
        ]

        if self.typ in (ResourceType.job, ResourceType.evaluation):
            if self.name:
                parts.append(self.name)
            elif self.version:
                parts.append(self.version)
            else:
                raise RuntimeError("job/evaluation uri must have version or name")
        else:
            parts.extend(
                [
                    self.name,
                    "version",
                    self.version or "latest",
                ]
            )

        return "/".join(parts)

    def __str__(self) -> str:
        return self.full_uri

    def __repr__(self) -> str:
        return f"<Resource {self.full_uri}>"

    def __eq__(self, other: object) -> bool:
        if not isinstance(other, Resource):
            return NotImplemented
        return self.full_uri == other.full_uri

    def __hash__(self) -> int:
        return hash(self.full_uri)
