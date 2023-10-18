from __future__ import annotations

import typing as t
from datetime import datetime

from starwhale.utils import NoSupportError
from starwhale.consts import (
    FMT_DATETIME,
    DEFAULT_PROJECT,
    DefaultYAMLName,
    DEFAULT_PAGE_IDX,
    DEFAULT_PAGE_SIZE,
    MINI_FMT_DATETIME,
    FMT_DATETIME_NO_TZ,
    DEFAULT_RESOURCE_POOL,
)
from starwhale.base.models.job import LocalJobInfo, RemoteJobInfo
from starwhale.base.bundle_copy import BundleCopy
from starwhale.base.uri.project import Project
from starwhale.base.uri.resource import Resource, ResourceType
from starwhale.base.client.api.job import JobApi
from starwhale.api._impl.evaluation.log import Evaluation
from starwhale.base.client.models.models import JobVo


class Job:
    def __init__(self, uri: Resource) -> None:
        self.uri = uri
        self._evaluation_store: Evaluation | None = None

    @property
    def evaluation_store(self) -> Evaluation:
        if self._evaluation_store is None:
            info = self.info()
            if isinstance(info, LocalJobInfo):
                eval_id = info.manifest.version
            else:
                eval_id = info.job.uuid
            self._evaluation_store = Evaluation(
                id=eval_id,
                project=self.uri.project,
            )
        return self._evaluation_store

    @property
    def status(self) -> str:
        """Get job real-time status.

        status is one of: `CREATED`, `READY`, `PAUSED`, `RUNNING`, `CANCELLING`, `CANCELED`, `SUCCESS`, `FAIL` and `UNKNOWN`.

        Returns:
            str
        """
        info = self.basic_info
        if isinstance(info, LocalJobInfo):
            return info.manifest.status.upper()
        else:
            return info.job_status.value.upper()

    @property
    def basic_info(self) -> LocalJobInfo | JobVo:
        return self._get_basic_info()

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
            jobs.append(cls(uri))

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
        return self.evaluation_store.get_summary()

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
        return self.evaluation_store.scan(
            category=name,
            start=start,
            end=end,
            keep_none=keep_none,
            end_inclusive=end_inclusive,
        )

    @classmethod
    def create(
        cls,
        project: Project | str,
        model: Resource | str,
        run_handler: str,
        datasets: t.List[str | Resource] | None = None,
        runtime: Resource | str | None = None,
        resource_pool: str = DEFAULT_RESOURCE_POOL,
        ttl: int = 0,
        dev_mode: bool = False,
        dev_mode_password: str = "",
        dataset_head: int = 0,
        overwrite_specs: t.Dict[str, t.Any] | None = None,
    ) -> Job:
        """Create a job for Evaluation, Fine-tuning, Online Serving or Developing.

        We use `project` argument to determine which instance to create job.
            - For Server and Cloud instance, create process will be async, so you can get job object.
            - For Standalone instance, create process will be sync, so you can get job object after job finished.

        When `project` is related to a server/cloud instance, the model, runtime and dataset must be in the same instance.
        When `project` is related to a standalone instance, the model and dataset accepts both standalone and server/cloud instance.

        For the Standalone instance, the runtime use the python environment of the current process.

        For all instances arguments:
            project: (Project | str, required): project object or project uri str.
            model: (Resource | str, required): model object or model uri str.
            run_handler: (str, required): run handler name.
            datasets: (List[str | Resource], optional): dataset object list or dataset uri str list. Default is None.

        Only for Standalone instance arguments:
            dataset_head: (int, optional): dataset head size for debugging. Default is 0, means no head.

        Only for Server/Cloud instance arguments:
            runtime: (Resource | str, optional): runtime object or runtime uri str. Default is None.
                When runtime is None, will try the model built-in runtime first.
            resource_pool: (str, optional): resource pool name. Default is "default" pool.
            ttl: (int, optional): job time to live seconds. Default is 0, means no ttl.
            dev_mode: (bool, optional): dev mode or not. Default is False.
            dev_mode_password: (str, optional): dev mode password. Default is "".
            overwrite_specs: (Dict[str, Any], optional): overwrite handler specs, currently only support `replicas` and `resources` fields.
                Default is None. `replicas` and `resources` follow the @starwhale.handler specs. If the field is omitted, will use the model package defined value.

        Examples:

        - create a Cloud Instance job
        ```python
        from starwhale import Job
        project = "https://cloud.starwhale.cn/project/starwhale:public"
        job = Job.create(
            project=project,
            model=f"{project}/model/mnist/version/v0",
            run_handler="mnist.evaluator:MNISTInference.evaluate",
            datasets=[f"{project}/dataset/mnist/version/v0"],
            runtime=f"{project}/runtime/pytorch",
            overwrite_specs={
                "mnist.evaluator:MNISTInference.evaluate": {"resources": "4GiB"},
                "mnist.evaluator:MNISTInference.predict": {"resources": "8GiB", "replicas": 10}
            }
        )
        print(job.status)
        ```

        - create a Standalone Instance job
        ```python
        from starwhale import Job
        job = Job.create(
            project="self",
            model="mnist",
            run_handler="mnist.evaluator:MNISTInference.evaluate",
            datasets=["mnist"],
        )
        print(job.status)
        ```
        Returns:
            Job Object
        """
        if isinstance(project, str):
            project = Project(project)

        if project.instance.is_local:
            return cls.create_local(
                project=project,
                model=model,
                run_handler=run_handler,
                datasets=datasets,
                dataset_head=dataset_head,
            )
        else:
            return cls.create_remote(
                project=project,
                model=model,
                run_handler=run_handler,
                datasets=datasets,
                runtime=runtime,
                resource_pool=resource_pool,
                ttl=ttl,
                dev_mode=dev_mode,
                dev_mode_password=dev_mode_password,
                overwrite_specs=overwrite_specs,
            )

    @classmethod
    def create_local(
        cls,
        model: Resource | str,
        run_handler: str,
        project: Project | str = DEFAULT_PROJECT,
        datasets: t.List[str | Resource] | None = None,
        dataset_head: int = 0,
    ) -> Job:
        """Create a standalone job."""
        if isinstance(project, str):
            project = Project(project)

        if not project.instance.is_local:
            raise ValueError("project must be a standalone instance")

        if isinstance(model, str):
            model = Resource(model, typ=ResourceType.model)

        if model.instance.is_cloud:
            model = BundleCopy.download_for_cache(model)

        from starwhale.core.model.model import ModelConfig, StandaloneModel
        from starwhale.core.model.store import ModelStorage

        model_src_dir = ModelStorage(model).src_dir
        model_config = ModelConfig.create_by_yaml(model_src_dir / DefaultYAMLName.MODEL)
        dataset_uris = []
        for d in datasets or []:
            if isinstance(d, Resource):
                dataset_uris.append(d.full_uri)
            else:
                dataset_uris.append(d)

        job_uri = StandaloneModel.run(
            model_src_dir=model_src_dir,
            model_config=model_config,
            run_project=project,
            log_project=project,
            run_handler=run_handler,
            dataset_uris=dataset_uris,
            dataset_head=dataset_head,
        )
        return cls(job_uri)

    @classmethod
    def create_remote(
        cls,
        project: Project | str,
        model: Resource | str,
        run_handler: str,
        datasets: t.List[str | Resource] | None = None,
        runtime: Resource | str | None = None,
        resource_pool: str = DEFAULT_RESOURCE_POOL,
        ttl: int = 0,
        dev_mode: bool = False,
        dev_mode_password: str = "",
        overwrite_specs: t.Dict[str, t.Any] | None = None,
    ) -> Job:
        """Create a server/cloud job."""
        if isinstance(project, str):
            project = Project(project)

        if not project.instance.is_cloud:
            raise ValueError("project must be a server/cloud instance")

        # TODO: support overwrite specs
        from starwhale.core.model.model import CloudModel

        status, job_id = CloudModel.run(
            project_uri=project,
            model_uri=model,
            run_handler=run_handler,
            dataset_uris=datasets,
            runtime_uri=runtime,
            resource_pool=resource_pool,
            ttl=ttl,
            dev_mode=dev_mode,
            dev_mode_password=dev_mode_password,
            overwrite_specs=overwrite_specs,
        )

        if not status:
            raise RuntimeError(f"create job failed: {job_id}")

        job_uri = Resource(job_id, typ=ResourceType.job, project=project)
        return cls(job_uri)
