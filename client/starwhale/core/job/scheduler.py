import json
import os
import time
import typing as t
import threading
import concurrent.futures
from abc import abstractmethod
from multiprocessing import Pipe
from pathlib import Path

from loguru import logger

from starwhale.api._impl import wrapper
from starwhale.api._impl.wrapper import EvaluationResult, EvaluationMetric, EvaluationQuery
from starwhale.consts import EvaluationResultKind
from starwhale.core.job.model import Step, Task, STATUS
from starwhale.utils.config import SWCliConfigMixed


class TaskPipe:
    def __init__(self, id: str, input_pipe: Pipe, output_pipe: Pipe):
        self.id = id
        self.input_pipe = input_pipe
        self.output_pipe = output_pipe


class Scheduler():
    def __init__(self, project: str, version: str, module: str, workdir: Path, src_dir: Path, dataset_uris: t.List[str],
                 steps: t.List[Step], **kw: t.Any):
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
        self._datastore = self._init_datastore(str(self._sw_config.datastore_dir), project, version)
        logger.debug("datastore inited:{}", self._datastore.eval_id)
        self.task_pipes: t.List[TaskPipe] = []
        self.cond = threading.Condition()

    def _init_datastore(self, root_path: str, project: str, eval_id: str) -> wrapper.Evaluation:
        os.environ["SW_ROOT_PATH"] = root_path
        os.environ["SW_PROJECT"] = project
        os.environ["SW_EVAL_ID"] = eval_id
        logger.debug("datastore info:{}, {}, {}", root_path, project, eval_id)
        return wrapper.Evaluation()

    def run(self) -> None:
        while True:
            with self.cond:
                while len(self.task_pipes) == 0:
                    self.cond.wait()
                for _tp in self.task_pipes:
                    logger.debug("get data from {}", _tp.id)
                    # data = _tp.output_pipe[0].recv()
                    # if isinstance(data, EvaluationResult):
                    #     self._datastore.log_result(data_id=data.data_id, result=data.result, **data.kwargs)
                    # elif isinstance(data, EvaluationMetric):
                    #     self._datastore.log_metrics(metrics=data.metrics, **data.kwargs)
                    # elif isinstance(data, EvaluationQuery):
                    #     if data.kind is EvaluationResultKind.RESULT:
                    #         _tp.input_pipe[0].send(self._datastore.get_results())
                    #     if data.kind is EvaluationResultKind.METRIC:
                    #         _tp.input_pipe[0].send(self._datastore.get_metrics())

    def add_data(self, data: t.Any):
        if isinstance(data, EvaluationResult):
            self._datastore.log_result(data_id=data.data_id, result=data.result, **data.kwargs)
        elif isinstance(data, EvaluationMetric):
            self._datastore.log_metrics(metrics=data.metrics, **data.kwargs)

    def query_data(self, data: EvaluationQuery) -> t.Any:
        logger.debug("main receive query data:{}", data.kind)
        if data.kind == EvaluationResultKind.RESULT:
            logger.debug("hi,all result size: {}", len([res for res in self._datastore.get_results()]))
            return [res for res in self._datastore.get_results()]
        elif data.kind == EvaluationResultKind.METRIC:
            return self._datastore.get_metrics()

    def _insert_pipes(self, task_pipe: TaskPipe):
        with self.cond:
            self.task_pipes.append(task_pipe)

    def __split_tasks(self, **kw: t.Any,):
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
                    **kw
                )

    def schedule(self) -> None:
        _threads = []
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
                        _d = TaskDaemon(TaskPipe(
                            id=f"{_wait.step_name}-{_t.context.index}",
                            input_pipe=_t.input_pipe,
                            output_pipe=_t.output_pipe,
                        ), self)
                        _d.start()
                    _executor = Executor(
                        _wait.concurrency, _wait, _wait.tasks, StepCallback(self)
                    )
                    _executor.start()
                    # executor.setDaemon()
                    _threads.append(_executor)

        for _t in _threads:
            _t.join()

    def schedule_single_task(self, step_name: str, task_index: int):
        _steps = [step for step in self.steps if step_name == step.step_name]
        if len(_steps) == 0:
            raise RuntimeError(f"step:{step_name} not found")
        _step = _steps[0]
        if task_index >= _step.task_num:
            raise RuntimeError(
                f"task_index:{task_index} out of bounds, total:{_step.task_num}"
            )
        _task = _step.tasks[task_index]
        logger.debug("all result size: {}", len([res for res in self._datastore.get_results()]))
        _d = TaskDaemon(TaskPipe(
            id=f"{_step.step_name}-{_task.context.index}",
            input_pipe=_task.input_pipe,
            output_pipe=_task.output_pipe,
        ), self)
        _d.start()

        _executor = Executor(1, _step, [_task], SingleTaskCallback(self))
        _executor.start()
        _executor.join()


class TaskDaemon(threading.Thread):
    def __init__(self, task_pipe: TaskPipe, scheduler: Scheduler):
        super().__init__()
        self.scheduler = scheduler
        self.task_pipe = task_pipe
        self.input_pipe = task_pipe.input_pipe
        self.output_pipe = task_pipe.output_pipe
        self.setDaemon(True)

    def run(self):
        while True:
            logger.debug("task:{} start to waiting data", self.task_pipe.id)
            data = self.output_pipe[1].recv()
            logger.debug("task:{} start to recv data", self.task_pipe.id)
            if isinstance(data, EvaluationQuery):
                self.input_pipe[0].send(self.scheduler.query_data(data))
            else:
                self.scheduler.add_data(data)


class Callback:
    def __init__(self, scheduler: Scheduler):
        self.scheduler = scheduler

    @abstractmethod
    def callback(self, step: Step, tasks: t.List[Task], res: bool, exec_time: float):
        pass


class StepCallback(Callback):
    def __init__(self, scheduler: Scheduler):
        super().__init__(scheduler)

    def callback(self, step: Step, tasks: t.List[Task], res: bool, exec_time: float):
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

    def callback(self, step: Step, tasks: t.List[Task], res: bool, exec_time: float):
        logger.debug(
            "task:{} finished, status:{}, run time:{}", tasks[0], res, exec_time
        )


class Executor(threading.Thread):
    def __init__(self,
                 concurrency: int,
                 step: Step,
                 tasks: t.List[Task],
                 callback: Callback,):
        super().__init__()
        self.concurrency = concurrency
        self.step = step
        self.tasks = tasks
        self.callback = callback

    def run(self):
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
