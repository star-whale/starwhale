import os
import sys
import time
import typing as t
import platform
import threading
from pathlib import Path
from collections import defaultdict

import psutil

from starwhale.utils import console
from starwhale.consts import FMT_DATETIME
from starwhale.utils.venv import guess_current_py_env
from starwhale.utils.error import NotFoundError
from starwhale.utils.dict_util import flatten

from .base import _TrackSource
from .tracker import Tracker


class CollectorThread(threading.Thread):
    def __init__(
        self,
        tracker: Tracker,
        workdir: t.Union[Path, str],
        sample_interval: float = 1.0,
        report_interval: float = 30.0,
        run_exceptions_limits: int = 100,
    ) -> None:
        super().__init__(name="CollectorThread")
        self.tracker = tracker
        workdir = Path(workdir)
        if not workdir.exists():
            raise NotFoundError(f"Collector workdir({workdir}) not found")
        self.workdir = workdir

        if sample_interval > report_interval:
            raise ValueError(
                f"sample interval({sample_interval}) should be less than report interval({report_interval})"
            )
        if sample_interval <= 0 or sample_interval > 60:
            raise ValueError(
                f"sample interval seconds({sample_interval}) must be between 0 and 60"
            )
        self.sample_interval = sample_interval

        if report_interval <= 0 or report_interval > 300:
            raise ValueError(
                f"report interval seconds({report_interval}) must be between 0 and 300"
            )
        self.report_interval = report_interval

        self._pid = os.getpid()
        self._staging_metrics: t.Dict = defaultdict(list)
        self._metrics_step = 0
        self._metrics_inspect_cnt = 0
        self._stopped = True
        self._stop_event = threading.Event()
        self._run_exceptions: t.List[Exception] = []

        self._run_exceptions_limits: int = max(run_exceptions_limits, 0)
        self.daemon = True

    def _raise_run_exceptions(self, limits: t.Optional[int] = None) -> None:
        limits = self._run_exceptions_limits if limits is None else limits
        if len(self._run_exceptions) > limits:
            raise threading.ThreadError(
                f"{self} run raise {len(self._run_exceptions)} exceptions: {self._run_exceptions}"
            )

    def run(self) -> None:
        self._stopped = False
        self._stop_event.clear()

        inspect_cases = [
            ("code info", self._inspect_code),
            ("python run info", self._inspect_python_run),
            ("system specs", self._inspect_system_specs),
            ("process info", self._inspect_process_specs),
            ("environments", self._inspect_environments),
        ]
        for _info, _action in inspect_cases:
            try:
                ret = _action()
                self.tracker._log_params(ret, source=_TrackSource.SYSTEM)
            except Exception:
                console.print(":warning: [red]{_info}[/red] inspect failed")
                console.print_exception()

        # TODO: tune the accuracy of inspect and report interval
        last_inspect_time = last_report_time = time.monotonic()
        check_interval = min(0.1, self.sample_interval)
        while True:
            if self._stop_event.wait(timeout=check_interval):
                break

            try:
                if time.monotonic() - last_inspect_time > self.sample_interval:
                    metrics = self._inspect_metrics()
                    for k, v in flatten(metrics).items():
                        self._staging_metrics[k].append(float(v))
                    last_inspect_time = time.monotonic()
                    self._metrics_inspect_cnt += 1

                if time.monotonic() - last_report_time > self.report_interval:
                    self._report_metrics()
                    last_report_time = time.monotonic()
            except Exception as e:
                console.print_exception()
                self._run_exceptions.append(e)
                self._raise_run_exceptions()

    def close(self) -> None:
        self._stop_event.set()
        self.join()
        self._stopped = True
        self._staging_metrics = defaultdict(list)
        self._raise_run_exceptions(0)

    def _inspect_code(self) -> t.Dict[str, t.Any]:
        # TODO: log code file as artifacts
        # TODO: support users custom to upload code scripts
        # TODO: make git fetch robust
        import git

        def _get_git_info() -> t.Dict[str, t.Any]:
            repo = git.Repo(self.workdir, search_parent_directories=True)
            commit = repo.head.commit
            return {
                "commit_sha": commit.hexsha,
                "message": commit.message,
                "author_name": commit.author.name,
                "author_email": commit.author.email,
                "date": commit.committed_datetime.strftime(FMT_DATETIME),
                "is_dirty": repo.is_dirty(untracked_files=True),
                "branch": repo.active_branch.name,
                "remote_urls": [r.url for r in repo.remotes],
            }

        try:
            git_info = _get_git_info()
        except git.InvalidGitRepositoryError:
            git_info = {}

        return {
            "code": {
                "git": git_info,
                "workdir": str(self.workdir),
            }
        }

    def _inspect_python_run(self) -> t.Dict[str, t.Any]:
        # TODO: add starwhale runtime, pip dependencies(pip requirements.txt and conda export.yaml) info
        return {
            "python": {
                "version": platform.python_version(),
                "bin_path": sys.executable,
                "env_mode": guess_current_py_env(),
            }
        }

    def _inspect_system_specs(self) -> t.Dict[str, t.Any]:
        # TODO: add more system specs: gpu, network, ip...
        return {
            "system": {
                "hostname": platform.node(),
                "platform": sys.platform,
                "os": platform.platform(),
                "arch": platform.machine(),
            },
            "hardware": {
                "cpu": {
                    "cores": psutil.cpu_count(logical=False),
                    "cores_logical": psutil.cpu_count(logical=True),
                },
                "memory": {"total_bytes": psutil.virtual_memory().total},
                "disk": {
                    "total_bytes": psutil.disk_usage("/").total,
                    "used_bytes": psutil.disk_usage("/").used,
                },
            },
        }

    def _inspect_process_specs(self) -> t.Dict[str, t.Any]:
        # TODO: get process more info
        return {
            "process": {
                "pid": self._pid,
                "cwd": str(self.workdir),
                "cmdline": " ".join(psutil.Process(self._pid).cmdline()),
            },
        }

    def _inspect_environments(self) -> t.Dict[str, t.Any]:
        return {
            "environments": dict(os.environ),
        }

    def _inspect_metrics(self) -> t.Dict[str, t.Any]:
        _process = psutil.Process(self._pid)
        # TODO: support more system and process metrics: network, io, memory
        system_cpu_percent = [float(v) for v in psutil.cpu_percent(percpu=True)]  # type: ignore
        cpu_cores = float(len(system_cpu_percent))
        return {
            "system": {
                "cpu": {
                    "usage_percent": sum(system_cpu_percent) / cpu_cores,
                },
            },
            "process": {
                "cpu": {
                    "usage_percent": _process.cpu_percent() / cpu_cores,
                },
                "num_threads": _process.num_threads(),
            },
        }

    def _report_metrics(self) -> None:
        data = {}
        for k, v in self._staging_metrics.items():
            if not v:
                continue

            data[f"{k}/last"] = float(v[-1])
            data[f"{k}/avg"] = round(sum(v) / len(v), 3)

        if not data:
            return

        self.tracker._log_metrics(
            data=data,
            step=self._metrics_step,
            commit=True,
            source=_TrackSource.SYSTEM,
        )

        self._metrics_step += 1
        self._staging_metrics = defaultdict(list)
