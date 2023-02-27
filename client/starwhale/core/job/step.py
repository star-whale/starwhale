from __future__ import annotations

import typing as t
from pathlib import Path
from concurrent.futures import as_completed, ThreadPoolExecutor

from loguru import logger

from starwhale.utils import load_yaml
from starwhale.consts import RunStatus, DEFAULT_EVALUATION_JOB_NAME
from starwhale.base.mixin import ASDictMixin

from .dag import DAG, generate_dag
from .task import TaskResult, TaskExecutor
from .context import Context


class StepResult:
    def __init__(self, name: str, task_results: t.List[TaskResult]):
        self.name = name
        self.task_results = task_results

    @property
    def status(self) -> str:
        return (
            RunStatus.SUCCESS
            if all(tr.status == RunStatus.SUCCESS for tr in self.task_results)
            else RunStatus.FAILED
        )

    def __repr__(self) -> str:
        return (
            f"step:{self.name}, taskResults:{self.task_results}, status:{self.status}"
        )


class Step(ASDictMixin):
    def __init__(
        self,
        name: str,
        job_name: str = DEFAULT_EVALUATION_JOB_NAME,
        resources: t.Optional[t.List[t.Dict]] = None,
        needs: t.Optional[t.List[str]] = None,
        concurrency: int = 1,
        task_num: int = 1,
        cls_name: str = "",
        module_name: str = "",
        func_name: str = "",
        extra_args: t.Optional[t.List] = None,
        extra_kwargs: t.Optional[t.Dict] = None,
        **kw: t.Any,
    ):
        self.job_name = job_name
        self.name = name
        self.module_name = module_name
        self.cls_name = cls_name
        self.func_name = func_name
        self.resources = resources or []
        self.concurrency = concurrency
        self.task_num = task_num
        self.needs = needs or []
        self.extra_args = extra_args or []
        self.extra_kwargs = extra_kwargs or {}

        # TODO: add validation

    def __str__(self) -> str:
        return f"Step[{self.name}]: handler-{self.module_name}:{self.cls_name}.{self.func_name}"

    def __repr__(self) -> str:
        return f"Step[{self.name}]: handler-{self.module_name}:{self.cls_name}.{self.func_name}, job-{self.job_name}, needs-{self.needs}"

    @classmethod
    def get_steps_from_yaml(
        cls, job_name: str, yaml_path: t.Union[str, Path]
    ) -> t.List[Step]:
        jobs = load_yaml(yaml_path)
        return [cls(**v) for v in jobs[job_name]]

    @staticmethod
    def generate_dag(steps: t.List[Step]) -> DAG:
        _vertices: t.List[str] = []
        _edges: t.Dict[str, str] = {}
        for step in steps:
            _vertices.append(step.name)
            if not step.needs:
                continue
            for _pre in step.needs:
                _edges[_pre] = step.name

        return generate_dag(_vertices, _edges)


class StepExecutor:
    def __init__(
        self,
        step: Step,
        project: str,
        version: str,
        workdir: Path,
        dataset_uris: t.List[str],
        task_num: int = 0,
    ) -> None:
        self.step = step
        self.task_num = step.task_num if task_num <= 0 else task_num
        self.project = project
        self.dataset_uris = dataset_uris
        self.workdir = workdir
        self.version = version

    def __str__(self) -> str:
        return f"StepExecutor: step-{self.step}, version-{self.version}"

    def __repr__(self) -> str:
        return f"StepExecutor: step-{self.step}, version-{self.version}, dataset_uris:{self.dataset_uris}"

    def execute(self) -> StepResult:
        logger.info(f"start to execute step:{self.step}")

        tasks = [
            TaskExecutor(
                index=index,
                context=Context(
                    project=self.project,
                    version=self.version,
                    step=self.step.name,
                    total=self.task_num,
                    index=index,
                    dataset_uris=self.dataset_uris,
                    workdir=self.workdir,
                ),
                step=self.step,
                workdir=self.workdir,
            )
            for index in range(self.task_num)
        ]

        with ThreadPoolExecutor(max_workers=self.step.concurrency) as pool:
            future_tasks = [pool.submit(t.execute) for t in tasks]
            task_results = [t.result() for t in as_completed(future_tasks)]

        logger.info(f"finish to execute step:{self.step}")
        return StepResult(name=self.step.name, task_results=task_results)
