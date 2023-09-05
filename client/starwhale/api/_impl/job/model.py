from __future__ import annotations

import typing as t
from datetime import datetime
from dataclasses import field, dataclass

from starwhale.consts import (
    FMT_DATETIME,
    DEFAULT_PAGE_IDX,
    DEFAULT_PAGE_SIZE,
    MINI_FMT_DATETIME,
    FMT_DATETIME_NO_TZ,
)
from starwhale.api._impl import wrapper
from starwhale.utils.error import NotFoundError
from starwhale.core.job.model import LocalJobInfo
from starwhale.base.models.job import JobManifest
from starwhale.base.uri.project import Project
from starwhale.base.uri.resource import Resource, ResourceType


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
        self,
        id: str,
        datastore_uuid: str,
        project: Project,
        status: str,
        handler_name: str,
        model: _BundleInfo | None,
        runtime: _BundleInfo | None = None,
        datasets: t.List[_BundleInfo] | None = None,
        resource_pool: str = "",
        raw_manifest: JobManifest | t.Dict | None = None,
        created_at: str = "",
        finished_at: str = "",
    ) -> None:
        self.id = id
        self.datastore_uuid = datastore_uuid
        self.project = project
        self.instance = self.project.instance
        self.status = status.lower()
        self.handler_name = handler_name
        self.model = model
        self.runtime = runtime
        self.datasets = datasets or []
        self.resource_pool = resource_pool
        self.raw_manifest = raw_manifest
        self.created_at = self._try_parse_datetime(created_at)
        self.finished_at = self._try_parse_datetime(finished_at)
        self.uri = Resource(uri=self.id, typ=ResourceType.job, project=self.project)
        self._evaluation_store = wrapper.Evaluation(
            eval_id=self.datastore_uuid,
            project=self.project.name,
            instance=self.instance.url,
        )

    def __str__(self) -> str:
        return f"Job[{self.id}] <{self.status}> {self.handler_name}"

    def __repr__(self) -> str:
        return f"Job[{self.id}] <{self.status}> {self.handler_name} project:{self.project}, model: {self.model}, datastore_uuid: {self.datastore_uuid}, uri: {self.uri}"

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
        raw_jobs, page_info = JobTermView.list(
            project_uri=project,
            fullname=True,
            page=page_index,
            size=page_size,
        )
        jobs: t.List[Job] = []
        for job in raw_jobs:
            manifest = (
                job.manifest if isinstance(job, LocalJobInfo) else job["manifest"]
            )
            jobs.append(cls.create_by_manifest(project, manifest))

        return jobs, page_info

    @classmethod
    def create_by_manifest(
        cls, project: str | Project, manifest: JobManifest | t.Dict
    ) -> Job:
        if not isinstance(project, Project):
            project = Project(project)

        if isinstance(manifest, JobManifest):
            name_or_id = manifest.version
            status = manifest.status
            handler_name = manifest.handler_name or ""
            resource_pool = ""
            # TODO: support runtime for standalone instance
            runtime = None
            # TODO: support version/tags for model and datasets
            datasets = [_BundleInfo(name=u) for u in manifest.datasets or []]
            if manifest.model is not None:
                model = _BundleInfo(name=manifest.model)
            else:
                model = None
            datastore_uuid = name_or_id
            created_at = manifest.created_at
            finished_at = manifest.finished_at
        else:
            name_or_id = manifest["id"]
            status = manifest["jobStatus"]
            handler_name = manifest["jobName"]
            resource_pool = manifest["resourcePool"]
            datastore_uuid = manifest["uuid"]
            created_at = manifest["created_at"]
            finished_at = manifest["finished_at"]

            def _t(_info: t.Dict) -> t.List[str]:
                tags = []
                if "alias" in _info and _info["alias"]:
                    tags.append(_info["alias"])
                if _info.get("latest"):
                    tags.append("latest")
                if "tags" in _info and _info["tags"]:
                    tags.extend(_info["tags"])
                return tags

            model = _BundleInfo(
                name=manifest["model"]["name"],
                version=manifest["model"]["version"]["name"],
                tags=_t(manifest["model"]["version"]),
            )

            runtime = _BundleInfo(
                name=manifest["runtime"]["name"],
                version=manifest["runtime"]["version"]["name"],
                tags=_t(manifest["runtime"]["version"]),
            )

            datasets = []
            for d in manifest.get("datasetList", []):
                datasets.append(
                    _BundleInfo(
                        name=d["name"],
                        version=d["version"]["name"],
                        tags=_t(d["version"]),
                    )
                )

        return Job(
            id=name_or_id,
            datastore_uuid=datastore_uuid,
            project=project,
            status=status,
            handler_name=handler_name,
            model=model,
            runtime=runtime,
            datasets=datasets,
            resource_pool=resource_pool,
            raw_manifest=manifest,
            created_at=created_at,
            finished_at=finished_at,
        )

    @classmethod
    def job(
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
        j3 = job(xm5wnup")
        ```
        """
        from starwhale.core.job.model import Job as JobModel

        _uri = Resource(uri, typ=ResourceType.job)
        manifest = JobModel.get_job(_uri)._fetch_job_info()
        if not manifest:
            raise NotFoundError(f"job not found: {uri}")
        return cls.create_by_manifest(_uri.project, manifest)

    get = job

    @property
    def tables(self) -> t.List[str]:
        """Get datastore table names of job."""
        return self._evaluation_store.get_tables()

    @property
    def summary(self) -> t.Dict[str, t.Any]:
        """Get job summary row of datastore."""
        return self._evaluation_store.get_summary_metrics()

    def asdict(self) -> t.Dict[str, t.Any]:
        """Get job all info as dict."""
        summary = {}
        for k, v in self.summary.items():
            if hasattr(v, "asdict"):
                v = v.asdict()
            summary[k] = v

        return {
            "tables": self.tables,
            "summary": summary,
            "run_info": {
                "status": self.status,
                "created_at": self.created_at.strftime(FMT_DATETIME)
                if self.created_at
                else None,
                "finished_at": self.finished_at.strftime(FMT_DATETIME)
                if self.finished_at
                else None,
                "resource_pool": self.resource_pool,
            },
            "basic_info": {
                "id": self.id,
                "datastore_uuid": self.datastore_uuid,
                "project": self.project.name,
                "instance": self.instance.url,
                "uri": str(self.uri),
            },
            "input_info": {
                "handler": self.handler_name,
                "model": self.model if self.model is None else str(self.model),
                "datasets": [str(d) for d in self.datasets if d],
                "runtime": self.runtime if self.runtime is None else str(self.runtime),
            },
        }

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
        return self._evaluation_store.get(
            table_name=name,
            start=start,
            end=end,
            keep_none=keep_none,
            end_inclusive=end_inclusive,
        )
