import time
import typing as t
import threading
import concurrent.futures
from queue import Queue
from pathlib import Path

from loguru import logger

from starwhale.api._impl.job import Step, Context
from starwhale.core.job.model import Task, STATUS, StepResult, TaskResult


class Scheduler:
    def __init__(
        self,
        project: str,
        version: str,
        module: str,
        workdir: Path,
        dataset_uris: t.List[str],
        steps: t.List[Step],
        kw: t.Dict[str, t.Any] = {},
    ) -> None:
        self._steps: t.Dict[str, Step] = {s.step_name: s for s in steps}
        self._results: t.List[StepResult] = []
        self.project = project
        self.dataset_uris = dataset_uris
        self.module = module
        self.workdir = workdir
        self.version = version
        self.kw = kw
        self.lock = threading.Lock()

    def report(self, step_result: StepResult) -> None:
        with self.lock:
            _sr = self._steps.get(step_result.step_name)
            if not _sr:
                raise RuntimeError(
                    f"step:{step_result.step_name} not in current scheduler!"
                )
            _sr.status = step_result.status
            self._results.append(step_result)

    def get_results(self) -> t.List[StepResult]:
        return self._results

    def get_next(self) -> t.Optional[t.List[Step]]:
        with self.lock:
            _wait_steps = []
            _finished_step_names = []
            _failed = False
            for _name, _step in self._steps.items():
                if _step.status == STATUS.FAILED:
                    logger.error(f"step:{_name} failed, now exited.")
                    _failed = True
                    break
                elif _step.status == STATUS.SUCCESS:
                    _finished_step_names.append(_name)
                elif _step.status == STATUS.INIT or not _step.status:
                    _wait_steps.append(_step)
            # judge whether a step's dependency all in finished
            if len(_wait_steps) == 0 or _failed:
                return None

            _rt = []
            for _wait in _wait_steps:
                if all(d in _finished_step_names for d in _wait.needs if d):
                    logger.debug(f"produce a step:{_wait}")
                    _wait.status = STATUS.RUNNING
                    _rt.append(_wait)
            return _rt

    def schedule(self) -> None:
        queue: Queue[Executor] = Queue()
        p = Producer(
            scheduler=self,
            queue=queue,
            project=self.project,
            dataset_uris=self.dataset_uris,
            module=self.module,
            workdir=self.workdir,
            version=self.version,
            kw=self.kw,
        )
        p.start()
        c = Consumer(queue)
        c.start()
        p.join()
        queue.join()

    def schedule_single_task(self, step_name: str, task_index: int) -> None:
        _step = self._steps[step_name]
        if not _step:
            raise RuntimeError(f"step:{step_name} not found")

        if task_index >= _step.task_num:
            raise RuntimeError(
                f"task_index:{task_index} out of bounds, total:{_step.task_num}"
            )
        _task = Task(
            index=task_index,
            context=Context(
                project=self.project,
                version=self.version,
                step=_step.step_name,
                total=_step.task_num,
                index=task_index,
                dataset_uris=self.dataset_uris,
                workdir=self.workdir,
                **self.kw,
            ),
            status=STATUS.INIT,
            module=self.module,
            workdir=self.workdir,
        )
        _sc = StepCallback(self, step=_step)
        start_time = time.time()
        task_result = _task.execute()
        _sc.callback([task_result])

        logger.debug(
            f"step:{str(_step)} {_step.status}, task result:{task_result}, run time:{time.time() - start_time}"
        )


class StepCallback:
    def __init__(self, scheduler: Scheduler, step: Step) -> None:
        self.scheduler = scheduler
        self.step = step

    def callback(self, results: t.List[TaskResult]) -> t.Any:
        self.scheduler.report(StepResult(self.step.step_name, results))


class Executor:
    def __init__(
        self,
        concurrency: int,
        step: Step,
        tasks: t.List[Task],
        callback: StepCallback,
    ) -> None:
        self.concurrency = concurrency
        self.step = step
        self.tasks = tasks
        self.callback = callback

    def exec(self) -> None:
        start_time = time.time()
        with concurrent.futures.ThreadPoolExecutor(
            max_workers=self.step.concurrency
        ) as pool:
            futures = [pool.submit(task.execute) for task in self.tasks]
            task_results: t.List[TaskResult] = list(
                future.result() for future in concurrent.futures.as_completed(futures)
            )
            exec_time = time.time() - start_time
            logger.debug(
                f"execute step:{self.step.step_name}, task results:{task_results}, exec time:{exec_time}"
            )

            self.callback.callback(results=task_results)


class Producer(threading.Thread):
    def __init__(
        self,
        scheduler: Scheduler,
        queue: Queue,
        project: str,
        version: str,
        module: str,
        workdir: Path,
        dataset_uris: t.List[str],
        kw: t.Dict[str, t.Any] = {},
    ) -> None:
        super().__init__()
        self.scheduler = scheduler
        self.queue = queue
        self.project = project
        self.dataset_uris = dataset_uris
        self.module = module
        self.workdir = workdir
        self.version = version
        self.kw = kw

    def _split_tasks(self, step: Step) -> t.List[Task]:
        _tasks = []
        for index in range(step.task_num):
            _tasks.append(
                Task(
                    index=index,
                    context=Context(
                        project=self.project,
                        version=self.version,
                        step=step.step_name,
                        total=step.task_num,
                        index=index,
                        dataset_uris=self.dataset_uris,
                        workdir=self.workdir,
                        kw=self.kw,
                    ),
                    status=STATUS.INIT,
                    module=self.module,
                    workdir=self.workdir,
                )
            )
        return _tasks

    def run(self) -> None:
        while True:
            time.sleep(1)
            logger.debug("hang in producer")
            _next_steps = self.scheduler.get_next()
            if _next_steps is None:
                self.queue.put(None)
                break
            for _next in _next_steps:
                logger.debug(f"produce a step:{_next}")
                _executor = Executor(
                    _next.concurrency,
                    _next,
                    self._split_tasks(step=_next),
                    StepCallback(self.scheduler, _next),
                )
                self.queue.put(_executor)


class Consumer(threading.Thread):
    def __init__(self, queue: Queue):
        super().__init__()
        self.queue = queue

    def run(self) -> None:
        logger.debug("Consumer starting")
        # process items from the queue
        while True:
            logger.debug("hang in consumer")
            # get a step from the queue
            executor: Executor = self.queue.get()
            # check for signal that we are done
            if executor is None:
                break
            executor.exec()
            # mark the unit of work as processed
            self.queue.task_done()
        # mark the signal as processed
        self.queue.task_done()
        logger.debug("Consumer finished")
