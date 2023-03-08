from __future__ import annotations

import shutil
import typing as t
import fnmatch
from abc import ABC
from pathlib import Path

from starwhale.utils.config import SWCliConfigMixed
from starwhale.base.blob.file import PathLike, BlakeFile, OptionalPathLike

BuiltinPyExcludes = ["__pycache__/", "*.py[cod]", "*$py.class"]


class ObjectStore(ABC):
    def get(self, key: str) -> t.Any:
        raise NotImplementedError

    def put(self, path: PathLike, key: t.Optional[str]) -> t.Any:
        raise NotImplementedError


# TODO: support file lock for concurrent access
class LocalFileStore(ObjectStore):
    def __init__(self, root: OptionalPathLike = None, soft_link: bool = False) -> None:
        if root is None:
            root = SWCliConfigMixed().object_store_dir
        self.root = Path(root)
        self.root.mkdir(parents=True, exist_ok=True)
        self.soft_link = soft_link

    def get(self, key: str) -> t.Union[BlakeFile, None]:
        """
        Get a file from the object store.
        Args:
            key: the hash of the file
        Returns: the BlakeFile object got or None if not found
        """
        if not key:
            raise ValueError("key cannot be empty")
        path = Path(self.root) / key[:2] / key
        if not path.exists():
            return None
        return BlakeFile(path, key)

    def put(self, path: PathLike, key: t.Optional[str] = None) -> BlakeFile:
        """
        Copy a file to the object store.
        Args:
            path: the file to be copied
            key: the hash of the file, if not provided, will be calculated
        Returns: the BlakeFile object putted
        """
        return self._put(path, key)

    def link(self, src: PathLike, key: t.Optional[str] = None) -> None:
        """
        Link a file to the object store. only support hard link for now.
        Args:
            src: the file links from
            key: the hash of the file, if not provided, will be calculated
        """
        file = BlakeFile(src, key)
        dst = Path(self.root) / file.hash[:2] / file.hash
        if dst.exists():
            return
        file.link(dst)

    def _put(self, path: PathLike, key: t.Optional[str] = None) -> BlakeFile:
        file = BlakeFile(path, key)
        dst = Path(self.root) / file.hash[:2] / file.hash
        if not dst.exists():
            dst.parent.mkdir(parents=True, exist_ok=True)
            shutil.copyfile(path, str(dst))
        return BlakeFile(dst, file.hash)

    def copy_dir(
        self,
        src_dir: PathLike,
        dst_dir: PathLike,
        excludes: t.Optional[t.List[str]] = None,
    ) -> None:
        """
        Copy a directory from src_dir to the dst_dir using the object store cache.
        When the file is already in the object store, it will be linked instead of copied.
        Args:
            src_dir: the source directory
            dst_dir: the destination directory
            excludes: the files to be excluded, default to BuiltinPyExcludes (python cache files)
        """
        src_dir = Path(src_dir)
        dst_dir = Path(dst_dir)
        excludes = excludes or BuiltinPyExcludes
        for src in src_dir.rglob("*"):
            exclude = any([fnmatch.fnmatch(str(src), ex) for ex in excludes])
            if not src.is_file() or exclude:
                continue
            file = self._put(path=src)
            dst = dst_dir / src.relative_to(src_dir)
            dst.parent.mkdir(parents=True, exist_ok=True)
            file.link(dst, soft=self.soft_link)
