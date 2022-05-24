from http import HTTPStatus
import typing as t
from datetime import datetime, timedelta
import yaml
import json

import requests

from starwhale.utils.config import SWCliConfigMixed
from starwhale.consts import SW_API_VERSION, HTTPMethod
from starwhale.utils.http import wrap_sw_error_resp, ignore_error
from starwhale.consts import FMT_DATETIME, SHORT_VERSION_CNT

_DEFAULT_TIMEOUT_SECS = 90

# TODO: tune page size
_SHOW_ALL = 100
DEFAULT_PAGE_NUM = 1
DEFAULT_PAGE_SIZE = 20

_fmt_timestamp = lambda x: datetime.fromtimestamp(float(x) / 1000.0).strftime(
    FMT_DATETIME
)
_fmt_duration = lambda x: str(timedelta(milliseconds=float(x)))
_device_id_map = {"cpu": 1, "gpu": 2}


class ProjectObjType:
    MODEL = "model"
    DATASET = "dataset"


# TODO: use model-view-control mode to refactor Cluster
class ClusterModel(SWCliConfigMixed):
    def __init__(self, swcli_config: t.Optional[t.Dict[str, t.Any]] = None) -> None:
        super().__init__(swcli_config)

    def request(
        self, path: str, method: str = HTTPMethod.GET, **kw: t.Any
    ) -> requests.Response:
        _url = f"{self.sw_remote_addr}/api/{SW_API_VERSION}/{path.lstrip('/')}"
        r = requests.request(
            method,
            _url,
            timeout=_DEFAULT_TIMEOUT_SECS,
            verify=False,
            headers={
                "Authorization": self._sw_token,
                "Content-Type": "application/json",
            },
            **kw,
        )
        wrap_sw_error_resp(r, path, exit=False, use_raise=False, silent=True)
        return r

    def _request_create_job(
        self,
        project: int,
        model_id: int,
        dataset_ids: t.List[int],
        baseimage_id: int,
        device: str,
        name: str,
        desc: str,
    ) -> t.Tuple[bool, str]:
        _did, _dcnt = self._parse_device(device)

        r = self.request(
            f"/project/{project}/job",
            method=HTTPMethod.POST,
            data=json.dumps(
                {
                    "modelVersionId": model_id,
                    "datasetVersionIds": ",".join([str(i) for i in dataset_ids]),
                    "baseImageId": baseimage_id,
                    "deviceId": _did,
                    "deviceAmount": _dcnt,
                }
            ),
        )
        _rt = r.json()
        if r.status_code == HTTPStatus.OK:
            return True, _rt["data"]
        else:
            return False, _rt["message"]

    def _parse_device(self, device: str) -> t.Tuple[int, int]:
        _t = device.split(":")
        _id = _device_id_map.get(_t[0].lower(), _device_id_map["cpu"])
        _cnt = int(_t[1]) if len(_t) == 2 else 1
        return _id, _cnt

    @ignore_error([])
    def _fetch_baseimage(self) -> t.List[str]:
        r = self.request("/runtime/baseImage")
        return [
            f"[{i['id']}]{i['name']}" for i in r.json().get("data", []) if i.get("name")
        ]

    @ignore_error("--")
    def _fetch_version(self) -> str:
        return self.request("/system/version").json()["data"]["version"]

    @ignore_error([])
    def _fetch_agents(self) -> t.List[t.Dict[str, t.Any]]:
        # TODO: add pageSize to args
        return self.request(
            "/system/agent",
            params={"pageSize": DEFAULT_PAGE_SIZE, "pageNum": DEFAULT_PAGE_NUM},
        ).json()["data"]["list"]

    @ignore_error({})
    def _fetch_current_user(self) -> t.Dict[str, t.Any]:
        r = self.request("/user/current").json()["data"]
        return dict(name=r["name"], role=r["role"]["roleName"])

    def _parse_pager(self, resp: t.Dict[str, t.Any]) -> t.Dict[str, t.Any]:
        _d = resp["data"]
        return dict(
            total=_d["total"],
            current=_d["size"],
            remain=_d["total"] - _d["size"],
        )

    @ignore_error(([], {}))
    def _fetch_jobs(
        self, project: int, page: int = DEFAULT_PAGE_NUM, size: int = DEFAULT_PAGE_SIZE
    ) -> t.Tuple[t.List[dict], dict]:
        r = self.request(
            f"/project/{project}/job", params={"pageNum": page, "pageSize": size}
        ).json()
        jobs = []

        for j in r["data"]["list"]:
            j.pop("owner", None)
            j["created_at"] = _fmt_timestamp(j["createdTime"])  # type: ignore
            j["finished_at"] = _fmt_timestamp(j["stopTime"])  # type: ignore
            j["duration_str"] = _fmt_duration(j["duration"])  # type: ignore
            j["short_model_version"] = j["modelVersion"][:SHORT_VERSION_CNT]

            jobs.append(j)

        return jobs, self._parse_pager(r)

    @ignore_error(([], {}))
    def _fetch_tasks(
        self,
        project: int,
        job: int,
        page: int = DEFAULT_PAGE_NUM,
        size: int = DEFAULT_PAGE_SIZE,
    ) -> t.Tuple[t.List[t.Any], t.Dict[str, t.Any]]:
        r = self.request(
            f"/project/{project}/job/{job}/task",
            params={"pageNum": page, "pageSize": size},
        ).json()

        tasks = []
        for _t in r["data"]["list"]:
            _t["created_at"] = _fmt_timestamp(_t["createdTime"])  # type: ignore
            tasks.append(_t)

        return tasks, self._parse_pager(r)

    @ignore_error({})
    def _fetch_job_report(self, project: int, job: int) -> t.Dict[str, t.Any]:
        r = self.request(f"/project/{project}/job/{job}/result").json()
        return r["data"]  # type: ignore
