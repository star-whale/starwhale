from __future__ import annotations

import queue
import typing as t
import threading

from starwhale.base.uri.project import Project


class SyncerThread(threading.Thread):
    def __init__(self, sync_queue: queue.Queue[t.Any], project_uri: Project) -> None:
        super().__init__(name=f"SyncerThread-{project_uri}")

        self.queue = sync_queue
        self.project_uri = project_uri

        self.daemon = True

    def run(self) -> None:
        ...

    def close(self) -> None:
        ...
