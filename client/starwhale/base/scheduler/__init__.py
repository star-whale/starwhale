from __future__ import annotations

import time
import typing as t
from pathlib import Path
from concurrent.futures import as_completed, ThreadPoolExecutor

from starwhale.utils import console
from starwhale.consts import RunStatus
from starwhale.base.context import Context
from starwhale.base.uri.project import Project

from .dag import DAG
from .step import Step, StepResult, StepExecutor
from .task import TaskResult, TaskExecutor


class Scheduler:
    def __init__(
        self,
        version: str,
        workdir: Path,
        dataset_uris: t.List[str],
        steps: t.List[Step],
        handler_args: t.List[str] | None = None,
        run_project: t.Optional[Project] = None,
        log_project: t.Optional[Project] = None,
        dataset_head: int = 0,
        finetune_val_dataset_uris: t.List[str] | None = None,
        model_name: str = "",
    ) -> None:
        self._steps: t.Dict[str, Step] = {s.name: s for s in steps}
        self.dag: DAG = Step.generate_dag(steps)
        self.run_project = run_project or Project()
        self.log_project = log_project or self.run_project
        self.dataset_uris = dataset_uris
        self.workdir = workdir
        self.version = version
        self.handler_args = handler_args or []
        self.dataset_head = dataset_head
        self.finetune_val_dataset_uris = finetune_val_dataset_uris
        self.model_name = model_name

    def run(
        self,
        step_name: str = "",
        task_num: int = 0,
        task_index: int | None = None,
    ) -> t.List[StepResult]:
        if not step_name:
            return self._schedule_all()

        if task_index is None or task_index < 0:
            return [self._schedule_one_step(step_name=step_name)]
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
                    run_project=self.run_project,
                    log_project=self.log_project,
                    dataset_uris=self.dataset_uris,
                    workdir=self.workdir,
                    version=self.version,
                    handler_args=self.handler_args,
                    dataset_head=self.dataset_head,
                    finetune_val_dataset_uris=self.finetune_val_dataset_uris,
                    model_name=self.model_name,
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

    def _schedule_one_step(self, step_name: str) -> StepResult:
        step = self._steps[step_name]
        start_time = time.time()
        result = StepExecutor(
            step=step,
            run_project=self.run_project,
            log_project=self.log_project,
            dataset_uris=self.dataset_uris,
            workdir=self.workdir,
            version=self.version,
            dataset_head=self.dataset_head,
            finetune_val_dataset_uris=self.finetune_val_dataset_uris,
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
                run_project=self.run_project,
                log_project=self.log_project,
                version=self.version,
                step=_step.name,
                total=task_num,
                index=task_index,
                dataset_uris=self.dataset_uris,
                finetune_val_dataset_uris=self.finetune_val_dataset_uris,
                dataset_head=self.dataset_head,
                workdir=self.workdir,
                model_name=self.model_name,
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
