import typing as t

from starwhale.consts import DEFAULT_PAGE_IDX, DEFAULT_PAGE_SIZE
from starwhale.base.uri import URI
from starwhale.base.type import URIType
from starwhale.base.cloud import CloudRequestMixed
from starwhale.utils.http import ignore_error


class CloudInstance(CloudRequestMixed):
    def __init__(self, uri: str) -> None:
        self.uri = URI(uri, expected_type=URIType.INSTANCE)

    @ignore_error("--")
    def _fetch_version(self) -> str:
        return self.do_http_request("/system/version", instance_uri=self.uri).json()[
            "data"
        ]["version"]

    @ignore_error([])
    def _fetch_agents(self) -> t.List[t.Dict[str, t.Any]]:
        # TODO: add pageSize to args
        return self.do_http_request(
            "/system/agent",
            params={"pageSize": DEFAULT_PAGE_SIZE, "pageNum": DEFAULT_PAGE_IDX},
            instance_uri=self.uri,
        ).json()["data"]["list"]

    @ignore_error({})
    def _fetch_current_user(self) -> t.Dict[str, t.Any]:
        r = self.do_http_request("/user/current", instance_uri=self.uri).json()["data"]
        return dict(name=r["name"], role=r["role"]["roleName"])
