import typing as t

from starwhale.consts import DEFAULT_PAGE_IDX, DEFAULT_PAGE_SIZE


class JobTermView(object):
    def __init__(self, job_uri: str) -> None:
        pass

    def recover(self) -> None:
        pass

    def remove(self) -> None:
        # EvalLocalStorage().delete(version)
        pass

    def cancel(self) -> None:
        pass

    def resume(self) -> None:
        pass

    def pause(self) -> None:
        pass

    def info(self, page: int = DEFAULT_PAGE_IDX, size: int = DEFAULT_PAGE_SIZE) -> None:
        # EvalLocalStorage().info(version)
        # ClusterView().info_job(project, job, page, size)
        pass

    def create(
        self,
        model_uri: str,
        dataset_uris: t.List[str],
        runtime_uri: str,
        project_uri: str = "",
        name: str = "",
        desc: str = "",
        resource: str = "",
        gencmd: bool = False,
        docker_verbose: bool = False,
    ) -> None:
        """
        EvalExecutor(
            model,
            dataset,
            baseimage,
            name,
            desc,
            gencmd=gencmd,
            docker_verbose=docker_verbose,
        ).run(phase)
        """
        """
        ClusterView().run_job(model, dataset, project, baseimage, resource, name, desc)
        """

    @classmethod
    def list(
        cls,
        project_uri: str = "",
        fullname: bool = False,
        page: int = DEFAULT_PAGE_IDX,
        size: int = DEFAULT_PAGE_SIZE,
    ) -> None:
        # EvalLocalStorage().list(fullname=fullname)
        # ClusterView().list_jobs(project, page, size)
        pass
