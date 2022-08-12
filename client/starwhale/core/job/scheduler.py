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
from starwhale.utils.config import SWCliConfigMixed
from starwhale.core.job.model import Step, Task, STATUS
from starwhale.api._impl.wrapper import (
    EvaluationQuery,
    EvaluationMetric,
    EvaluationResult,
)


class TaskPipe:
    def __init__(self, idx: str, main_conn: Connection, sub_conn: Connection):
        self.id = idx
        self.main_conn = main_conn
        self.sub_conn = sub_conn


def _init_datastore(root_path: str, project: str, eval_id: str) -> wrapper.Evaluation:
    os.environ["SW_ROOT_PATH"] = root_path
    os.environ["SW_PROJECT"] = project
    os.environ["SW_EVAL_ID"] = eval_id
    logger.debug("datastore info:{}, {}, {}", root_path, project, eval_id)
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
    ):
        self.project = project
        self.steps = steps
        self.dataset_uris = dataset_uris
        self.module = module
        self.workdir = workdir
        self.src_dir = src_dir
        self.version = version
        self.__split_tasks(**kw)
        self._lock = threading.Lock()
        self._sw_config = SWCliConfigMixed()
        self._datastore = _init_datastore(
            str(self._sw_config.datastore_dir), project, version
        )

    def add_data(self, data: t.Any) -> None:
        if isinstance(data, EvaluationResult):
            self._datastore.log_result(
                data_id=data.data_id, result=data.result, **data.kwargs
            )
        elif isinstance(data, EvaluationMetric):
            self._datastore.log_metrics(metrics=data.metrics, **data.kwargs)

    def query_data(self, query: EvaluationQuery) -> t.Any:
        logger.debug("main receive query data:{}", query.kind)
        if query.kind == EvaluationResultKind.RESULT:
            logger.debug(
                "hi,all result size: {}",
                len([res for res in self._datastore.get_results()]),
            )
            return [res for res in self._datastore.get_results()]
        elif query.kind == EvaluationResultKind.METRIC:
            return self._datastore.get_metrics()

    def __split_tasks(
        self,
        **kw: t.Any,
    ) -> None:
        for _step in self.steps:
            # update step status = init
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
                    # add pipe handler

                    for _t in _wait.tasks:
                        _d = TaskDaemon(
                            TaskPipe(
                                idx=f"{_wait.step_name}-{_t.context.index}",
                                main_conn=_t.main_conn,
                                sub_conn=_t.sub_conn,
                            ),
                            self,
                        )
                        _d.start()
                    _executor = Executor(
                        _wait.concurrency, _wait, _wait.tasks, StepCallback(self)
                    )
                    _executor.start()
                    # executor.setDaemon()
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
        logger.debug(
            "all result size: {}", len([res for res in self._datastore.get_results()])
        )
        _d = TaskDaemon(
            TaskPipe(
                idx=f"{_step.step_name}-{_task.context.index}",
                main_conn=_task.main_conn,
                sub_conn=_task.sub_conn,
            ),
            self,
        )
        _d.start()

        _executor = Executor(1, _step, [_task], SingleTaskCallback(self))
        _executor.start()
        _executor.join()


class TaskDaemon(threading.Thread):
    def __init__(self, task_pipe: TaskPipe, scheduler: Scheduler):
        super().__init__()
        self.scheduler = scheduler
        self.task_pipe = task_pipe
        self.main_conn = task_pipe.main_conn
        self.setDaemon(True)

    def run(self) -> None:
        while True:
            logger.debug("task:{} start to waiting data", self.task_pipe.id)
            data = self.main_conn.recv()
            logger.debug("task:{} start to recv data", self.task_pipe.id)
            if isinstance(data, EvaluationQuery):
                self.main_conn.send(self.scheduler.query_data(data))
            else:
                self.scheduler.add_data(data)


class Callback:
    def __init__(self, scheduler: Scheduler):
        self.scheduler = scheduler

    @abstractmethod
    def callback(self, step: Step, tasks: t.List[Task], res: bool, exec_time: float) -> t.Any:
        pass


class StepCallback(Callback):
    def __init__(self, scheduler: Scheduler):
        super().__init__(scheduler)

    def callback(self, step: Step, tasks: t.List[Task], res: bool, exec_time: float) -> t.Any:
        if res:
            step.status = STATUS.SUCCESS
            logger.debug("step:{} success, run time:{}", step, exec_time)
            # trigger next schedule
            self.scheduler.schedule()
        else:
            step.status = STATUS.FAILED
            logger.debug("step:{} failed, run time:{}", step, exec_time)
            # TODO: whether break process?


class SingleTaskCallback(Callback):
    def __init__(self, scheduler: Scheduler):
        super().__init__(scheduler)

    def callback(self, step: Step, tasks: t.List[Task], res: bool, exec_time: float) -> t.Any:
        logger.debug(
            "task:{} finished, status:{}, run time:{}", tasks[0], res, exec_time
        )


class Executor(threading.Thread):
    def __init__(
        self,
        concurrency: int,
        step: Step,
        tasks: t.List[Task],
        callback: Callback,
    ):
        super().__init__()
        self.concurrency = concurrency
        self.step = step
        self.tasks = tasks
        self.callback = callback

    def run(self) -> None:
        # processing pool
        start_time = time.time()
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
