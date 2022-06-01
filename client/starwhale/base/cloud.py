from copy import deepcopy
import typing as t
from http import HTTPStatus
from datetime import datetime, timedelta
from pathlib import Path

import requests
from requests_toolbelt.multipart.encoder import MultipartEncoder  # type: ignore

from starwhale.consts import SW_API_VERSION, HTTPMethod, FMT_DATETIME
from starwhale.utils.http import wrap_sw_error_resp
from starwhale.base.uri import URI

_TMP_FILE_BUFSIZE = 8192
_DEFAULT_TIMEOUT_SECS = 90
_UPLOAD_CHUNK_SIZE = 20 * 1024 * 1024


class CloudRequestMixed(object):
    def fmt_timestamp(self, ts: t.Union[float, str]) -> str:
        return datetime.fromtimestamp(float(ts) / 1000.0).strftime(FMT_DATETIME)

    def fmt_duration(self, ts: t.Union[float, str]) -> str:
        return str(timedelta(milliseconds=float(ts)))

    def do_download_file(
        self,
        url_path: str,
        dest_path: Path,
        instance_uri: t.Optional[URI] = None,
        **kw: t.Any,
    ) -> None:
        r = self.do_http_request(
            path=url_path,
            method=HTTPMethod.GET,
            instance_uri=instance_uri,
            use_raise=True,
            **kw,
        )

        with dest_path.open("wb") as f:
            for chunk in r.iter_content(chunk_size=_TMP_FILE_BUFSIZE):
                f.write(chunk)

    def do_multipart_upload_file(
        self,
        url_path: str,
        file_path: t.Union[str, Path],
        fields: t.Dict[str, t.Any] = {},
        instance_uri: t.Optional[URI] = None,
        headers: t.Dict[str, t.Any] = {},
        **kw: t.Any,
    ) -> requests.Response:
        # TODO: add progress bar and rich live

        _headers = deepcopy(headers)
        fpath = Path(file_path)
        fields["file"] = (fpath.name, fpath.open("rb"), "text/plain")

        _en = MultipartEncoder(fields=fields)
        # default chunk is 8192 Bytes
        _en._read = _en.read  # type: ignore
        _en.read = lambda size: _en._read(_UPLOAD_CHUNK_SIZE)  # type: ignore

        _headers["Content-Type"] = _en.content_type

        return self.do_http_request(
            url_path,
            method=HTTPMethod.POST,
            instance_uri=instance_uri,
            timeout=1200,
            data=_en,
            headers=_headers,
            **kw,
        )

    def do_http_request_simple_ret(
        self,
        path: str,
        method: str = HTTPMethod.GET,
        instance_uri: t.Optional[URI] = None,
        **kw: t.Any,
    ) -> t.Tuple[bool, str]:
        r = self.do_http_request(path, method, instance_uri, **kw)
        status = r.status_code == HTTPStatus.OK

        try:
            message = r.json()["message"]
        except Exception as e:
            message = r.text or str(e)

        return status, message

    def do_http_request(
        self,
        path: str,
        method: str = HTTPMethod.GET,
        instance_uri: t.Optional[URI] = None,
        timeout: int = _DEFAULT_TIMEOUT_SECS,
        headers: t.Dict[str, t.Any] = {},
        disable_default_content_type: bool = False,
        **kw: t.Any,
    ) -> requests.Response:
        instance_uri = instance_uri or URI("")
        _url = f"{instance_uri.instance}/api/{SW_API_VERSION}/{path.lstrip('/')}"
        _headers = {
            "Authorization": instance_uri.sw_token,
        }
        if not disable_default_content_type:
            _headers["Content-Type"] = "application/json"

        _headers.update(headers)

        use_raise = kw.pop("use_raise", False)
        ignore_status_codes = kw.pop("ignore_status_codes", [])

        r = requests.request(
            method,
            _url,
            timeout=timeout,
            verify=False,
            headers=_headers,
            **kw,
        )
        wrap_sw_error_resp(
            r,
            path,
            use_raise=use_raise,
            silent=True,
            ignore_status_codes=ignore_status_codes,
        )
        return r

    def parse_pager(self, resp: t.Dict[str, t.Any]) -> t.Dict[str, t.Any]:
        _d = resp["data"]
        return dict(
            total=_d["total"],
            current=_d["size"],
            remain=_d["total"] - _d["size"],
        )
