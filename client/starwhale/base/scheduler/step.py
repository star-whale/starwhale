from __future__ import annotations

import typing as t
from pathlib import Path
from concurrent.futures import as_completed, ThreadPoolExecutor

from starwhale.utils import console, load_yaml
from starwhale.consts import RunStatus
from starwhale.base.mixin import ASDictMixin
from starwhale.base.context import Context

from .dag import DAG
from .task import TaskResult, TaskExecutor


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
        show_name: str = "",
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
        self.name = name
        self.show_name = show_name or name
        self.module_name = module_name
        self.cls_name = cls_name
        self.func_name = func_name
        self.resources = resources or []
        self.concurrency = concurrency
        self.task_num = task_num
        self.needs = needs or []
        self.extra_args = extra_args or []
        self.extra_kwargs = extra_kwargs or {}
        self.expose = kw.get("expose", 0)
        self.virtual = kw.get("virtual", False)
        self.require_dataset = kw.get("require_dataset", False)

        # TODO: add validation

    def __str__(self) -> str:
        return f"Step[{self.name}]: handler-{self.module_name}:{self.cls_name}.{self.func_name}"

    def __repr__(self) -> str:
        return f"Step[{self.name}]: handler-{self.module_name}:{self.cls_name}.{self.func_name}, {self.asdict()}"

    @classmethod
    def get_steps_from_yaml(
        cls, job_name: str | int, yaml_path: t.Union[str, Path]
    ) -> t.List[Step]:
        # default run index 0 handler
        job_name = job_name or "0"
        jobs = load_yaml(yaml_path)
        sorted_jobs = sorted(jobs.items())

        job = None
        try:
            if job_name in jobs:
                job = jobs[job_name]
            else:
                job_index = int(job_name)
                job = sorted_jobs[job_index][1]
        finally:
            console.print(":bank: runnable handlers:")
            for i, j in enumerate(sorted_jobs):
                flag = "" if job is None or job != j[1] else "*"
                console.print(f"\t {flag:2} [{i}]: {j[0]}")

        """
        - cls_name: MNISTInference
          concurrency: 1
          extra_args: []
          extra_kwargs: {}
          func_name: ppl
          name: mnist.evaluator:MNISTInference.ppl
          show_name: ppl
          needs: ["mnist.evaluator:MNISTInference.cmp"]
          module_name: mnist.evaluator
          replicas: 2
          resources: []
          expose: 0
          virtual: false
        """
        steps = []
        for v in job:
            step = Step(
                name=v["name"],
                show_name=v["show_name"],
                resources=v["resources"],
                needs=v["needs"],
                concurrency=v["concurrency"],
                task_num=v["replicas"],
                cls_name=v["cls_name"],
                module_name=v["module_name"],
                func_name=v["func_name"],
                extra_args=v.get("extra_args"),
                extra_kwargs=v.get("extra_kwargs"),
                expose=v.get("expose"),
                virtual=v.get("virtual"),
                require_dataset=v.get("require_dataset"),
            )
            steps.append(step)
        return steps

    @staticmethod
    def generate_dag(steps: t.List[Step]) -> DAG:
        dag = DAG()
        # add all vertexes before add edges
        for step in steps:
            dag.add_vertex(step.name)

        for step in steps:
            for pre in step.needs or []:
                dag.add_edge(pre, step.name)
        return dag


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
        console.info(f"start to execute step:{self.step}")

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

        console.info(f"finish to execute step:{self.step}")
        return StepResult(name=self.step.name, task_results=task_results)
