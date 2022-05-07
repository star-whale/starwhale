from http import HTTPStatus
import sys
import typing as t
from pathlib import Path
from functools import wraps

from loguru import logger
import requests
from requests_toolbelt.multipart.encoder import MultipartEncoder

from rich import print as rprint
from rich.panel import Panel

_UPLOAD_CHUNK_SIZE = 20 * 1024 * 1024


def wrap_sw_error_resp(r :requests.Response, header: str,
                       exit: bool=False, use_raise: bool=False,
                       slient: bool=False) -> None:

    _rprint = lambda x:x if slient else rprint
    if r.status_code == HTTPStatus.OK:
        _rprint(f" :clap: {header} success")
        return

    _rprint(f":fearful: {header} failed")
    msg = f"http status code: {r.status_code} \n"

    try:
        _resp = r.json()
    except Exception:
        msg += f"error message: {r.text} \n"
    else:
        msg += f"starwhale code: {_resp['code']} \n"
        msg += f"error message: {_resp['message']}"
    finally:
        _rprint(Panel.fit(msg, title=":space_invader: error details"))
        if exit:
            sys.exit(1)

        if use_raise:
            r.raise_for_status()


def upload_file(url: str, fpath: t.Union[str, Path], fields: dict={}, headers: dict={}, exit: bool=False, use_raise: bool=False) -> requests.Response:
    #TODO: add progress bar and rich live
    #TODO: add more push log
    #TODO: use head first to check swmp exists

    _headers = headers.copy()
    fpath = Path(fpath)
    fields["file"] = (fpath.name, fpath.open("rb"), "text/plain")

    _en = MultipartEncoder(fields=fields)
    #default chunk is 8192 Bytes
    _en._read = _en.read #type: ignore
    _en.read = lambda size: _en._read(_UPLOAD_CHUNK_SIZE) #type: ignore

    _headers["Content-Type"] = _en.content_type

    r = requests.post(url, data=_en, headers=_headers, timeout=1200) #type: ignore
    if r.status_code != HTTPStatus.OK:
        wrap_sw_error_resp(r, "upload failed", exit=exit)
    return r


def ignore_error(default_ret: t.Any=""):
    def _decorator(func):
        @wraps(func)
        def _wrapper(*args, **kwargs):
            try:
                return func(*args, **kwargs)
            except Exception as e:
                logger.warning(f"{func} error: {e}")
                return default_ret
        return _wrapper
    return _decorator