from __future__ import annotations

import json
import typing as t
from abc import ABCMeta, abstractmethod
from http import HTTPStatus
from collections import defaultdict

import jsonlines

from starwhale.utils import load_yaml
from starwhale.consts import HTTPMethod, DEFAULT_PAGE_IDX, DEFAULT_PAGE_SIZE
from starwhale.base.uri import URI
from starwhale.utils.fs import move_dir
from starwhale.base.type import EvalTaskType, InstanceType, JobOperationType
from starwhale.base.cloud import CloudRequestMixed
from starwhale.utils.http import ignore_error
from starwhale.utils.error import NotFoundError, NoSupportError
from starwhale.utils.config import SWCliConfigMixed
from starwhale.utils.process import check_call
from starwhale.core.runtime.process import Process as RuntimeProcess

from .store import JobStorage
from .executor import EvalExecutor

_device_id_map = {"cpu": 1, "gpu": 2}


class Job(metaclass=ABCMeta):
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

    @abstractmethod
    def compare(self, jobs: t.List[Job]) -> t.Dict[str, t.Any]:
        raise NotImplementedError

    @abstractmethod
    def _get_report(self) -> t.Dict[str, t.Any]:
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
        use_docker = kw.get("use_docker", False)
        phase = kw.get("phase", EvalTaskType.ALL)

        ee = EvalExecutor(
            model_uri=model_uri,
            dataset_uris=dataset_uris,
            project_uri=project_uri,
            runtime_uri=runtime_uri,
            name=name,
            desc=desc,
            gencmd=kw.get("gencmd", False),
            use_docker=use_docker,
        )
        if runtime_uri and not use_docker:
            RuntimeProcess.from_runtime_uri(
                uri=runtime_uri,
                target=ee.run,
                args=(phase,),
                runtime_restore=kw.get("runtime_restore", False),
            ).run()
        else:
            ee.run(phase)

        return True, ee._version

    def _get_report(self) -> t.Dict[str, t.Any]:
        report = {}
        with jsonlines.open(str(self.store.eval_report_path.resolve()), "r") as _reader:
            for _report in _reader:
                if not _report or not isinstance(_report, dict):
                    continue

                report = _report
                break
        return report

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

    def compare(self, jobs: t.List[Job]) -> t.Dict[str, t.Any]:
        rt = {}
        base_report = self._get_report()
        compare_reports = [j._get_report() for j in jobs]

        rt = {
            "kind": base_report["kind"],
            "base": {
                "uri": self.uri.full_uri,
                "version": self.uri.object.name,
            },
            "versions": [self.uri.object.name] + [j.uri.object.name for j in jobs],
            "summary": defaultdict(list),
            "charts": defaultdict(list),
        }

        _base_summary = {}
        _number_types = (int, float, complex)

        for _idx, _report in enumerate([base_report] + compare_reports):
            _summary = _report.get("summary", {})
            if "labels" in _report:
                _summary["labels"] = _report["labels"]

            _flat_summary = self._do_flatten_summary(_summary)
            if _idx == 0:
                _base_summary = _flat_summary

            for _bk, _bv in _base_summary.items():
                _cv = _flat_summary.get(_bk)

                if isinstance(_cv, _number_types) and isinstance(_bv, _number_types):
                    _delta = _cv - _bv
                else:
                    _delta = None
                rt["summary"][_bk].append(
                    {"value": _cv, "delta": _delta, "base": _idx == 0}
                )

            for _k, _v in _report.items():
                if _k in ("kind", "labels", "summary"):
                    continue
                rt["charts"][_k].append(_v)

        return rt

    def info(
        self, page: int = DEFAULT_PAGE_IDX, size: int = DEFAULT_PAGE_SIZE
    ) -> t.Dict[str, t.Any]:

        return {
            "manifest": self.store.manifest,
            "report": self._get_report(),
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
        for _path, _is_removed in JobStorage.iter_all_jobs(project_uri):
            _manifest = load_yaml(_path)
            if not _manifest:
                continue

            _rt.append(
                {
                    "location": str(_path.absolute()),
                    "manifest": _manifest,
                    "is_removed": _is_removed,
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
                    "modelVersionUrl": model_uri,
                    "datasetVersionUrls": ",".join([str(i) for i in dataset_uris]),
                    "runtimeVersionUrl": runtime_uri,
                    "device": _did,
                    "deviceAmount": _dcnt,
                }
            ),
        )
        if r.status_code == HTTPStatus.OK:
            return True, r.json()["data"]
        else:
            return False, r.json()["message"]

    @ignore_error({})
    def info(
        self, page: int = DEFAULT_PAGE_IDX, size: int = DEFAULT_PAGE_SIZE
    ) -> t.Dict[str, t.Any]:
        if not self.uri.project:
            raise NotFoundError("no selected project")
        return {
            "tasks": self._fetch_tasks(page, size),
            "report": self._fetch_job_report(),
            "manifest": self._fetch_job_info(),
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
        if not project_uri.project:
            raise NotFoundError("no selected project")
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

    @ignore_error({})
    def _fetch_job_info(self) -> t.Dict[str, t.Any]:
        r = self.do_http_request(
            f"/project/{self.project_name}/job/{self.name}",
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

    def _get_report(self) -> t.Dict[str, t.Any]:
        # TODO: need to implement it
        return {}

    def compare(self, jobs: t.List[Job]) -> t.Dict[str, t.Any]:
        # TODO: need to implement it
        return {}
