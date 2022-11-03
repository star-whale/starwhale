import time
import typing as t
from pathlib import Path

from loguru import logger

from starwhale.core.job.dag import DAG
from starwhale.api._impl.job import Context
from starwhale.core.job.model import (
    Step,
    STATUS,
    Generator,
    StepResult,
    TaskResult,
    StepExecutor,
    TaskExecutor,
    MultiThreadProcessor,
)


class Scheduler:
    def __init__(
        self,
        project: str,
        version: str,
        module: str,
        workdir: Path,
        dataset_uris: t.List[str],
        steps: t.List[Step],
    ) -> None:
        self._steps: t.Dict[str, Step] = {s.step_name: s for s in steps}
        self.dag: DAG = Generator.generate_dag_from_steps(steps)
        self.project = project
        self.dataset_uris = dataset_uris
        self.module = module
        self.workdir = workdir
        self.version = version

    def schedule(self) -> t.List[StepResult]:
        _results: t.List[StepResult] = []
        # record all vertex's in degree
        indegree_dict = {}
        for vtx in self.dag.vertices():
            indegree_dict[vtx] = self.dag.in_degree(vtx)

        vertices_running: t.Set[str] = set()
        prepared_vertices = self.dag.all_starts()

        while prepared_vertices:
            vertices_to_run = prepared_vertices - vertices_running

            processor = MultiThreadProcessor(
                "",
                len(vertices_to_run),
                list(
                    StepExecutor(
                        self._steps[v],
                        project=self.project,
                        dataset_uris=self.dataset_uris,
                        module=self.module,
                        workdir=self.workdir,
                        version=self.version,
                    )
                    for v in vertices_to_run
                ),
            )

            vertices_running |= set(vertices_to_run)
            # execute and get results
            step_results: t.List[StepResult] = processor.execute()
            _results += step_results
            vertices_finished = [result.step_name for result in step_results]
            vertices_running -= set(vertices_finished)
            prepared_vertices -= set(vertices_finished)

            # update dag
            for result in step_results:
                for v_to in self.dag.successors(result.step_name):
                    indegree_dict[v_to] -= 1
                    if indegree_dict[v_to] == 0:
                        prepared_vertices.add(v_to)

            if not all(
                [
                    all(tr.status == STATUS.SUCCESS for tr in sr.task_results)
                    for sr in step_results
                ]
            ):
                break
        return _results

    def schedule_single_step(self, step_name: str, task_num: int = 0) -> StepResult:
        _step = self._steps[step_name]
        if not _step:
            raise RuntimeError(f"step:{step_name} not found")
        _step_executor = StepExecutor(
            _step,
            project=self.project,
            dataset_uris=self.dataset_uris,
            module=self.module,
            workdir=self.workdir,
            version=self.version,
            task_num=task_num,
        )
        start_time = time.time()
        _result = _step_executor.execute()

        logger.info(
            f"step:{step_name} {_step.status}, result:{_result}, run time:{time.time() - start_time}"
        )
        return _result

    def schedule_single_task(
        self, step_name: str, task_index: int, task_num: int = 0
    ) -> StepResult:
        _step = self._steps[step_name]
        if not _step:
            raise RuntimeError(f"step:{step_name} not found")

        total = task_num or _step.task_num
        if task_index >= total:
            raise RuntimeError(
                f"task_index:{task_index} out of bounds, total:{_step.task_num}"
            )
        _task = TaskExecutor(
            index=task_index,
            context=Context(
                project=self.project,
                version=self.version,
                step=_step.step_name,
                total=total,
                index=task_index,
                dataset_uris=self.dataset_uris,
                workdir=self.workdir,
            ),
            status=STATUS.INIT,
            module=self.module,
            func=_step.step_name,
            cls_name=_step.cls_name,
            workdir=self.workdir,
        )
        start_time = time.time()
        task_result: TaskResult = _task.execute()

        logger.info(
            f"step:{step_name} {_step.status}, task result:{task_result}, run time:{time.time() - start_time}"
        )
        return StepResult(step_name=step_name, task_results=[task_result])
