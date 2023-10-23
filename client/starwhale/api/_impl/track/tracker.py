from __future__ import annotations

import os
import copy
import json
import time
import queue
import atexit
import typing as t
import threading
from types import TracebackType
from pathlib import Path

import yaml

from starwhale.utils import console, now_str, random_str, gen_uniq_version
from starwhale.consts import SW_AUTO_DIRNAME, DEFAULT_MANIFEST_NAME
from starwhale.utils.fs import ensure_dir, ensure_file
from starwhale.consts.env import SWEnv
from starwhale.utils.error import NoSupportError
from starwhale.base.data_type import BaseArtifact
from starwhale.utils.dict_util import flatten
from starwhale.base.uri.project import Project

from .base import (
    _TrackMode,
    _TrackSource,
    ParamsRecord,
    MetricsRecord,
    ArtifactsRecord,
    HandleQueueElementType,
)
from .hooker import ConsoleLogHook, hook_frameworks, hook_python_libs
from .syncer import SyncerThread
from .handler import HandlerThread

_INSTANCE_NOT_INIT_ERROR = (
    "Tracker instance has not been initialized, please call track.start() at first"
)


# TODO: use multi-process model to refactor Tracer for GIL performance
class Tracker:
    _instance: t.Optional[Tracker] = None
    _lock = threading.Lock()

    def __init__(
        self,
        name: str,
        project_uri: Project,
        access_token: str = "",
        auto_run_collect: bool = True,
        auto_framework_hook: bool = True,
        auto_console_log_redirect: bool = True,
        mode: _TrackMode = _TrackMode.ONLINE,
        with_id: str = "",
        tags: t.Optional[t.List[str]] = None,
        description: str = "",
        collector_sample_interval: float = 1.0,
        collector_report_interval: float = 30.0,
        saved_dir: t.Optional[t.Union[str, Path]] = None,
    ) -> None:
        name = name.strip()
        if not name:
            raise ValueError("name field is empty")
        self.name = name

        if not project_uri.name:
            raise ValueError(
                f"{project_uri} is wrong format, that cannot fetch project field"
            )

        if project_uri.instance.is_cloud:
            access_token = (
                access_token
                or project_uri.instance.token
                or os.environ.get(SWEnv.instance_token, "")
            )
            if not access_token:
                raise ValueError("access_token is required for cloud instance")

        self.project_uri = project_uri
        self.access_token = access_token
        self.auto_run_collect = auto_run_collect
        self.auto_framework_hook = auto_framework_hook
        self.auto_console_log_redirect = auto_console_log_redirect
        # TODO: support envs setting for collector_*_interval
        self.collector_sample_interval = collector_sample_interval
        self.collector_report_interval = collector_report_interval
        self.mode = mode
        self.tags = tags or []
        self.description = description

        if saved_dir is None:
            saved_dir = Path.cwd()

        self._saved_dir = Path(saved_dir).absolute()

        self._id = with_id or gen_uniq_version()
        # TODO: support with_id argument from existed Tracker in the local storage
        self._metric_step = 0
        self._metric_lock = threading.Lock()
        self._uncommitted_metrics: t.Dict[str, float] = {}

        self._handle_queue: queue.Queue[HandleQueueElementType] = queue.Queue()
        self._sync_queue: queue.Queue[t.Any] = queue.Queue()
        self._threads: t.List[threading.Thread] = []
        self._handler_thread: t.Optional[threading.Thread] = None
        self._syncer_thread: t.Optional[threading.Thread] = None
        self._collector_thread: t.Optional[threading.Thread] = None

        self._start_time = time.monotonic()
        self._workdir: t.Optional[Path] = None
        self._log_hook: t.Optional[ConsoleLogHook] = None
        self._manifest: t.Dict[str, t.Any] = {"start_time": now_str()}

    def __str__(self) -> str:
        return f"Starwhale Tracker[{self.mode.value}]-{self.name}-{self._id}"

    def __repr__(self) -> str:
        return f"Starwhale Tracker[{self.mode.value}]-{self.name}-{self._id}-{self.project_uri}"

    def __enter__(self) -> Tracker:
        return self

    def __exit__(
        self,
        type: t.Optional[t.Type[BaseException]],
        value: t.Optional[BaseException],
        trace: TracebackType,
    ) -> None:
        if value:  # pragma: no cover
            console.warning(f"type:{type}, exception:{value}, traceback:{trace}")

        self.end()

    @property
    def id(self) -> str:
        return self._id

    # TODO: support re-init for Tracker, create multi trackers
    @classmethod
    def start(
        cls,
        name: str = "",
        project_uri: t.Optional[t.Union[str, Project]] = None,
        access_token: str = "",
        auto_run_collect: bool = True,
        auto_framework_hook: bool = True,
        auto_console_log_redirect: bool = True,
        mode: str = "online",
        with_id: str = "",
        tags: t.Optional[t.List[str]] = None,
        description: str = "",
        collector_sample_interval: float = 1.0,
        collector_report_interval: float = 30.0,
        saved_dir: t.Optional[t.Union[str, Path]] = None,
    ) -> Tracker:
        """Starts an new tracker to track metrics, artifacts and parameters to Starwhale.

        In your python script code, you should add `track.start()` to the beginning of training or evaluation code block,
        and every thing will be tracked to Starwhale.

        Arguments:
            name: (str, optional) The name of tracker. If the name is not specified,
                a random and human-readable name will be generated. Every name will be added an increment index suffix automatically.
            project_uri: (str, URI, optional) The project URI is the place where tracker sends records.
                If the project_uri is not specified, the default selected instance and project will be used. Standalone or Cloud instance
                project is ok.
            access_token: (str, optional) When project_uri is the cloud project, access_token is required. If access_token is not specified,
                starwhale will try to fetch token by `SW_TOKEN` environment or `~/.config/starwhale/config.yaml` config file.
            auto_run_collect: (bool, optional) When auto_environment value is `True`, Tracker will log program run environment information
                automatically. The run environment information includes cpu/gpu/mem/python/os and so on. `auto_environment` is `True` in default.
            auto_framework_hook: (bool, optional) Auto record MachineLearning Frameworks metrics. `auto_framework` is `True` in default.
            auto_console_log_redirect: (bool, optional) Tracker will catch program stdout and stderr console log. `auto_console_log` is `True` in default.
            mode: (str, optional) Config track mode. Options: `online` or `offline`. Defaults to `online`.
                cases:
                - `online`: sync track data to Starwhale Server.
                - `offline`: Only record track data in the local dir. You can run `swcli track track` manually.
            with_id: (str, optional) Reuse the existed Tracker. id is the tracker uuid.
            tags: (list[str], optional) Tags are used to organize, filter tracks.
            description: (str, optional) Tacker description.
            collector_sample_interval: (float, optional) the interval seconds of the run collector collect, default is 2.0 seconds.
            collector_report_interval: (float, optional) the interval seconds of the run collector report, all values will be aggregated to one value. default is 30.0 seconds.
            saved_dir: (str, pathlib.Path, optional) the saved dir for track, default is cwd dir.

        Examples:
        ```python
        from starwhale import track
        track.start()
        track.metrics(loss=0.768, accuracy=0.9)
        track.end()

        Returns:
            A `Tracker` object.
        ```
        """
        with Tracker._lock:
            if Tracker._instance is not None:
                raise RuntimeError(f"{Tracker._instance} has already been initialized")

            if project_uri is None:
                project_uri = Project()
            elif isinstance(project_uri, str):
                project_uri = Project(project_uri)
            elif isinstance(project_uri, Project):
                project_uri = project_uri
            else:
                raise TypeError(
                    f"project_uri({project_uri}) type is unexpected, only accepts str, URI or None"
                )

            name = name.strip()
            if not name:
                name = random_str()

            _tracker = Tracker(
                name=name,
                project_uri=project_uri,
                access_token=access_token,
                auto_run_collect=auto_run_collect,
                auto_framework_hook=auto_framework_hook,
                auto_console_log_redirect=auto_console_log_redirect,
                mode=_TrackMode(mode),
                with_id=with_id,
                tags=tags,
                description=description,
                collector_sample_interval=collector_sample_interval,
                collector_report_interval=collector_report_interval,
                saved_dir=saved_dir,
            )
            _tracker._setup()
            Tracker._instance = _tracker
            return _tracker

    @classmethod
    def end(cls) -> None:
        """Ends a track.
        Singleton Tracker instance will be released. If users don't call end function,
        Tracker will call end function when the process exiting automatically.
        """
        with Tracker._lock:
            if Tracker._instance is None:
                return

            _inst = Tracker._instance
            Tracker._instance = None

            _inst._render_manifest()
            _inst._close_threads()

            if _inst._log_hook is not None:
                _inst._log_hook.uninstall()
                _inst._log_hook = None

            atexit.unregister(Tracker.end)

    @classmethod
    def metrics(
        cls,
        *args: t.Dict,
        step: t.Optional[int] = None,
        commit: bool = True,
        **kwargs: float,
    ) -> Tracker:
        """Track metrics to Starwhale.
        Metric value type must be float or convertible-float type. Each call to `metrics` function will trigger
        internal step counter to be added by 1.

        Arguments:
            *args: [dict, optional] use dicts to store metrics.
            **kwargs: [float, optional] use kv to store metrics.
            step: [int, optional] custom step value.
            commit: [bool, optional] when `commit` is `false`, the metrics will be aggregated with the same step value
                until the next `track.metrics(commit=True)` function call. Defaults to `true`.

        Examples:
        ```python
        track.metrics(loss=0.99)
        track.metrics(loss=0.99, accuracy=0.98)
        track.metrics({"loss": 0.99, "accuracy": 0.98})

        # loss and accuracy use the same step.
        track.metrics(loss=0.99, commit=False)
        track.metrics(accuracy=0.98)

        # use custom step value
        track.metrics(loss=0.99, step=1)

        Returns:
            A `Tracker` object.
        ```
        """
        if cls._instance is None:
            raise RuntimeError(_INSTANCE_NOT_INIT_ERROR)

        data = cls._merge_dicts(args, kwargs)
        cls._instance._log_metrics(data=data, step=step, commit=commit)
        return cls._instance

    def _log_metrics(
        self,
        data: t.Dict[str, float],
        step: t.Optional[int] = None,
        commit: bool = False,
        source: _TrackSource = _TrackSource.USER,
    ) -> None:
        if not data:
            return

        _data = flatten(data, extract_sequence=True)
        for k, v in _data.items():
            _data[k] = float(v)

        _step: int = self._metric_step
        if step is not None:
            if not isinstance(step, int) or step < 0:
                raise TypeError(
                    f"step({step}) is unexpected, the normal step is non-negative integer"
                )
            _step = step

        with self._metric_lock:
            self._uncommitted_metrics.update(_data)
            if commit:
                self._handle_queue.put(
                    MetricsRecord(
                        data=copy.deepcopy(self._uncommitted_metrics),
                        step=_step,
                        source=source,
                        start_time=self._start_time,
                    )
                )
                self._metric_step = _step + 1
                self._uncommitted_metrics = {}

    @classmethod
    def _merge_dicts(cls, *args: t.Any, **kwargs: t.Any) -> t.Dict:
        data: t.Dict = {}
        for item in args:
            if not item:
                continue
            elif isinstance(item, tuple):
                for i in item:
                    if i and isinstance(i, dict):
                        data.update(i)
            elif isinstance(item, dict):
                data.update(item)
            else:
                raise TypeError(f"item{item} no support type")

        data.update(kwargs)
        return data

    @classmethod
    def params(cls, *args: t.Dict[str, t.Any], **kwargs: t.Any) -> Tracker:
        """Track parameters to Starwhale.
        We can use `track.params` to record hyperparameter and configuration. The arguments must be jsonable values.

        Arguments:
            *args: [dict, optional] use dicts to store parameters.
            **kwargs: [optional] use kv to store parameters.

        Examples:
        ```python
        track.params(model="multi-classification")
        track.params({"batch_size": 10})
        ```
        Returns:
            A `Tracker` object.
        """
        if cls._instance is None:
            raise RuntimeError(_INSTANCE_NOT_INIT_ERROR)
        data = cls._merge_dicts(args, kwargs)
        cls._instance._log_params(data)
        return cls._instance

    def _log_params(
        self, data: t.Dict[str, t.Any], source: _TrackSource = _TrackSource.USER
    ) -> None:
        if not data:
            return

        if data != json.loads(json.dumps(data)):
            raise TypeError(f"{data} keys should be string type")

        self._handle_queue.put(
            ParamsRecord(data=data, source=source, start_time=self._start_time)
        )

    @classmethod
    def artifacts(cls, *args: t.Dict[str, t.Any], **kwargs: t.Any) -> Tracker:
        """Track artifacts to Starwhale.

        Arguments:
            *args: [dict, optional] use dicts to store artifacts.
            **kwargs: [optional] use kv to store artifacts.

        Examples:
        ```python
        track.artifacts(model=Model("./model/mnist.pth"))
        track.artifacts(image=Image("sample.png"))
        ```
        Returns:
            A `Tracker` object.
        """
        if cls._instance is None:
            raise RuntimeError(_INSTANCE_NOT_INIT_ERROR)
        data = cls._merge_dicts(args, kwargs)
        cls._instance._log_artifacts(data)
        return cls._instance

    def _log_artifacts(
        self, data: t.Dict[str, t.Any], source: _TrackSource = _TrackSource.USER
    ) -> None:
        if not data:
            return

        _data = flatten(data, extract_sequence=True)
        for v in _data.values():
            # TODO: support more artifacts type
            if not isinstance(v, BaseArtifact):
                raise NoSupportError(f"v({v}:{type(v)}) not support for artifacts")

        self._handle_queue.put(
            ArtifactsRecord(data=_data, source=source, start_time=self._start_time)
        )

    def _close_threads(self) -> None:
        for _t in self._threads:
            if _t is None:
                continue
            _t.close()  # type: ignore

    def _setup_dirs(self) -> None:
        track_rootdir = self._saved_dir / SW_AUTO_DIRNAME / "track"
        ensure_dir(track_rootdir)

        target_idx = -1
        for d in track_rootdir.iterdir():
            if not d.is_dir() or "-" not in d.name:
                continue

            idx, tid = d.name.split("-", 1)
            if tid == self._id:
                self._workdir = track_rootdir / d.name
                return

            target_idx = max(target_idx, int(idx))

        self._manifest["track_incr_index"] = target_idx + 1
        self._workdir = track_rootdir / f"{target_idx + 1}-{self._id}"
        ensure_dir(self._workdir)
        ensure_dir(self._workdir / "logs")
        ensure_dir(self._workdir / "metrics")
        ensure_dir(self._workdir / "params")
        ensure_dir(self._workdir / "artifacts")

    def _setup(self) -> None:
        atexit.register(Tracker.end)
        self._setup_dirs()

        if self._workdir is None:
            raise RuntimeError("workdir is None")

        if self.auto_run_collect:
            from .collector import CollectorThread

            self._collector_thread = CollectorThread(
                tracker=self,
                sample_interval=self.collector_sample_interval,
                report_interval=self.collector_report_interval,
                workdir=self._workdir,
            )
            self._threads.append(self._collector_thread)

        if self.mode == _TrackMode.ONLINE:
            self._syncer_thread = SyncerThread(self._sync_queue, self.project_uri)
            self._threads.append(self._syncer_thread)
            _sync_queue = self._sync_queue
        else:
            _sync_queue = None

        self._handler_thread = HandlerThread(
            workdir=self._workdir,
            handle_queue=self._handle_queue,
            sync_queue=_sync_queue,
        )
        self._threads.append(self._handler_thread)

        for _t in self._threads:
            if _t is None:  # pragma: no cover
                continue
            _t.start()

        if self.auto_console_log_redirect and self._log_hook is None:
            self._log_hook = ConsoleLogHook(
                log_dir=self._workdir / "logs",
                track_id=self._id,
            )
            self._log_hook.install()

        if self.auto_framework_hook:
            hook_frameworks()
            hook_python_libs()

    def _render_manifest(self) -> None:
        if self._workdir is None:
            raise RuntimeError("can not fetch workdir")

        self._manifest.update(
            {
                "id": self._id,
                "name": self.name,
                "project_uri": str(self.project_uri),
                "end_time": now_str(),
                "auto": {
                    "run_collect": self.auto_run_collect,
                    "console_log_redirect": self.auto_console_log_redirect,
                    "framework_hook": self.auto_framework_hook,
                },
                "collector_interval": {
                    "sample": self.collector_sample_interval,
                    "report": self.collector_report_interval,
                },
                "mode": self.mode.value,
                "tags": self.tags,
                "description": self.description,
                "workdir": str(self._workdir),
            }
        )

        ensure_dir(self._workdir)
        ensure_file(
            self._workdir / DEFAULT_MANIFEST_NAME,
            yaml.safe_dump(self._manifest, default_flow_style=False),
        )
