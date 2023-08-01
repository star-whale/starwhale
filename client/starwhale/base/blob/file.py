from __future__ import annotations

import os
import typing as t
from pathlib import Path

from starwhale.utils.fs import blake2b_file
from starwhale.base.type import PathLike


class BlakeFile:
    def __init__(self, path: PathLike, blake: t.Optional[str] = None):
        self._blake = blake or blake2b_file(path)
        self._path = Path(path)

    @property
    def hash(self) -> str:
        return self._blake

    @property
    def path(self) -> Path:
        return self._path

    @property
    def size(self) -> int:
        return self._path.stat().st_size

    def exists(self) -> bool:
        return self._path.exists()

    def link(self, to: PathLike, soft: bool = False) -> None:
        Path(to).parent.mkdir(parents=True, exist_ok=True)
        # https://github.com/python/mypy/issues/10740
        # link: t.Callable[[Path, str], None] = os.symlink if soft else os.link
        os.symlink(self._path, str(to)) if soft else os.link(self._path, str(to))
