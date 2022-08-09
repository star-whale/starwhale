import os
import time
import typing as t
import threading
import concurrent.futures
from abc import abstractmethod
from pathlib import Path

from loguru import logger

from starwhale.base.uri import URI
from starwhale.core.job.model import Step, Task, STATUS


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
    ):
        self.project = project
        self.steps = steps
        self.dataset_uris = dataset_uris
        self.module = module
        self.workdir = workdir
        self.src_dir = src_dir
        self.version = version
        self.__split_tasks()
        self._lock = threading.RLock()

    def __split_tasks(self):
        for _step in self.steps:
            # update step status = init
            _step.status = STATUS.INIT
            for index in range(_step.task_num):
                _step.gen_task(
                    index,
                    self.module,
                    self.workdir,
                    self.src_dir,
                    self.dataset_uris,
                    self.version,
                    self.project,
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
                    _executor = Executor(
                        _wait.concurrency, _wait.tasks, StepCallback(self, _wait)
                    )
                    _executor.start()
                    # executor.setDaemon()
                    _threads.append(_executor)

        for _t in _threads:
            _t.join()

    def schedule_single_task(self, step_name: str, task_index: int):
        _steps = [step for step in self.steps if step_name == step.step_name]
        if len(_steps) is 0:
            raise RuntimeError(f"step:{step_name} not found")
        _step = _steps[0]
        if task_index >= _step.task_num:
            raise RuntimeError(
                f"task_index:{task_index} out of bounds, total:{_step.task_num}"
            )
        _task = _step.tasks[task_index]
        _executor = Executor(1, [_task], SingleTaskCallback(self, _task))
        _executor.start()
        _executor.join()


class Callback:
    def __init__(self, scheduler: Scheduler):
        self.scheduler = scheduler

    @abstractmethod
    def callback(self, res: bool, exec_time: float):
        pass


class StepCallback(Callback):
    def __init__(self, scheduler: Scheduler, step: Step):
        super().__init__(scheduler)
        self.step = step

    def callback(self, res: bool, exec_time: float):
        if res:
            self.step.status = STATUS.SUCCESS
            logger.debug("step:{} success, run time:{}", self.step, exec_time)
            # trigger next schedule
            self.scheduler.schedule()
        else:
            self.step.status = STATUS.FAILED
            logger.debug("step:{} failed, run time:{}", self.step, exec_time)
            # TODO: whether break process?


class SingleTaskCallback(Callback):
    def __init__(self, scheduler: Scheduler, task: Task):
        super().__init__(scheduler)
        self.task = task

    def callback(self, res: bool, exec_time: float):
        logger.debug(
            "task:{} finished, status:{}, run time:{}", self.task, res, exec_time
        )


class Executor(threading.Thread):
    def __init__(self, concurrency: int, tasks: t.List[Task], callback: Callback):
        super().__init__()
        self.concurrency = concurrency
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
                all(
                    future.result()
                    for future in concurrent.futures.as_completed(futures)
                ),
                time.time() - start_time,
            )
