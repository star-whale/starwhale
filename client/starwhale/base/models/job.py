from __future__ import annotations

import typing as t
from typing import List, Optional

from pydantic import BaseModel

from starwhale.base.client.models.models import JobVo, TaskVo


class JobManifest(BaseModel):
    created_at: str
    scheduler_run_args: Optional[dict]
    version: str
    project: str
    model_src_dir: str
    datasets: Optional[List[str]]
    # for backward compatibility, old version may not have this field
    model: Optional[str]
    status: str
    handler_name: Optional[str]  # added from v0.5.12
    error_message: Optional[str]
    finished_at: str


class LocalJobInfo(BaseModel):
    manifest: JobManifest
    report: t.Optional[t.Dict[str, t.Any]] = None


class RemoteJobInfo(BaseModel):
    job: JobVo
    tasks: t.Optional[t.List[TaskVo]] = None
    report: t.Dict[str, t.Any]
