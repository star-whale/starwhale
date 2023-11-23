from __future__ import annotations

import typing as t
from typing import List, Union, Optional

from starwhale.base.models.base import SwBaseModel
from starwhale.base.client.models.models import JobVo, TaskVo


class JobManifest(SwBaseModel):
    created_at: str
    scheduler_run_args: Optional[dict] = None
    version: str
    project: str
    model_src_dir: str
    datasets: Optional[List[str]] = None
    # for backward compatibility, old version may not have this field
    model: Optional[str] = None
    status: str
    handler_name: Optional[str] = None  # added from v0.5.12
    error_message: Optional[str] = None
    finished_at: str
    finetune_validation_datasets: Optional[List[str]] = None


class LocalJobInfo(SwBaseModel):
    manifest: JobManifest
    report: t.Optional[t.Dict[str, t.Any]] = None


class RemoteJobInfo(SwBaseModel):
    job: JobVo
    tasks: t.Optional[t.List[TaskVo]] = None
    report: t.Dict[str, t.Any]


JobListType = Union[t.List[LocalJobInfo], t.List[JobVo]]
