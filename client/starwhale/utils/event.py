from __future__ import annotations

import os
import json
import queue
import atexit
import typing as t
import threading
from types import TracebackType
from functools import wraps

from starwhale.consts.env import SWEnv
from starwhale.utils.debug import console
from starwhale.base.uri.project import Project
from starwhale.base.client.api.event import EventApi
from starwhale.base.client.models.models import (
    Source,
    EventType,
    EventRequest,
    RelatedResource,
    EventResourceType,
)


def event(*deco_args: t.Any, **deco_kw: t.Any) -> t.Any:
    """Event decorator for recording event to server or standalone instance

    Arguments:
        msg: [str, optional] The event message. Default is the function name.
        external: [Dict, optional] The external data for the event. Default is None.
            The external data should be json serializable.
        event_type: [str|EventType, optional] The event type. Default is EventType.info.
            str accepts: INFO, WARNING, ERROR.
        ignore_end: [bool, optional] Whether to ignore the event when the function is finished. Default is False.
    """
    if len(deco_args) == 1 and len(deco_kw) == 0 and callable(deco_args[0]):
        return event()(deco_args[0])
    else:

        def _decorator(func: t.Callable) -> t.Any:
            @wraps(func)
            def _wrapper(*f_args: t.Any, **f_kw: t.Any) -> t.Any:
                msg = deco_kw.get("msg", f"func:{func.__qualname__}")
                external = deco_kw.get("external")
                event_type = deco_kw.get("event_type", EventType.info)
                ignore_end = deco_kw.get("ignore_end", False)

                add_event(msg if ignore_end else f"{msg}[start]", external, event_type)
                try:
                    _rt = func(*f_args, **f_kw)
                except Exception as e:
                    if not ignore_end:
                        add_event(f"{msg}[failed:{e}]", external, EventType.error)
                    raise
                else:
                    if not ignore_end:
                        add_event(f"{msg}[end]", external, event_type)

                return _rt

            return _wrapper

        return _decorator


def add_event(
    msg: str, external: t.Any = None, event_type: str | EventType = EventType.info
) -> None:
    with Event._lock:
        if Event._instance is None:
            Event._instance = Event()
        event = Event._instance

    event.add(msg, external, event_type)


class Event(threading.Thread):
    _instance: Event | None = None
    _lock = threading.Lock()

    def __init__(self, maxsize: int = 1000) -> None:
        super().__init__(name="EventThread")
        self.queue: queue.Queue[t.Tuple | None] = queue.Queue(maxsize)

        self.daemon = True
        self.start()
        atexit.register(self.close)

    def add(
        self,
        msg: str,
        external: t.Any = None,
        event_type: str | EventType = EventType.info,
    ) -> None:
        msg = msg.strip()
        if not msg:
            return

        if external:
            external = json.dumps(external)

        if isinstance(event_type, str):
            event_type = EventType(event_type)

        self.queue.put((msg, external, event_type))

    def __enter__(self) -> Event:
        return self

    def __exit__(
        self,
        type: t.Optional[t.Type[BaseException]],
        value: t.Optional[BaseException],
        trace: TracebackType,
    ) -> None:
        if value:  # pragma: no cover
            console.warning(f"type:{type}, exception:{value}, traceback:{trace}")

        self.close()

    def close(self) -> None:
        atexit.unregister(self.close)
        self.queue.put(None)

        self.flush()
        self.join()

    def flush(self) -> None:
        self.queue.join()

    def _dispatch_to_server(
        self,
        project: Project,
        msg: str,
        external: t.Any,
        event_type: EventType = EventType.info,
    ) -> None:
        job_id = os.environ.get(SWEnv.job_version)
        run_id = os.environ.get(SWEnv.run_id)
        if not job_id or not run_id:
            console.warning(
                f"failed to dispatch event({msg}) to server, "
                f"project={project} job_id={job_id} run_id={run_id}"
            )
            return

        EventApi(project.instance).add(
            project=project.id,
            job=job_id,
            event=EventRequest(
                event_type=event_type,
                source=Source.client,
                related_resource=RelatedResource(
                    event_resource_type=EventResourceType.run,
                    id=int(run_id),
                ),
                message=msg,
                data=external,
            ),
        ).raise_on_error()

    def run(self) -> None:
        while True:
            item = self.queue.get()
            if item is None:
                self.queue.task_done()
                break

            try:
                msg, external, typ = item
                console.info(
                    f"[EVENT][{typ.value}] msg='{msg}' \t external='{external}'"
                )
                project_uri = os.environ.get(SWEnv.project_uri)
                if project_uri:
                    project = Project(project_uri)
                else:
                    project = None

                if project and project.instance.is_cloud:
                    self._dispatch_to_server(project, msg, external, event_type=typ)
                # TODO: support event dispatch to standalone
            except Exception as e:
                # Event can ignore dispatch error, event is not critical
                console.exception(f"failed to dispatch event({item}): {e}")
            finally:
                self.queue.task_done()
