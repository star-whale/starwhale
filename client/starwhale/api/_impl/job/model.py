from __future__ import annotations

import typing as t
from datetime import datetime
from dataclasses import field, dataclass

from starwhale.utils import NoSupportError
from starwhale.consts import (
    FMT_DATETIME,
    DEFAULT_PAGE_IDX,
    DEFAULT_PAGE_SIZE,
    MINI_FMT_DATETIME,
    FMT_DATETIME_NO_TZ,
)
from starwhale.api._impl import wrapper
from starwhale.base.models.job import LocalJobInfo, RemoteJobInfo
from starwhale.base.uri.project import Project
from starwhale.base.uri.resource import Resource, ResourceType
from starwhale.base.client.api.job import JobApi
from starwhale.base.client.models.models import JobVo


@dataclass
class _BundleInfo:
    name: str
    version: str = ""
    tags: list[str] = field(default_factory=list)

    def __str__(self) -> str:
        r = self.name
        if self.version:
            r = f"{r}/version/{self.version}"
        return r


class Job:
    def __init__(
        self, uri: Resource, basic_info: LocalJobInfo | JobVo | None = None
    ) -> None:
        self.uri = uri
        self._basic_info: LocalJobInfo | JobVo = basic_info or self._get_basic_info()
        self._evaluation_store: wrapper.Evaluation | None = None

    @property
    def evaluation_store(self) -> wrapper.Evaluation:
        if self._evaluation_store is None:
            info = self.info()
            if isinstance(info, LocalJobInfo):
                eval_id = info.manifest.version
            else:
                eval_id = info.job.uuid
            self._evaluation_store = wrapper.Evaluation(
                eval_id=eval_id,
                project=self.uri.project,
            )
        return self._evaluation_store

    @property
    def basic_info(self) -> LocalJobInfo | JobVo:
        return self._basic_info

    def _get_basic_info(self) -> LocalJobInfo | JobVo:
        if self.uri.instance.is_local:
            info = self.info()
            if isinstance(info, LocalJobInfo):
                return info
            else:
                # this can not happen, make mypy happy
                raise NoSupportError
        else:
            return (
                JobApi(self.uri.instance)
                .info(self.uri.project.id, self.uri.name)
                .raise_on_error()
                .response()
                .data
            )

    def __str__(self) -> str:
        return f"Job[{self.uri}]"

    __repr__ = __str__

    @staticmethod
    def _try_parse_datetime(s: str) -> datetime | None:
        s = s.strip()
        if not s:
            return None

        for time_fmt in (FMT_DATETIME_NO_TZ, FMT_DATETIME, MINI_FMT_DATETIME):
            try:
                return datetime.strptime(s, time_fmt)
            except ValueError:
                ...
        raise ValueError(f"can not parse datetime string: {s}")

    @classmethod
    def list(
        cls,
        project: str = "",
        page_index: int = DEFAULT_PAGE_IDX,
        page_size: int = DEFAULT_PAGE_SIZE,
    ) -> t.Tuple[t.List[Job], t.Dict]:
        """List jobs of a project.

        Arguments:
            project: (str, optional): project uri str. Standalone, Server or Cloud instance's project is ok.
              Default is current selected project.
            page_index: (int, optional): page index. Default is 1. Only available for Server and Cloud instance.
            page_size: (int, optional): page size. Default is 20. Only available for Server and Cloud instance.

        Returns:
            Tuple: (jobs list, pagination_info)

        Examples:

        ```python
        from starwhale import Job
        # list jobs of current selected project
        jobs, pagination_info = Job.list()
        # list jobs of starwhale/public project in the cloud.starwhale.cn instance
        jobs, pagination_info = Job.list("https://cloud.starwhale.cn/project/starwhale:public")
        # list jobs of id=1 project in the server instance, page index is 2, page size is 10
        jobs, pagination_info = Job.list("https://server/project/1", page_index=2, page_size=10)
        ```
        """
        from starwhale.core.job.view import JobTermView

        # TODO: support filters
        ls, page = JobTermView.list(
            project_uri=project,
            fullname=True,
            page=page_index,
            size=page_size,
        )
        jobs: t.List[Job] = []
        for i in ls:
            if isinstance(i, LocalJobInfo):
                uri = Resource(i.manifest.version, typ=ResourceType.job)
            elif isinstance(i, JobVo):
                uri = Resource(i.uuid, typ=ResourceType.job, project=Project(project))
            else:
                raise NoSupportError
            jobs.append(cls(uri, basic_info=i))

        return jobs, page

    @classmethod
    def get(
        cls,
        uri: str,
    ) -> Job:
        """Get job object by uri.

        Arguments:
            uri: (str, required): job uri str.

        Returns:
            Job Object

        Examples:
        ```python
        from starwhale import job
        # get job object of uri=https://server/job/1
        j1 = job("https://server/job/1")
        # get job from standalone instance
        j2 = job("local/project/self/job/xm5wnup")
        j3 = job("xm5wnup")
        ```
        """

        _uri = Resource(uri, typ=ResourceType.job)
        return cls(_uri)

    def info(self) -> LocalJobInfo | RemoteJobInfo:
        from starwhale.core.job.model import Job as JobModel

        return JobModel.get_job(self.uri).info()

    @property
    def tables(self) -> t.List[str]:
        """Get datastore table names of job."""
        return self.evaluation_store.get_tables()

    @property
    def summary(self) -> t.Dict[str, t.Any]:
        """Get job summary row of datastore."""
        return self.evaluation_store.get_summary_metrics()

    def get_table_rows(
        self,
        name: str,
        start: t.Any = None,
        end: t.Any = None,
        keep_none: bool = False,
        end_inclusive: bool = False,
    ) -> t.Iterator[t.Dict[str, t.Any]]:
        """Get table rows.

        If start and end is None, will get all rows.

        Arguments:
            name: (str, required): table name.
            start: (Any, optional): start key. Default is None.
            end: (Any): end key. Default is None.
            keep_none: (bool, optional): keep None value or not. Default is False.
            end_inclusive: (bool, optional): end key is inclusive or not. Default is False.

        Returns:
            A iterator of table rows.

        Examples:
        ```python
        from starwhale import job
        j = job("local/project/self/job/xm5wnup")
        table_name = j.tables[0]
        for row in j.get_table_rows(table_name):
            print(row)
        rows = list(j.get_table_rows(table_name, start=0, end=100))
        ```
        """
        return self.evaluation_store.get(
            table_name=name,
            start=start,
            end=end,
            keep_none=keep_none,
            end_inclusive=end_inclusive,
        )
