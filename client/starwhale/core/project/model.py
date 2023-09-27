from __future__ import annotations

import typing as t
from abc import ABCMeta, abstractmethod
from pathlib import Path

from starwhale.utils import validate_obj_name
from starwhale.consts import (
    CREATED_AT_KEY,
    DEFAULT_PROJECT,
    RECOVER_DIRNAME,
    DEFAULT_PAGE_IDX,
    DEFAULT_PAGE_SIZE,
)
from starwhale.utils.fs import move_dir, ensure_dir, get_path_created_time
from starwhale.base.cloud import CloudRequestMixed
from starwhale.utils.http import ignore_error
from starwhale.utils.error import NoSupportError
from starwhale.utils.config import SWCliConfigMixed
from starwhale.base.uri.project import Project as ProjectURI
from starwhale.base.uri.instance import Instance
from starwhale.base.client.api.project import ProjectApi

_SHOW_ALL = 100


class ProjectObjType:
    MODEL = "model"
    DATASET = "dataset"
    RUNTIME = "runtime"


class Project(metaclass=ABCMeta):
    def __init__(self, uri: ProjectURI) -> None:
        self.uri = uri
        self.name = uri.name.lower()
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
        _uri = Instance(instance_uri)
        if _uri.is_local:
            return StandaloneProject.list()
        return CloudProject.list(instance_uri, page, size)  # type: ignore

    @classmethod
    def get_project(cls, project_uri: ProjectURI) -> Project:
        if project_uri.instance.is_local:
            return StandaloneProject(project_uri)
        elif project_uri.instance.is_cloud:
            return CloudProject(project_uri)
        else:
            raise NoSupportError(f"{project_uri}")


class StandaloneProject(Project):
    def __init__(self, uri: ProjectURI) -> None:
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
            CREATED_AT_KEY: get_path_created_time(self.loc),
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
                    CREATED_AT_KEY: get_path_created_time(pdir),
                }
            )

        return rt, {}


class CloudProject(Project, CloudRequestMixed):
    def create(self) -> t.Tuple[bool, str]:
        resp = ProjectApi(self.uri.instance).create(self.name)
        return resp.is_success(), resp.response().message

    def recover(self) -> t.Tuple[bool, str]:
        resp = ProjectApi(self.uri.instance).recover(self.name)
        return resp.is_success(), resp.response().message

    def remove(self, force: bool = False) -> t.Tuple[bool, str]:
        resp = ProjectApi(self.uri.instance).delete(self.name)
        return resp.is_success(), resp.response().message

    @classmethod
    def list(
        cls,
        instance_uri: str = "",
        page: int = DEFAULT_PAGE_IDX,
        size: int = DEFAULT_PAGE_SIZE,
    ) -> t.Tuple[t.List[t.Dict[str, t.Any]], t.Dict[str, t.Any]]:
        crm = CloudRequestMixed()
        uri = Instance(instance_uri)
        from starwhale.base.client.api.project import ProjectApi

        resp = (
            ProjectApi(uri)
            .list(page_num=page, page_size=size)
            .raise_on_error()
            .response()
        )

        projects = []
        for _p in resp.data.list or []:
            owner = _p.owner.name
            projects.append(
                dict(
                    id=_p.id,
                    name=_p.name,
                    created_at=crm.fmt_timestamp(_p.created_time),  # type: ignore
                    is_default=0,
                    owner=owner,
                )
            )
        return projects, crm.parse_pager(resp.dict())

    @ignore_error({})
    def info(self) -> t.Dict[str, t.Any]:
        r = ProjectApi(self.uri.instance).get(self.name).response().data
        # TODO: add more project details
        base: t.Dict[str, t.Any] = {
            "name": self.name,
            CREATED_AT_KEY: self.fmt_timestamp(r.created_time),
            "location": f"{self.uri.instance.url}/projects/{r.id}",
        }

        if r.statistics is not None:
            base.update(
                {
                    "models": r.statistics.model_counts,
                    "datasets": r.statistics.dataset_counts,
                    "runtimes": r.statistics.runtime_counts,
                    "evaluations": r.statistics.evaluation_counts,
                }
            )

        return base
