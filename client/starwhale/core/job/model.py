from __future__ import annotations
from http import HTTPStatus

import typing as t
import yaml
import json

import jsonlines

from abc import ABCMeta, abstractmethod

from starwhale.base.cloud import CloudRequestMixed
from starwhale.base.type import InstanceType, EvalTaskType, JobOperationType
from starwhale.base.uri import URI
from starwhale.consts import DEFAULT_PAGE_IDX, HTTPMethod, DEFAULT_PAGE_SIZE
from starwhale.utils.config import SWCliConfigMixed
from starwhale.utils.error import NoSupportError
from starwhale.utils.fs import move_dir
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

    @classmethod
    def create(
        cls,
        project_uri: URI,
        model_uri: str,
        dataset_uris: t.List[str],
        runtime_uri: str,
        name: str = "",
        desc: str = "",
        **kw: t.Any,
    ) -> t.Tuple[bool, str]:
        _cls = cls._get_job_cls(project_uri)
        return _cls.create(
            project_uri,
            model_uri,
            dataset_uris,
            runtime_uri,
            name,
            desc,
            **kw,
        )

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

    @classmethod
    def _get_job_cls(cls, uri: URI) -> t.Union[t.Type[StandaloneJob], t.Type[CloudJob]]:
        if uri.instance_type == InstanceType.STANDALONE:
            return StandaloneJob
        elif uri.instance_type == InstanceType.CLOUD:
            return CloudJob
        else:
            raise NoSupportError(f"job uri:{uri}")

    @classmethod
    def list(
        cls,
        project_uri: URI,
        page: int = DEFAULT_PAGE_IDX,
        size: int = DEFAULT_PAGE_SIZE,
    ) -> t.Tuple[t.List[t.Dict[str, t.Any]], t.Dict[str, t.Any]]:
        _cls = cls._get_job_cls(project_uri)
        return _cls.list(project_uri)

    @classmethod
    def get_job(cls, job_uri: URI) -> Job:
        _cls = cls._get_job_cls(job_uri)
        return _cls(job_uri)


# TODO: Storage Class Mixed
class StandaloneJob(Job):
    def __init__(self, uri: URI) -> None:
        super().__init__(uri)
        self.store = JobStorage(uri)

    @classmethod
    def create(
        cls,
        project_uri: URI,
        model_uri: str,
        dataset_uris: t.List[str],
        runtime_uri: str,
        name: str = "",
        desc: str = "",
        **kw: t.Any,
    ) -> t.Tuple[bool, str]:
        # TODO: support another job type
        EvalExecutor(
            model_uri,
            dataset_uris,
            project_uri.real_request_uri,  # type: ignore
            runtime_uri,
            name=name,
            desc=desc,
            gencmd=kw.get("gen_cmd", False),
            docker_verbose=kw.get("docker_verbose", False),
        ).run(kw.get("phase", EvalTaskType.ALL))
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
                "ppl": str(self.store.ppl_dir.absolute()),
                "cmp": str(self.store.cmp_dir.absolute()),
            },
        }

    def remove(self, force: bool = False) -> t.Tuple[bool, str]:
        return move_dir(self.store.loc, self.store.recover_loc, force)

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
        page: int = DEFAULT_PAGE_IDX,
        size: int = DEFAULT_PAGE_SIZE,
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

    @classmethod
    def create(
        cls,
        project_uri: URI,
        model_uri: str,
        dataset_uris: t.List[str],
        runtime_uri: str,
        name: str = "",
        desc: str = "",
        **kw: t.Any,
    ) -> t.Tuple[bool, str]:
        _did, _dcnt = cls.parse_device(kw["resource"])

        crm = CloudRequestMixed()
        # TODO: use argument for uri
        r = crm.do_http_request(
            f"/project/{project_uri.project}/job",
            method=HTTPMethod.POST,
            instance_uri=project_uri,
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
        if r.status_code == HTTPStatus.OK:
            return True, r.json()["data"]
        else:
            return False, r.json()["message"]

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
            f"/project/{self.uri.project}/job/{self.name}/{action}",
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

    @staticmethod
    def parse_device(device: str) -> t.Tuple[int, int]:
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
            _t["created_at"] = self.fmt_timestamp(_t["createdTime"])  # type: ignore
            tasks.append(_t)

        return tasks, self.parse_pager(r)
