
import typing as t
from datetime import datetime, timedelta
import yaml
from collections import namedtuple

import requests

from starwhale.utils.config import SWCliConfigMixed
from starwhale.consts import SW_API_VERSION, HTTP_METHOD
from starwhale.utils.http import wrap_sw_error_resp, ignore_error
from starwhale.consts import FMT_DATETIME, SHORT_VERSION_CNT

_DEFAULT_TIMEOUT_SECS = 90

#TODO: tune page size
_SHOW_ALL = 100
DEFAULT_PAGE_NUM = 1
DEFAULT_PAGE_SIZE = 20

_fmt_timestamp = lambda x: datetime.fromtimestamp(float(x) / 1000.0).strftime(FMT_DATETIME)
_fmt_duration = lambda x: str(timedelta(milliseconds=float(x)))
PROJECT_OBJ_TYPE = namedtuple("PROJECT_OBJ_TYPE", ["MODEL", "DATASET"])(
    "model", "dataset"
)

#TODO: use model-view-control mode to refactor Cluster
class ClusterModel(SWCliConfigMixed):

    def __init__(self, swcli_config: t.Union[dict, None] = None) -> None:
        super().__init__(swcli_config)

    def request(self, path: str, method: str=HTTP_METHOD.GET, **kw: dict) -> requests.Response:
        _url = f"{self.sw_remote_addr}/api/{SW_API_VERSION}/{path.lstrip('/')}"
        r = requests.request(method, _url,
                             timeout=_DEFAULT_TIMEOUT_SECS,
                             verify=False,
                             headers={"Authorization": self._sw_token},
                             **kw)
        wrap_sw_error_resp(r, path, exit=False, use_raise=False, slient=True)
        return r

    @ignore_error([])
    def _fetch_baseimage(self) -> t.List[str]:
        r = self.request("/runtime/baseImage")
        return [i["name"] for i in r.json().get("data", []) if i.get("name")]

    @ignore_error("--")
    def _fetch_version(self) -> str:
        return self.request("/system/version").json()["data"]["version"]

    @ignore_error([])
    def _fetch_agents(self) -> t.List[dict]:
        #TODO: add pageSize to args
        return self.request("/system/agent", params={"pageSize": DEFAULT_PAGE_SIZE, "pageNum": DEFAULT_PAGE_NUM}).json()["data"]["list"]

    @ignore_error({})
    def _fetch_current_user(self) -> dict:
        r = self.request("/user/current").json()["data"]
        return dict(name=r["name"], role=r["role"]["roleName"])

    @ignore_error(([], {}))
    def _fetch_projects(self, user_name:str="", page: int=DEFAULT_PAGE_NUM, size: int=DEFAULT_PAGE_SIZE) -> t.Tuple[t.List[dict], dict]:
        #TODO: user params for project api
        r = self.request("/project", params={"pageNum": page, "pageSize": size, "userName": user_name}).json()
        projects = []
        for _p in r["data"]["list"]:
            owner = _p["owner"]["name"]
            if user_name != "" and user_name != owner:
                continue

            projects.append(
                dict(
                    id=_p["id"],
                    name=_p["name"],
                    created_at=_fmt_timestamp(_p["createTime"]),
                    is_default=_p["isDefault"],
                    owner=owner,
                )
            )
        return projects, self._parse_pager(r)

    def _parse_pager(self, resp: dict) -> dict:
        _d = resp["data"]
        return dict(
            total=_d["total"],
            current=_d["size"],
            remain=_d["total"] - _d["size"],
        )

    @ignore_error({})
    def _inspect_project(self, pid: int, versions_size: int=10) -> dict:
        return {
            "models": self._fetch_project_objects(pid, "model", versions_size),
            "datasets": self._fetch_project_objects(pid, "dataset", versions_size)
        }

    @ignore_error([])
    def _fetch_model_files(self, pid: int, mid: int) -> t.List:
        r = self.request(f"/project/{pid}/model/{mid}")
        return r.json()["data"]["files"]

    @ignore_error([])
    def _fetch_project_objects(self, pid: int, typ: str, versions_size: int=10) -> t.List[dict]:
        r = self.request(f"/project/{pid}/{typ}", params={"pageSize": _SHOW_ALL})

        ret = []
        for _m in r.json()["data"]["list"]:
            _m["created_at"] = _fmt_timestamp(_m.pop("createTime")),
            _m.pop("owner", None)

            mvr = self.request(f"/project/{pid}/{typ}/{_m['id']}/version", params={"pageSize": versions_size})
            versions = []
            for _v in mvr.json()["data"]["list"]:
                _v["short_name"] = _v["name"][:SHORT_VERSION_CNT]
                _v["created_at"] = _fmt_timestamp(_v.pop("createTime"))
                _v.pop("owner", None)
                if typ == PROJECT_OBJ_TYPE.DATASET:
                    _v["meta"] = yaml.safe_load(_v["meta"])
                versions.append(_v)

            _m["latest_versions"] = versions
            if typ == PROJECT_OBJ_TYPE.MODEL:
                _m["files"] = self._fetch_model_files(pid, _m["id"])
            ret.append(_m)

        return ret

    @ignore_error(([], {}))
    def _fetch_jobs(self, project: int, page: int=DEFAULT_PAGE_NUM, size: int=DEFAULT_PAGE_SIZE) -> t.Tuple[t.List[dict], dict]:
        r = self.request(f"/project/{project}/job", params={"pageNum": page, "pageSize": size}).json()
        jobs = []

        for j in r["data"]["list"]:
            j.pop("owner", None)
            j["created_at"] = _fmt_timestamp(j['createTime'])
            j["finished_at"] = _fmt_timestamp(j['stopTime'])
            j["duration_str"] = _fmt_duration(j['duration'])
            j["short_model_version"] = j["modelVersion"][:SHORT_VERSION_CNT]

            jobs.append(j)

        return jobs, self._parse_pager(r)

    @ignore_error(([], {}))
    def _fetch_tasks(self, project: int, job: int, page: int=DEFAULT_PAGE_NUM, size: int=DEFAULT_PAGE_SIZE) -> t.Tuple[t.List[dict], dict]:
        r = self.request(f"/project/{project}/job/{job}/task", params={"pageNum": page, "pageSize": size}).json()

        tasks = []
        for t in r["data"]["list"]:
            t["created_at"] = _fmt_timestamp(t["startTime"])
            tasks.append(t)

        return tasks, self._parse_pager(r)

    @ignore_error({})
    def _fetch_job_report(self, project: int, job: int) -> dict:
        r = self.request(f"/project/{project}/job/{job}/result").json()
        return r["data"]
