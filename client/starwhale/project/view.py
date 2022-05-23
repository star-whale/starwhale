import typing as t

from starwhale.consts import DEFAULT_PAGE_IDX, DEFAULT_PAGE_SIZE


class ProjectTermView(object):
    def __init__(self, project_uri: str) -> None:
        pass

    def create(self) -> None:
        pass

    @classmethod
    def list(
        cls,
        instance_uri: str = "",
        page: int = DEFAULT_PAGE_IDX,
        size: int = DEFAULT_PAGE_SIZE,
        fullname: bool = False,
    ) -> None:
        pass

    def select(self) -> None:
        pass

    def remove(self) -> None:
        pass

    def recover(self) -> None:
        pass

    def info(self) -> None:
        pass


# TODO: add ProjectHTTPView for http request
