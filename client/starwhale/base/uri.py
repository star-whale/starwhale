import typing as t

from pathlib import Path

from urllib.parse import urlparse

from starwhale.instance.model import InstanceType
from starwhale.utils.config import SWCliConfigMixed
from starwhale.utils.error import URIFormatError
from starwhale.consts import SW_API_VERSION, VERSION_PREFIX_CNT


class URIType:
    INSTANCE = "instance"
    PROJECT = "project"
    MODEL = "model"
    DATASET = "dataset"
    RUNTIME = "runtime"
    JOB = "job"
    UNKNOWN = "unknown"


class ObjField(t.NamedTuple):
    typ: str = ""
    name: str = ""
    version: str = ""


class URI(object):
    def __init__(self, raw: str) -> None:
        self.raw = raw.strip().strip("/")
        self._sw_config = SWCliConfigMixed()

        self.full_uri = self.raw

        self.instance = ""
        self.instance_type = ""
        self.project = ""
        self.object = ObjField()

        self._do_parse()

    def _do_parse_instance_uri(self, raw: str) -> t.Tuple[str, str, str]:
        _remain: str = raw
        _inst: str = ""
        _inst_type: str = ""

        if raw.startswith(("http://", "https://", "cloud://")):
            _up = urlparse(raw)
            if raw.startswith("cloud://"):
                _inst = (
                    self._sw_config._config["instances"]
                    .get(_up.netloc, {})
                    .get("uri", "")
                )
            else:
                _inst = f"{_up.scheme}://{_up.netloc}"
            _remain = _up.path
            _inst_type = InstanceType.CLOUD
        elif raw.startswith("local/") or raw == "local":
            _inst = "local"
            _sp = raw.split("local/", 1)
            _remain = "" if len(_sp) == 1 else _sp[1]
            _inst_type = InstanceType.STANDALONE

        if not _inst:
            _inst = self._sw_config._current_instance_obj["uri"]
            _inst_type = self._sw_config._current_instance_obj["type"]

        return _inst, _inst_type, _remain

    def _do_parse_project_uri(self, raw: str) -> t.Tuple[str, str]:
        raw = raw.strip().strip("/")
        if not raw:
            _proj = self._sw_config.current_project
            _remain = ""
        else:
            _sp = raw.split("/", 2)
            if _sp[0] == "project" and len(_sp) > 1:
                _proj = _sp[1]
                _remain = _sp[2] if len(_sp) == 3 else ""
            else:
                _proj = self._sw_config.current_project
                _remain = raw

        return _proj, _remain

    def _do_parse_obj_uri(self, raw: str) -> t.Tuple[ObjField, str]:
        raw = raw.strip().strip("/")

        if not raw:
            return ObjField(), raw

        _sp = raw.split("/")
        if (
            _sp[0]
            not in (
                URIType.DATASET,
                URIType.INSTANCE,
                URIType.MODEL,
                URIType.JOB,
                URIType.RUNTIME,
            )
            or len(_sp) < 2
        ):
            raise URIFormatError(f"raw: {self.raw}, failed to parse object")

        if len(_sp) >= 4 and _sp[2] == "version":
            _version = _sp[3]
            _remain = "/".join(_sp[4:])
        else:
            _version = ""
            _remain = "/".join(_sp[2:])

        return ObjField(typ=_sp[0], name=_sp[1], version=_version), _remain

    def _do_parse(self) -> None:
        _inst, _inst_type, _remain = self._do_parse_instance_uri(self.raw)
        _proj, _remain = self._do_parse_project_uri(_remain)
        _obj, _remain = self._do_parse_obj_uri(_remain)

        self.instance = _inst
        self.instance_type = _inst_type
        self.project = _proj
        self.object = _obj

        self.full_uri = _inst
        if _proj:
            self.full_uri = f"{self.full_uri}/project/{_proj}"

            if _obj.name:
                self.full_uri = f"{self.full_uri}/{_obj.typ}/{_obj.name}"

                if _obj.version:
                    self.full_uri = f"{self.full_uri}/version/{_obj.version}"

    @property
    def real_request_uri(self) -> t.Union[str, Path]:
        if self.instance_type == InstanceType.CLOUD:
            _up = urlparse(self.full_uri)
            return f"{_up.scheme}://{_up.netloc}/api/{SW_API_VERSION}{_up.path}"
        else:
            rt = self._sw_config.rootdir
            if self.project:
                rt = rt / self.project

                if self.object.name:
                    rt = rt / self.object.typ / self.object.name

                    if self.object.version:
                        rt = (
                            rt
                            / self.object.version[:VERSION_PREFIX_CNT]
                            / self.object.version
                        )

            return rt
