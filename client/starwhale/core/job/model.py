from __future__ import annotations

import typing as t
import subprocess
from abc import ABCMeta, abstractmethod
from typing import Union

from pydantic import BaseModel

from starwhale.utils import load_yaml
from starwhale.consts import (
    HTTPMethod,
    CREATED_AT_KEY,
    DEFAULT_PAGE_IDX,
    DEFAULT_PAGE_SIZE,
)
from starwhale.utils.fs import move_dir, empty_dir
from starwhale.api._impl import wrapper
from starwhale.base.type import JobOperationType
from starwhale.base.cloud import CloudRequestMixed
from starwhale.utils.error import NotFoundError, NoSupportError
from starwhale.utils.config import SWCliConfigMixed
from starwhale.utils.process import check_call
from starwhale.core.job.store import JobStorage
from starwhale.base.models.job import JobManifest
from starwhale.api._impl.metric import MetricKind
from starwhale.base.uri.project import Project
from starwhale.base.uri.resource import Resource


class LocalJobInfo(BaseModel):
    manifest: JobManifest
    report: t.Optional[t.Dict[str, t.Any]] = None


# TODO: change the dict for remote job to the open api model
JobListType = Union[t.List[LocalJobInfo], t.List[t.Dict[str, t.Any]]]


class Job(metaclass=ABCMeta):
    def __init__(self, uri: Resource) -> None:
        self.uri = uri
        self.name = uri.name
        self.project_name = uri.project.name
        self.sw_config = SWCliConfigMixed()

    @abstractmethod
    def info(self) -> LocalJobInfo | t.Dict[str, t.Any]:
        raise NotImplementedError

    @abstractmethod
    def remove(self, force: bool = False) -> t.Tuple[bool, str]:
        raise NotImplementedError

    @abstractmethod
    def recover(self, force: bool = False) -> t.Tuple[bool, str]:
        raise NotImplementedError

    @abstractmethod
    def cancel(self, force: bool = False) -> t.Tuple[bool, str]:
        raise NotImplementedError

    @abstractmethod
    def resume(self, force: bool = False) -> t.Tuple[bool, str]:
        raise NotImplementedError

    @abstractmethod
    def pause(self, force: bool = False) -> t.Tuple[bool, str]:
        raise NotImplementedError

    @abstractmethod
    def _get_version(self) -> str:
        raise NotImplementedError

    @abstractmethod
    def _fetch_job_info(self) -> JobManifest | t.Dict[str, t.Any] | None:
        raise NotImplementedError

    def _get_report(self) -> t.Dict[str, t.Any]:
        evaluation = wrapper.Evaluation(
            eval_id=self._get_version(),
            project=self.uri.project.name,
            instance=self.uri.instance.url,
        )
        summary = evaluation.get_summary_metrics()
        kind = summary.get("kind", "")

        ret = {
            "summary": summary,
            "kind": kind,
        }

        if kind == MetricKind.MultiClassification.value:
            ret["labels"] = {
                item["id"]: item for item in list(evaluation.get("labels"))
            }
            ret["confusion_matrix"] = {
                "binarylabel": list(evaluation.get("confusion_matrix/binarylabel"))
            }

        return ret

    @classmethod
    def _get_job_cls(
        cls, uri: Project
    ) -> t.Union[t.Type[StandaloneJob], t.Type[CloudJob]]:
        if uri.instance.is_local:
            return StandaloneJob
        elif uri.instance.is_cloud:
            return CloudJob
        else:
            raise NoSupportError(f"job uri:{uri}")

    @classmethod
    def list(
        cls,
        project_uri: Project,
        page: int = DEFAULT_PAGE_IDX,
        size: int = DEFAULT_PAGE_SIZE,
    ) -> t.Tuple[JobListType, t.Dict[str, t.Any]]:
        _cls = cls._get_job_cls(project_uri)
        return _cls.list(project_uri, page=page, size=size)

    @classmethod
    def get_job(cls, job_uri: Resource) -> Job:
        _cls = cls._get_job_cls(job_uri.project)
        return _cls(job_uri)


class StandaloneJob(Job):
    def __init__(self, uri: Resource) -> None:
        super().__init__(uri)
        self.store = JobStorage(uri)

    def _get_version(self) -> str:
        return self.store.id

    @staticmethod
    def _do_flatten_summary(summary: t.Dict[str, t.Any]) -> t.Dict[str, t.Any]:
        rt = {}

        def _f(_s: t.Dict[str, t.Any], _prefix: str = "") -> None:
            for _k, _v in _s.items():
                _k = f"{_prefix}{_k}"
                if isinstance(_v, dict):
                    _f(_v, _prefix=f"{_k}.")
                else:
                    rt[_k] = _v

        _f(summary)
        return rt

    def _fetch_job_info(self) -> JobManifest | t.Dict[str, t.Any] | None:
        if not self.store.manifest:
            return None
        return JobManifest(**self.store.manifest)

    def info(self) -> LocalJobInfo | t.Dict[str, t.Any]:
        m = self._fetch_job_info()
        if m is None:
            raise NotFoundError
        if not isinstance(m, JobManifest):
            raise TypeError  # this can never happen
        return LocalJobInfo(
            manifest=m,
            report=self._get_report(),
        )

    def remove(self, force: bool = False) -> t.Tuple[bool, str]:
        if force:
            empty_dir(self.store.loc)
            return True, ""
        else:
            return move_dir(self.store.loc, self.store.recover_loc, False)

    def recover(self, force: bool = False) -> t.Tuple[bool, str]:
        return move_dir(self.store.recover_loc, self.store.loc, force)

    def cancel(self, force: bool = False) -> t.Tuple[bool, str]:
        return self._do_container_action(JobOperationType.CANCEL, force)

    def resume(self, force: bool = False) -> t.Tuple[bool, str]:
        return self._do_container_action(JobOperationType.RESUME, force)

    def pause(self, force: bool = False) -> t.Tuple[bool, str]:
        return self._do_container_action(JobOperationType.PAUSE, force)

    def _do_container_action(
        self, action: str, force: bool = False
    ) -> t.Tuple[bool, str]:
        cmd = ["docker"]
        if action == JobOperationType.CANCEL:
            cmd += ["rm", "-fv"]
        elif action == JobOperationType.PAUSE:
            cmd += ["pause"]
        elif action == JobOperationType.RESUME:
            cmd += ["unpause"]

        # search container first
        out = subprocess.check_output(
            ["docker", "ps", "-f", f"label=version={self.store.id}", "-q"]
        )
        _container = out.decode().strip()
        if not _container:
            return (
                False,
                f"failed to {action} container, reason: not found container by label=version={self.store.id}",
            )
        cmd += [_container]
        try:
            check_call(cmd)
        except Exception as e:
            return False, f"failed to {action} container, reason: {e}"
        else:
            return True, f"run {action} successfully"

    @classmethod
    def list(
        cls,
        project_uri: Project,
        page: int = DEFAULT_PAGE_IDX,
        size: int = DEFAULT_PAGE_SIZE,
    ) -> t.Tuple[JobListType, t.Dict[str, t.Any]]:
        _rt = []
        for _path, _is_removed in JobStorage.iter_all_jobs(project_uri):
            _manifest = JobManifest(**load_yaml(_path))
            if not _manifest:
                continue

            _rt.append(LocalJobInfo(manifest=_manifest))
        return _rt, {}


class CloudJob(Job, CloudRequestMixed):
    def _get_version(self) -> str:
        # TODO:use full version id
        return self.uri.name

    def info(self) -> t.Dict[str, t.Any]:
        if not self.uri.project:
            raise NotFoundError("no selected project")
        return {
            "tasks": self._fetch_tasks(),
            "manifest": self._fetch_job_info(),
            "report": self._get_report(),
        }

    def remove(self, force: bool = False) -> t.Tuple[bool, str]:
        # TODO: support force remove
        return self.do_http_request_simple_ret(
            f"/project/{self.uri.project}/job/{self.name}",
            method=HTTPMethod.DELETE,
            instance=self.uri.instance,
        )

    def recover(self, force: bool = False) -> t.Tuple[bool, str]:
        return self._do_job_action(JobOperationType.RECOVER, force)

    def cancel(self, force: bool = False) -> t.Tuple[bool, str]:
        return self._do_job_action(JobOperationType.CANCEL, force)

    def resume(self, force: bool = False) -> t.Tuple[bool, str]:
        return self._do_job_action(JobOperationType.RESUME, force)

    def pause(self, force: bool = False) -> t.Tuple[bool, str]:
        return self._do_job_action(JobOperationType.PAUSE, force)

    def _do_job_action(self, action: str, force: bool = False) -> t.Tuple[bool, str]:
        # TODO: support force action
        return self.do_http_request_simple_ret(
            f"/project/{self.uri.project.name}/job/{self.name}/{action}",
            method=HTTPMethod.POST,
            instance=self.uri.instance,
        )

    @classmethod
    def list(
        cls,
        project_uri: Project,
        page: int = DEFAULT_PAGE_IDX,
        size: int = DEFAULT_PAGE_SIZE,
    ) -> t.Tuple[JobListType, t.Dict[str, t.Any]]:
        if not project_uri:
            raise NotFoundError("no selected project")
        crm = CloudRequestMixed()

        r = crm.do_http_request(
            f"/project/{project_uri.name}/job",
            params={"pageNum": page, "pageSize": size},
            instance=project_uri.instance,
        ).json()

        jobs = [{"manifest": cls._fmt_job_info(j)} for j in r["data"]["list"]]
        return jobs, crm.parse_pager(r)

    @classmethod
    def _fmt_job_info(cls, info: t.Dict[str, t.Any]) -> t.Dict[str, t.Any]:
        info.pop("owner", None)
        info[CREATED_AT_KEY] = cls.fmt_timestamp(info["createdTime"])
        info["finished_at"] = cls.fmt_timestamp(info["stopTime"])
        info["duration_str"] = cls.fmt_duration(info["duration"])
        return info

    def _fetch_job_info(self) -> JobManifest | t.Dict[str, t.Any] | None:
        r = self.do_http_request(
            f"/project/{self.project_name}/job/{self.name}",
            instance=self.uri.instance,
        ).json()

        return self._fmt_job_info(r["data"])  # type: ignore

    def _fetch_tasks(
        self,
    ) -> t.Tuple[t.List[t.Any], t.Dict[str, t.Any]]:
        r = self.do_http_request(
            f"/project/{self.project_name}/job/{self.name}/task",
            # simplify to fetch all tasks
            params={"pageNum": 1, "pageSize": 100000},
            instance=self.uri.instance,
        ).json()

        tasks = []
        for _t in r["data"]["list"]:
            _t[CREATED_AT_KEY] = self.fmt_timestamp(_t["startedTime"]) if _t["startedTime"] else None  # type: ignore
            tasks.append(_t)

        return tasks, self.parse_pager(r)
