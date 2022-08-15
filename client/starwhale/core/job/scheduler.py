import os
import time
import typing as t
import threading
import concurrent.futures
from abc import abstractmethod
from pathlib import Path
from multiprocessing.connection import Connection

from loguru import logger

from starwhale.consts import EvaluationResultKind
from starwhale.api._impl import wrapper
from starwhale.core.job.model import Step, Task, STATUS
from starwhale.api._impl.wrapper import (
    EvaluationQuery,
    EvaluationMetric,
    EvaluationResult,
)


class TaskPipe:
    def __init__(self, idx: str, main_conn: Connection, sub_conn: Connection) -> None:
        self.id = idx
        self.main_conn = main_conn
        self.sub_conn = sub_conn


def _init_datastore(project: str, eval_id: str) -> wrapper.Evaluation:
    os.environ["SW_PROJECT"] = project
    os.environ["SW_EVAL_ID"] = eval_id
    logger.debug(f"datastore info, project:{project}, eval_id:{eval_id}")
    return wrapper.Evaluation()


class Scheduler:
    def __init__(
        self,
        project: str,
        version: str,
        module: str,
        workdir: Path,
        src_dir: Path,
        dataset_uris: t.List[str],
        steps: t.List[Step],
        **kw: t.Any,
    ) -> None:
        self.project = project
        self.steps = steps
        self.dataset_uris = dataset_uris
        self.module = module
        self.workdir = workdir
        self.src_dir = src_dir
        self.version = version
        self._split_tasks(**kw)
        self._lock = threading.Lock()
        self._datastore = _init_datastore(project, version)

    def add_data(self, data: t.Any) -> None:
        if isinstance(data, EvaluationResult):
            self._datastore.log_result(
                data_id=data.data_id, result=data.result, **data.kwargs
            )
        elif isinstance(data, EvaluationMetric):
            self._datastore.log_metrics(metrics=data.metrics, **data.kwargs)

    def query_data(self, query: EvaluationQuery) -> t.Any:
        logger.debug(f"main process receive query cmd:{query.kind}")
        if query.kind == EvaluationResultKind.RESULT:
            logger.debug(
                f"query result size: {len(list(self._datastore.get_results()))}"
            )
            return list(self._datastore.get_results())
        elif query.kind == EvaluationResultKind.METRIC:
            return self._datastore.get_metrics()

    def _split_tasks(
        self,
        **kw: t.Any,
    ) -> None:
        for _step in self.steps:
            _step.status = STATUS.INIT
            for index in range(_step.task_num):
                _step.gen_task(
                    index=index,
                    module=self.module,
                    workdir=self.workdir,
                    src_dir=self.src_dir,
                    dataset_uris=self.dataset_uris,
                    version=self.version,
                    project=self.project,
                    **kw,
                )

    def schedule(self) -> None:
        _threads: t.List[Executor] = []
        with self._lock:
            _wait_steps = []
            _finished_step_names = []
            for _step in self.steps:
                if _step.status is STATUS.FAILED:
                    # todo break processing
                    pass
                if _step.status is STATUS.SUCCESS:
                    _finished_step_names.append(_step.step_name)
                if _step.status is STATUS.INIT:
                    _wait_steps.append(_step)
            # judge whether a step's dependency all in finished
            for _wait in _wait_steps:
                if all(d in _finished_step_names for d in _wait.dependency if d):
                    _wait.status = STATUS.RUNNING
                    _executor = Executor(
                        _wait.concurrency, _wait, _wait.tasks, StepCallback(self), self
                    )
                    _executor.start()
                    _threads.append(_executor)

        for _thread in _threads:
            _thread.join()

    def schedule_single_task(self, step_name: str, task_index: int) -> None:
        _steps = [step for step in self.steps if step_name == step.step_name]
        if len(_steps) == 0:
            raise RuntimeError(f"step:{step_name} not found")
        _step = _steps[0]
        if task_index >= _step.task_num:
            raise RuntimeError(
                f"task_index:{task_index} out of bounds, total:{_step.task_num}"
            )
        _task = _step.tasks[task_index]

        _executor = Executor(1, _step, [_task], SingleTaskCallback(self), self)
        _executor.start()
        _executor.join()


class TaskDaemon(threading.Thread):
    def __init__(self, task_pipe: TaskPipe, scheduler: Scheduler) -> None:
        super().__init__()
        self.scheduler = scheduler
        self.task_pipe = task_pipe
        self.main_conn = task_pipe.main_conn
        self.setDaemon(True)

    def run(self) -> None:
        while True:
            logger.debug(f"task:{self.task_pipe.id} waiting data")
            data = self.main_conn.recv()
            logger.debug(f"task:{self.task_pipe.id} start to recv data")
            if isinstance(data, EvaluationQuery):
                self.main_conn.send(self.scheduler.query_data(data))
            else:
                self.scheduler.add_data(data)


class Callback:
    def __init__(self, scheduler: Scheduler) -> None:
        self.scheduler = scheduler

    @abstractmethod
    def callback(
        self, step: Step, tasks: t.List[Task], res: bool, exec_time: float
    ) -> t.Any:
        pass


class StepCallback(Callback):
    def __init__(self, scheduler: Scheduler):
        super().__init__(scheduler)

    def callback(
        self, step: Step, tasks: t.List[Task], res: bool, exec_time: float
    ) -> t.Any:
        if res:
            step.status = STATUS.SUCCESS
            logger.debug(f"step:{step} success, run time:{exec_time}")
            # trigger next schedule
            self.scheduler.schedule()
        else:
            step.status = STATUS.FAILED
            logger.error(f"step:{step} failed, run time:{exec_time}")
            # TODO: whether break process?


class SingleTaskCallback(Callback):
    def __init__(self, scheduler: Scheduler):
        super().__init__(scheduler)

    def callback(
        self, step: Step, tasks: t.List[Task], res: bool, exec_time: float
    ) -> t.Any:
        logger.debug(
            f"task:{tasks[0]} finished, status:{res}, run time:{exec_time}"
        )


class Executor(threading.Thread):
    def __init__(
        self,
        concurrency: int,
        step: Step,
        tasks: t.List[Task],
        callback: Callback,
        scheduler: Scheduler,
    ):
        super().__init__()
        self.concurrency = concurrency
        self.step = step
        self.tasks = tasks
        self.callback = callback
        self.scheduler = scheduler

    def run(self) -> None:
        start_time = time.time()
        # use pipe for multiprocessing communication
        for _t in self.tasks:
            _d = TaskDaemon(
                TaskPipe(
                    idx=f"{self.step.step_name}-{_t.context.index}",
                    main_conn=_t.main_conn,
                    sub_conn=_t.sub_conn,
                ),
                scheduler=self.scheduler,
            )
            _d.start()
        with concurrent.futures.ProcessPoolExecutor(
            max_workers=self.concurrency
        ) as executor:
            futures = [executor.submit(task.execute) for task in self.tasks]
            self.callback.callback(
                step=self.step,
                tasks=self.tasks,
                res=all(
                    future.result()
                    for future in concurrent.futures.as_completed(futures)
                ),
                exec_time=time.time() - start_time,
            )
