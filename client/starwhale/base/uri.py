import typing as t

from pathlib import Path

from urllib.parse import urlparse

from starwhale.utils.config import SWCliConfigMixed
from starwhale.utils.error import URIFormatError
from starwhale.utils import validate_obj_name
from starwhale.consts import (
    SW_API_VERSION,
    VERSION_PREFIX_CNT,
    UserRoleType,
    STANDALONE_INSTANCE,
)
from starwhale.base.type import URIType
from .type import InstanceType


class ObjField(object):
    def __init__(
        self, typ: str = URIType.UNKNOWN, name: str = "", version: str = ""
    ) -> None:
        self.typ = typ
        self.name = name
        self.version = version

    def __str__(self) -> str:
        return f"{self.typ}-{self.name}-{self.version}"


class URI(object):
    def __init__(self, raw: str, expected_type: str = URIType.UNKNOWN) -> None:
        self.raw = raw.strip().strip("/").lower()
        self._sw_config = SWCliConfigMixed()

        self.expected_type = expected_type
        self.full_uri = self.raw

        self.instance = ""
        self.instance_type = ""
        self.project = ""
        self.object = ObjField()

        self._do_parse()

    def __str__(self) -> str:
        return self.full_uri

    def __repr__(self) -> str:
        return f"instance:{self.instance}, instance_type:{self.instance_type}, projec:{self.project}, object:{self.object}"

    def _do_parse_instance_uri(self, raw: str) -> t.Tuple[str, str, str]:
        _remain: str = raw
        _inst: str = ""
        _inst_type: str = ""

        if not raw:
            return (
                self._sw_config._current_instance_obj["uri"],
                self._sw_config._current_instance_obj["type"],
                "",
            )

        if self.expected_type == URIType.INSTANCE:
            ok, reason = validate_obj_name(raw)
            if not ok:
                raise Exception(reason)

            return (
                raw,
                InstanceType.STANDALONE
                if raw == STANDALONE_INSTANCE
                else InstanceType.CLOUD,
                "",
            )

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
            return self._sw_config.current_project, ""

        if self.expected_type == URIType.PROJECT and not raw.startswith(
            URIType.PROJECT + "/"
        ):
            ok, reason = validate_obj_name(raw)
            if not ok:
                raise Exception(reason)
            return raw, ""

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

        _all_types = (
            URIType.DATASET,
            URIType.INSTANCE,
            URIType.MODEL,
            URIType.JOB,
            URIType.RUNTIME,
        )

        if self.expected_type in _all_types and not raw.startswith(
            self.expected_type + "/"
        ):
            if "/version/" in raw:
                _name, _version = raw.split("/version/")
            else:
                _name, _version = raw, ""

            ok, reason = validate_obj_name(_name)
            if not ok:
                raise Exception(reason)

            return ObjField(typ=self.expected_type, name=_name, version=_version), ""

        _sp = raw.split("/")
        if _sp[0] not in _all_types or len(_sp) < 2:
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

    @property
    def sw_instance_config(self) -> t.Dict[str, t.Any]:
        return self._sw_config.get_sw_instance_config(self.instance)

    @property
    def sw_token(self) -> str:
        return self.sw_instance_config.get("sw_token", "")

    @property
    def sw_remote_addr(self) -> str:
        return self.sw_instance_config.get("uri", "")

    @property
    def user_name(self) -> str:
        return self.sw_instance_config.get("user_name", "")

    @property
    def user_role(self) -> str:
        return self.sw_instance_config.get("user_role", UserRoleType.NORMAL)

    @classmethod
    def capsulate_uri_str(
        cls,
        instance: str,
        project: str = "",
        obj_type: str = "",
        obj_name: str = "",
        obj_ver: str = "",
    ) -> str:
        _fmt = lambda x: x.strip().strip("/").lower()

        _rt = f"{_fmt(instance)}"  # type: ignore
        if project:
            _rt = f"{_rt}/project/{project}"
        else:
            return _rt

        if obj_name and obj_type:
            _rt = f"{_rt}/{obj_type}/{obj_name}"
        else:
            return _rt

        if obj_ver:
            _rt = f"{_rt}/version/{obj_ver}"

        return _rt

    @classmethod
    def capsulate_uri(
        cls,
        instance: str,
        project: str = "",
        obj_type: str = "",
        obj_name: str = "",
        obj_ver: str = "",
    ) -> "URI":
        _uri = cls.capsulate_uri_str(instance, project, obj_type, obj_name, obj_ver)
        return cls(_uri)
