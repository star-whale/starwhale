import time
import threading
import concurrent.futures
from abc import abstractmethod
from pathlib import Path

from loguru import logger

from starwhale.core.job.base.model import Step, STATUS, Task


class Scheduler:
    def __init__(self, module: str, workdir: Path, steps: dict[Step]):
        self.steps = steps
        self.module = module
        self.workdir = workdir
        self.__split_tasks()
        self._lock = threading.RLock()

    def __split_tasks(self):
        for item in self.steps.items():
            _step = item[1]
            # update step status = init
            _step.status = STATUS.INIT
            for index in range(_step.task_num):
                _step.gen_task(index, self.module, self.workdir)

    def schedule(self) -> None:
        _threads = []
        with self._lock:
            _wait_steps = []
            _finished_step_names = []
            for item in self.steps.items():
                _step = item[1]
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
                    _executor = Executor(_wait.concurrency, _wait.tasks, StepCallback(self, _wait))
                    _executor.start()
                    # executor.setDaemon()
                    _threads.append(_executor)

        for t in _threads:
            t.join()

    def schedule_single_task(self, step_name: str, task_index: int):
        _step = self.steps[step_name]
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
        logger.debug("step:{} finished, status:{}, run time:{}", self.step, res, exec_time)
        if res:
            self.step.status = STATUS.SUCCESS
            # trigger next schedule
            self.scheduler.schedule()
        else:
            self.step.status = STATUS.FAILED
            # todo whether break process?


class SingleTaskCallback(Callback):
    def __init__(self, scheduler: Scheduler, task: Task):
        super().__init__(scheduler)
        self.task = task

    def callback(self, res: bool, exec_time: float):
        logger.debug("task:{} finished, status:{}, run time:{}", self.task, res, exec_time)


class Executor(threading.Thread):
    def __init__(self, concurrency: int, tasks: list[Task], callback: Callback):
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
            # todo custom module and path
            futures = [
                executor.submit(task.execute)
                for task in self.tasks
            ]
            self.callback.callback(
                all(future.result() for future in concurrent.futures.as_completed(futures)), time.time() - start_time)
