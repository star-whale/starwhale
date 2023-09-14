from __future__ import annotations

import typing as t
from typing import Union

from starwhale import Resource
from starwhale.base.models.base import SwBaseModel
from starwhale.base.client.models.models import RuntimeVo


class LocalRuntimeVersion(SwBaseModel):
    name: str
    version: str
    path: str
    created_at: str
    size: int
    removed: bool
    tags: t.List[str]


class LocalRuntimeVersionInfoBasic(SwBaseModel):
    name: str
    uri: Resource
    tags: t.List[str]
    snapshot_workdir: str
    bundle_path: str


class LocalRuntimeVersionInfo(SwBaseModel):
    basic: LocalRuntimeVersionInfoBasic
    manifest: t.Dict[str, t.Any]  # TODO use manifest model
    yaml: str = ""  # TODO use yaml model
    lock: t.Dict[str, str]


RuntimeListType = Union[t.List[LocalRuntimeVersion], t.List[RuntimeVo]]
