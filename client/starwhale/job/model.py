from __future__ import annotations

import typing as t
import yaml
import json

import jsonlines

from abc import ABCMeta, abstractclassmethod, abstractmethod

from starwhale.base.cloud import CloudRequestMixed
from starwhale.base.type import InstanceType, EvalTaskType, JobOperationType
from starwhale.base.uri import URI
from starwhale.consts import DEFAULT_PAGE_IDX, HTTPMethod, DEFAULT_PAGE_SIZE
from starwhale.utils.config import SWCliConfigMixed
from starwhale.utils.error import NoSupportError
from starwhale.utils.fs import ensure_dir, move_dir
from starwhale.utils.http import ignore_error
from starwhale.utils.process import check_call
from .executor import EvalExecutor
from .store import JobStorage


_device_id_map = {"cpu": 1, "gpu": 2}


class Job(object):
    __metaclass__ = ABCMeta

    def __init__(self, uri: URI) -> None:
        self.uri = uri
        self.name = uri.object.name
        self.project_name = uri.project
        self.sw_config = SWCliConfigMixed()

    @abstractmethod
    def create(
        self,
        model_uri: str,
        dataset_uris: t.List[str],
        runtime_uri: str,
        **kw: t.Any,
    ) -> t.Tuple[bool, str]:
        raise NotImplementedError

    @abstractmethod
    def info(
        self, page: int = DEFAULT_PAGE_IDX, size: int = DEFAULT_PAGE_SIZE
    ) -> t.Dict[str, t.Any]:
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

    @abstractclassmethod
    def list(
        cls,
        project_uri: str = "",
        page: int = DEFAULT_PAGE_IDX,
        size: int = DEFAULT_PAGE_SIZE,
    ) -> t.Tuple[t.List[t.Dict[str, t.Any]], t.Dict[str, t.Any]]:
        _uri = URI(project_uri)
        if _uri.instance_type == InstanceType.STANDALONE:
            return StandaloneJob.list(_uri)
        elif _uri.instance_type == InstanceType.CLOUD:
            return CloudJob.list(_uri, page, size)
        else:
            raise NoSupportError(f"{_uri}")

    @classmethod
    def get_job(cls, job_uri: URI) -> Job:
        if job_uri.instance_type == InstanceType.STANDALONE:
            return StandaloneJob(job_uri)
        elif job_uri.instance_type == InstanceType.CLOUD:
            return CloudJob(job_uri)
        else:
            raise NoSupportError(f"{job_uri}")


# TODO: Storage Class Mixed
class StandaloneJob(Job):
    def __init__(self, uri: URI) -> None:
        super().__init__(uri)
        self.store = JobStorage(uri)

    def create(
        self,
        model_uri: str,
        datasets_uri: t.List[str],
        runtime_uri: str,
        name: str = "",
        desc: str = "",
        gencmd: bool = False,
        docker_verbose: bool = False,
        phase: str = EvalTaskType.ALL,
        **kw: t.Any,
    ) -> t.Tuple[bool, str]:
        # TODO: support another job type
        EvalExecutor(
            model_uri,
            datasets_uri,
            self.store,
            runtime_uri,
            name=name,
            desc=desc,
            gencmd=gencmd,
            docker_verbose=docker_verbose,
        ).run(phase)
        return True, "run standalone eval job successfully"

    def info(
        self, page: int = DEFAULT_PAGE_IDX, size: int = DEFAULT_PAGE_SIZE
    ) -> t.Dict[str, t.Any]:

        report = {}
        with jsonlines.open(str(self.store.eval_report_path.resolve()), "r") as _reader:
            for _report in _reader:
                if not _report or not isinstance(_report, dict):
                    continue

                report = _report
                break

        return {
            "manifest": self.store.mainfest,
            "report": report,
            "location": {
                "ppl": self.store.ppl_dir,
                "cmp": self.store.cmp_dir,
            },
        }

    def remove(self, force: bool = False) -> t.Tuple[bool, str]:
        ensure_dir(self.store.recover_loc.parent)
        return move_dir(self.store.loc, self.store.recover_loc, force)

    def recover(self, force: bool = False) -> t.Tuple[bool, str]:
        ensure_dir(self.store.loc.parent)
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

        # TODO: search container first
        cmd += [f"{self.name}-{EvalTaskType.PPL}", f"{self.name}-{EvalTaskType.CMP}"]
        try:
            check_call(cmd)
        except Exception as e:
            return False, f"failed to {action} container, reason: {e}"
        else:
            return True, f"run {action} successfully"

    @classmethod
    def list(
        cls,
        project_uri: URI,
    ) -> t.Tuple[t.List[t.Dict[str, t.Any]], t.Dict[str, t.Any]]:
        _rt = []
        for _mf in JobStorage.iter_all_jobs(project_uri):
            _rt.append(
                {
                    "location": str(_mf.absolute()),
                    "manifest": yaml.safe_load(_mf.open()),
                }
            )
        return _rt, {}


class CloudJob(Job, CloudRequestMixed):
    def __init__(self, uri: URI) -> None:
        super().__init__(uri)

    def create(
        self,
        model_uri: str,
        dataset_uris: t.List[str],
        runtime_uri: str,
        device: str,
        **kw: t.Any,
    ) -> t.Tuple[bool, str]:
        _did, _dcnt = self._parse_device(device)

        # TODO: use argument for uri
        return self.do_http_request_simple_ret(
            f"/project/{self.project_name}/job",
            method=HTTPMethod.POST,
            data=json.dumps(
                {
                    "modelVersionId": model_uri,
                    "datasetVersionIds": ",".join([str(i) for i in dataset_uris]),
                    "baseImageId": runtime_uri,
                    "deviceId": _did,
                    "deviceAmount": _dcnt,
                }
            ),
        )

    def info(
        self, page: int = DEFAULT_PAGE_IDX, size: int = DEFAULT_PAGE_SIZE
    ) -> t.Dict[str, t.Any]:
        return {
            "tasks": self._fetch_tasks(page, size),
            "report": self._fetch_job_report(),
        }

    def remove(self, force: bool = False) -> t.Tuple[bool, str]:
        # TODO: support force remove
        return self.do_http_request_simple_ret(
            f"/project/{self.uri.project}/job/{self.name}",
            method=HTTPMethod.DELETE,
            instance_uri=self.uri,
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
            f"/project/{self.uri.project}/job/{self.name}/action/{action}",
            method=HTTPMethod.POST,
            instance_uri=self.uri,
        )

    @classmethod
    @ignore_error(([], {}))
    def list(
        cls,
        project_uri: URI,
        page: int = DEFAULT_PAGE_IDX,
        size: int = DEFAULT_PAGE_SIZE,
    ) -> t.Tuple[t.List[t.Dict[str, t.Any]], t.Dict[str, t.Any]]:
        crm = CloudRequestMixed()

        r = crm.do_http_request(
            f"/project/{project_uri.project}/job",
            params={"pageNum": page, "pageSize": size},
            instance_uri=project_uri,
        ).json()

        jobs = []
        for j in r["data"]["list"]:
            j.pop("owner", None)
            j["created_at"] = crm.fmt_timestamp(j["createdTime"])
            j["finished_at"] = crm.fmt_timestamp(j["stopTime"])
            j["duration_str"] = crm.fmt_duration(j["duration"])
            jobs.append({"manifest": j})

        return jobs, crm.parse_pager(r)

    def _parse_device(self, device: str) -> t.Tuple[int, int]:
        _t = device.split(":")
        _id = _device_id_map.get(_t[0].lower(), _device_id_map["cpu"])
        _cnt = int(_t[1]) if len(_t) == 2 else 1
        return _id, _cnt

    @ignore_error({})
    def _fetch_job_report(self) -> t.Dict[str, t.Any]:
        r = self.do_http_request(
            f"/project/{self.project_name}/job/{self.name}/result",
            instance_uri=self.uri,
        ).json()
        return r["data"]  # type: ignore

    @ignore_error(([], {}))
    def _fetch_tasks(
        self,
        page: int = DEFAULT_PAGE_IDX,
        size: int = DEFAULT_PAGE_SIZE,
    ) -> t.Tuple[t.List[t.Any], t.Dict[str, t.Any]]:
        r = self.do_http_request(
            f"/project/{self.project_name}/job/{self.name}/task",
            params={"pageNum": page, "pageSize": size},
            instance_uri=self.uri,
        ).json()

        tasks = []
        for _t in r["data"]["list"]:
            _t["created_at"] = _fmt_timestamp(_t["createdTime"])  # type: ignore
            tasks.append(_t)

        return tasks, self.parse_pager(r)
