import typing as t
import requests
from http import HTTPStatus
from datetime import datetime, timedelta

from starwhale.consts import SW_API_VERSION, HTTPMethod, FMT_DATETIME
from starwhale.utils.http import wrap_sw_error_resp
from starwhale.base.uri import URI

_DEFAULT_TIMEOUT_SECS = 90


class CloudRequestMixed(object):
    def fmt_timestamp(self, ts: t.Union[float, str]) -> str:
        return datetime.fromtimestamp(float(ts) / 1000.0).strftime(FMT_DATETIME)

    def fmt_duration(self, ts: t.Union[float, str]) -> str:
        return str(timedelta(milliseconds=float(ts)))

    def do_http_request_simple_ret(
        self,
        path: str,
        method: str = HTTPMethod.GET,
        instance_uri: t.Optional[URI] = None,
        **kw: t.Any,
    ) -> t.Tuple[bool, str]:
        r = self.do_http_request(path, method, instance_uri, **kw)
        return r.status_code == HTTPStatus.OK, r.json()["message"]

    def do_http_request(
        self,
        path: str,
        method: str = HTTPMethod.GET,
        instance_uri: t.Optional[URI] = None,
        **kw: t.Any,
    ) -> requests.Response:
        instance_uri = instance_uri or URI("")
        _url = f"{instance_uri.instance}/api/{SW_API_VERSION}/{path.lstrip('/')}"

        r = requests.request(
            method,
            _url,
            timeout=_DEFAULT_TIMEOUT_SECS,
            verify=False,
            headers={
                "Authorization": instance_uri.sw_token,
                "Content-Type": "application/json",
            },
            **kw,
        )
        wrap_sw_error_resp(r, path, exit=False, use_raise=False, silent=True)
        return r

    def parse_pager(self, resp: t.Dict[str, t.Any]) -> t.Dict[str, t.Any]:
        _d = resp["data"]
        return dict(
            total=_d["total"],
            current=_d["size"],
            remain=_d["total"] - _d["size"],
        )
