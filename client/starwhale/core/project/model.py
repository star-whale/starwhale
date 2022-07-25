from __future__ import annotations

import json
import typing as t
from abc import ABCMeta, abstractmethod
from pathlib import Path

import yaml

from starwhale.utils import validate_obj_name
from starwhale.consts import (
    HTTPMethod,
    DEFAULT_PROJECT,
    RECOVER_DIRNAME,
    DEFAULT_PAGE_IDX,
    DEFAULT_PAGE_SIZE,
    SHORT_VERSION_CNT,
)
from starwhale.base.uri import URI
from starwhale.utils.fs import move_dir, ensure_dir, get_path_created_time
from starwhale.base.type import URIType, InstanceType
from starwhale.base.cloud import CloudRequestMixed
from starwhale.utils.http import ignore_error
from starwhale.utils.error import NoSupportError
from starwhale.utils.config import SWCliConfigMixed

_SHOW_ALL = 100


class ProjectObjType:
    MODEL = "model"
    DATASET = "dataset"
    RUNTIME = "runtime"


class Project(metaclass=ABCMeta):
    def __init__(self, uri: URI) -> None:
        self.uri = uri
        self.name = uri.project.lower()
        self.sw_config = SWCliConfigMixed()

    @abstractmethod
    def create(self) -> t.Tuple[bool, str]:
        raise NotImplementedError

    @abstractmethod
    def info(self) -> t.Dict[str, t.Any]:
        raise NotImplementedError

    @abstractmethod
    def remove(self, force: bool = False) -> t.Tuple[bool, str]:
        raise NotImplementedError

    @abstractmethod
    def recover(self) -> t.Tuple[bool, str]:
        raise NotImplementedError

    @classmethod
    def list(
        cls,
        instance_uri: str = "",
        page: int = DEFAULT_PAGE_IDX,
        size: int = DEFAULT_PAGE_SIZE,
    ) -> t.Tuple[t.List[t.Dict[str, t.Any]], t.Dict[str, t.Any]]:
        _uri = URI(instance_uri, expected_type=URIType.INSTANCE)
        if _uri.instance_type == InstanceType.STANDALONE:
            return StandaloneProject.list()
        elif _uri.instance_type == InstanceType.CLOUD:
            return CloudProject.list(instance_uri, page, size)  # type: ignore
        else:
            raise NoSupportError(f"{instance_uri}")

    @classmethod
    def get_project(cls, project_uri: URI) -> Project:
        if project_uri.instance_type == InstanceType.STANDALONE:
            return StandaloneProject(project_uri)
        elif project_uri.instance_type == InstanceType.CLOUD:
            return CloudProject(project_uri)
        else:
            raise NoSupportError(f"{project_uri}")


class StandaloneProject(Project):
    def __init__(self, uri: URI) -> None:
        super().__init__(uri)
        self.loc = Path(self.sw_config.rootdir / self.name)
        self.recover_dir = Path(self.sw_config.rootdir / RECOVER_DIRNAME)

    def create(self) -> t.Tuple[bool, str]:
        ok, reason = validate_obj_name(self.name)
        if not ok:
            return ok, reason

        if self.loc.exists():
            return False, f"{self.loc} was already existed"

        try:
            ensure_dir(self.loc)
        except Exception as e:
            return False, str(e)
        else:
            return True, f"{self.loc} created"

    def info(self) -> t.Dict[str, t.Any]:
        if not self.loc.exists():
            return {}

        # TODO: add more project info
        return {
            "name": self.name,
            "location": str(self.loc),
            "created_at": get_path_created_time(self.loc),
        }

    def remove(self, force: bool = False) -> t.Tuple[bool, str]:
        if self.name == DEFAULT_PROJECT:
            return False, f"prohibit to remove {self.name}"

        ensure_dir(self.recover_dir)
        return move_dir(self.loc, self.recover_dir / self.name, force)

    def recover(self) -> t.Tuple[bool, str]:
        if self.name == DEFAULT_PROJECT:
            return False, f"prohibit to recover {self.name}"

        return move_dir(self.recover_dir / self.name, self.loc)

    @classmethod
    def list(
        cls,
        instance_uri: str = "",
        page: int = DEFAULT_PAGE_IDX,
        size: int = DEFAULT_PAGE_SIZE,
    ) -> t.Tuple[t.List[t.Dict[str, t.Any]], t.Dict[str, t.Any]]:
        sw = SWCliConfigMixed()

        if not sw.rootdir.exists():
            return [], {}

        rt = []
        for pdir in sw.rootdir.iterdir():
            # TODO: add more project details
            valid, _ = validate_obj_name(pdir.name)
            if not valid:
                continue

            rt.append(
                {
                    "name": pdir.name,
                    "location": str(pdir),
                    "created_at": get_path_created_time(pdir),
                }
            )

        return rt, {}


class CloudProject(Project, CloudRequestMixed):
    def create(self) -> t.Tuple[bool, str]:
        return self.do_http_request_simple_ret(
            "/project",
            method=HTTPMethod.POST,
            instance_uri=self.uri,
            data=json.dumps({"projectName": self.name}),
        )

    def recover(self) -> t.Tuple[bool, str]:
        return self.do_http_request_simple_ret(
            f"/project/{self.name}/action/recover",
            method=HTTPMethod.POST,
            instance_uri=self.uri,
        )

    def remove(self, force: bool = False) -> t.Tuple[bool, str]:
        return self.do_http_request_simple_ret(
            f"/project/{self.name}",
            method=HTTPMethod.DELETE,
            instance_uri=self.uri,
            data=json.dumps({"force": int(force)}),
        )

    @classmethod
    @ignore_error(([], {}))
    def list(
        cls,
        instance_uri: str = "",
        page: int = DEFAULT_PAGE_IDX,
        size: int = DEFAULT_PAGE_SIZE,
    ) -> t.Tuple[t.List[t.Dict[str, t.Any]], t.Dict[str, t.Any]]:
        crm = CloudRequestMixed()
        uri = URI(instance_uri, expected_type=URIType.INSTANCE)
        r = crm.do_http_request(
            "/project",
            params={"pageNum": page, "pageSize": size, "ownerName": uri.user_name},
            instance_uri=uri,
        ).json()

        projects = []
        for _p in r["data"]["list"]:
            owner = _p["owner"]["name"]
            projects.append(
                dict(
                    id=_p["id"],
                    name=_p["name"],
                    created_at=crm.fmt_timestamp(_p["createdTime"]),  # type: ignore
                    is_default=_p["isDefault"],
                    owner=owner,
                )
            )
        return projects, crm.parse_pager(r)

    @ignore_error({})
    def info(self) -> t.Dict[str, t.Any]:
        r = self.do_http_request(
            f"/project/{self.name}",
            method=HTTPMethod.GET,
            instance_uri=self.uri,
        )
        # TODO: add more project details
        return {
            "name": self.name,
            "created_at": self.fmt_timestamp(r.json()["data"]["createdTime"]),
            "location": r.url,
            "models": self._fetch_project_objects(ProjectObjType.MODEL),
            "datasets": self._fetch_project_objects(ProjectObjType.DATASET),
        }

    @ignore_error([])
    def _fetch_model_files(self, mid: int) -> t.List:
        r = self.do_http_request(
            f"/project/{self.name}/model/{mid}", instance_uri=self.uri
        )
        return r.json()["data"]["files"]  # type: ignore

    @ignore_error([])
    def _fetch_project_objects(
        self, typ: str, versions_size: int = 10
    ) -> t.List[t.Dict[str, t.Any]]:
        r = self.do_http_request(
            f"/project/{self.name}/{typ}",
            params={"pageSize": _SHOW_ALL},
            instance_uri=self.uri,
        )

        ret = []
        for _m in r.json()["data"]["list"]:
            _m["created_at"] = self.fmt_timestamp(_m.pop("createdTime"))
            _m.pop("owner", None)

            mvr = self.do_http_request(
                f"/project/{self.name}/{typ}/{_m['id']}/version",
                params={"pageSize": versions_size},
                instance_uri=self.uri,
            )
            versions = []
            for _v in mvr.json()["data"]["list"]:
                _v["short_name"] = _v["name"][:SHORT_VERSION_CNT]
                _v["created_at"] = self.fmt_timestamp(_v.pop("createdTime"))
                _v.pop("owner", None)
                if typ == ProjectObjType.DATASET:
                    _v["meta"] = yaml.safe_load(_v["meta"])
                versions.append(_v)

            _m["latest_versions"] = versions
            if typ == ProjectObjType.MODEL:
                _m["files"] = self._fetch_model_files(_m["id"])
            ret.append(_m)

        return ret
