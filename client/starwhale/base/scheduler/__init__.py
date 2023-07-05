from __future__ import annotations

import time
import typing as t
from pathlib import Path
from concurrent.futures import as_completed, ThreadPoolExecutor

from starwhale.utils import console
from starwhale.consts import RunStatus
from starwhale.base.context import Context

from .dag import DAG
from .step import Step, StepResult, StepExecutor
from .task import TaskResult, TaskExecutor


class Scheduler:
    def __init__(
        self,
        project: str,
        version: str,
        workdir: Path,
        dataset_uris: t.List[str],
        steps: t.List[Step],
    ) -> None:
        self._steps: t.Dict[str, Step] = {s.name: s for s in steps}
        self.dag: DAG = Step.generate_dag(steps)
        self.project = project
        self.dataset_uris = dataset_uris
        self.workdir = workdir
        self.version = version

    def run(
        self,
        step_name: str = "",
        task_num: int = 0,
        task_index: int | None = None,
    ) -> t.List[StepResult]:
        if not step_name:
            return self._schedule_all()

        if task_index is None or task_index < 0:
            return [self._schedule_one_step(step_name=step_name, task_num=task_num)]
        else:
            return [
                self._schedule_one_task(
                    step_name=step_name, task_index=task_index, task_num=task_num
                )
            ]

    def _schedule_all(self) -> t.List[StepResult]:
        _results: t.List[StepResult] = []
        # record all vertex's in degree
        indegree_dict = {}
        for vtx in self.dag.vertices():
            indegree_dict[vtx] = self.dag.in_degree(vtx)

        vertices_running: t.Set[str] = set()
        prepared_vertices = self.dag.all_starts()

        while prepared_vertices:
            vertices_to_run = prepared_vertices - vertices_running

            tasks = [
                StepExecutor(
                    self._steps[v],
                    project=self.project,
                    dataset_uris=self.dataset_uris,
                    workdir=self.workdir,
                    version=self.version,
                )
                for v in vertices_to_run
            ]
            with ThreadPoolExecutor(max_workers=len(vertices_to_run)) as pool:
                future_tasks = [pool.submit(t.execute) for t in tasks]
                step_results = [t.result() for t in as_completed(future_tasks)]

            vertices_running |= set(vertices_to_run)
            _results += step_results
            vertices_finished = [result.name for result in step_results]
            vertices_running -= set(vertices_finished)
            prepared_vertices -= set(vertices_finished)

            # update dag
            for result in step_results:
                for v_to in self.dag.successors(result.name):
                    indegree_dict[v_to] -= 1
                    if indegree_dict[v_to] == 0:
                        prepared_vertices.add(v_to)

            if not all(
                [
                    all(tr.status == RunStatus.SUCCESS for tr in sr.task_results)
                    for sr in step_results
                ]
            ):
                break
        return _results

    def _schedule_one_step(self, step_name: str, task_num: int = 0) -> StepResult:
        step = self._steps[step_name]
        start_time = time.time()
        result = StepExecutor(
            step=step,
            project=self.project,
            dataset_uris=self.dataset_uris,
            workdir=self.workdir,
            version=self.version,
            task_num=task_num,
        ).execute()

        console.info(
            f"step:{step_name}, result:{result}, run time:{time.time() - start_time}"
        )
        return result

    def _schedule_one_task(
        self, step_name: str, task_index: int, task_num: int | None = None
    ) -> StepResult:
        _step = self._steps[step_name]
        if task_num is None or task_num <= 0:
            task_num = _step.task_num

        if task_index >= task_num:
            raise RuntimeError(
                f"task_index:{task_index} out of bounds, task_num:{task_num}"
            )

        _task = TaskExecutor(
            index=task_index,
            context=Context(
                project=self.project,
                version=self.version,
                step=_step.name,
                total=task_num,
                index=task_index,
                dataset_uris=self.dataset_uris,
                workdir=self.workdir,
            ),
            step=_step,
            workdir=self.workdir,
        )
        start_time = time.time()
        task_result: TaskResult = _task.execute()

        console.info(
            f"step:{step_name}, task result:{task_result}, run time:{time.time() - start_time}"
        )
        return StepResult(name=step_name, task_results=[task_result])
