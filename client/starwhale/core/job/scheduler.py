import time
import typing as t
import threading
import concurrent.futures
from abc import ABCMeta, abstractmethod
from pathlib import Path

from loguru import logger

from starwhale.api._impl.job import Step, Context
from starwhale.core.job.model import Task, STATUS


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
        kw: t.Dict[str, t.Any] = {},
    ) -> None:
        self.project = project
        self.steps = steps
        self.dataset_uris = dataset_uris
        self.module = module
        self.workdir = workdir
        self.src_dir = src_dir
        self.version = version
        self.kw = kw
        self._lock = threading.Lock()

    def _split_tasks(self, step: Step) -> t.List[Task]:
        step.status = STATUS.INIT
        _tasks = []
        for index in range(step.task_num):
            _tasks.append(
                Task(
                    context=Context(
                        project=self.project,
                        # todo:use id or version
                        version=self.version,
                        step=step.step_name,
                        total=step.task_num,
                        index=index,
                        dataset_uris=self.dataset_uris,
                        workdir=self.workdir,
                        src_dir=self.src_dir,
                        **self.kw,
                    ),
                    status=STATUS.INIT,
                    module=self.module,
                    src_dir=self.src_dir,
                )
            )
        return _tasks

    def schedule(self) -> None:
        _threads: t.List[Executor] = []
        with self._lock:
            _wait_steps = []
            _finished_step_names = []
            for _step in self.steps:
                if _step.status == STATUS.FAILED:
                    # todo break processing
                    pass
                if _step.status == STATUS.SUCCESS:
                    _finished_step_names.append(_step.step_name)
                if _step.status == STATUS.INIT or not _step.status:
                    _wait_steps.append(_step)
            # judge whether a step's dependency all in finished
            logger.debug(f"wait run:{_wait_steps}")
            for _wait in _wait_steps:
                if all(d in _finished_step_names for d in _wait.dependency if d):
                    _wait.status = STATUS.RUNNING
                    _executor = Executor(
                        _wait.concurrency,
                        _wait,
                        self._split_tasks(step=_wait),
                        StepCallback(self),
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
        _task = Task(
            context=Context(
                project=self.project,
                # todo:use id or version
                version=self.version,
                step=_step.step_name,
                total=_step.task_num,
                index=task_index,
                dataset_uris=self.dataset_uris,
                workdir=self.workdir,
                src_dir=self.src_dir,
                **self.kw,
            ),
            status=STATUS.INIT,
            module=self.module,
            src_dir=self.src_dir,
        )

        _executor = Executor(1, _step, [_task], SingleTaskCallback(self))
        _executor.start()
        _executor.join()


class Callback(metaclass=ABCMeta):
    def __init__(self, scheduler: Scheduler) -> None:
        self.scheduler = scheduler

    @abstractmethod
    def callback(
        self, step: Step, tasks: t.List[Task], res: bool, exec_time: float
    ) -> t.Any:
        raise NotImplementedError


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
        logger.debug(f"task:{tasks[0]} finished, status:{res}, run time:{exec_time}")


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
        start_time = time.time()
        with concurrent.futures.ProcessPoolExecutor(
            max_workers=self.concurrency
        ) as executor:
            futures = [executor.submit(task.execute) for task in self.tasks]
            res = all(
                future.result() for future in concurrent.futures.as_completed(futures)
            )
        self.callback.callback(
            step=self.step,
            tasks=self.tasks,
            res=res,
            exec_time=time.time() - start_time,
        )
