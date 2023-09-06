from __future__ import annotations

import typing as t

from starwhale.base.models.base import SwBaseModel
from starwhale.base.client.models.models import DatasetVo


class LocalDatasetInfoBase(SwBaseModel):
    name: str
    version: str
    project: str
    path: str
    tags: t.Optional[t.List[str]]
    created_at: str
    is_removed: bool
    size: int


DatasetListType = t.Union[t.List[LocalDatasetInfoBase], t.List[DatasetVo]]
