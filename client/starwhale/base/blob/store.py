from __future__ import annotations

import os
import sys
import shutil
import typing as t
import fnmatch
from abc import ABC
from pathlib import Path

from starwhale.utils import console, pretty_bytes
from starwhale.utils.venv import check_valid_venv_prefix, check_valid_conda_prefix
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
        try:
            file.link(dst)
        except FileExistsError:
            pass

    def _put(self, path: PathLike, key: t.Optional[str] = None) -> BlakeFile:
        file = BlakeFile(path, key)
        dst = Path(self.root) / file.hash[:2] / file.hash
        if not dst.exists():
            dst.parent.mkdir(parents=True, exist_ok=True)
            shutil.copyfile(path, str(dst))
        return BlakeFile(dst, file.hash)

    @staticmethod
    def _is_file_under_dir(file: Path, dir: Path) -> bool:
        """
        Check if the file is under the directory.
        Args:
            file: the file to be checked
            dir: the directory to be checked
        Returns: True if the file is under the directory
        """
        if sys.version_info < (3, 9):
            return str(file).startswith(str(dir))
        return file.is_relative_to(dir)

    @staticmethod
    def _check_if_is_py_env(path: PathLike) -> bool:
        """
        Check if the path is a virtual environment.
        Args:
            path: the path to be checked
        Returns: True if the path is a virtual environment or conda environment
        """
        return check_valid_venv_prefix(path) or check_valid_conda_prefix(path)

    @classmethod
    def _search_all_py_envs(cls, path: PathLike) -> t.List[Path]:
        """
        Search all virtual environments under the path.
        Args:
            path: the path to be searched
        Returns: a list of virtual environments or conda environments
        """
        envs = []
        for root, dirs, files in os.walk(path):
            if cls._check_if_is_py_env(root):
                envs.append(Path(root))
        return envs

    def copy_dir(
        self,
        src_dir: PathLike,
        dst_dir: PathLike,
        excludes: t.List[str],
        ignore_venv_or_conda: bool = True,
    ) -> int:
        """
        Copy a directory from src_dir to the dst_dir using the object store cache.
        When the file is already in the object store, it will be linked instead of copied.
        Args:
            src_dir: the source directory
            dst_dir: the destination directory
            excludes: the files to be excluded
            ignore_venv_or_conda: whether to ignore virtual environments or conda environments
        Returns: total file size in bytes
        """
        src_dir = Path(src_dir)
        dst_dir = Path(dst_dir)
        ignore_dirs = self._search_all_py_envs(src_dir) if ignore_venv_or_conda else []

        size = 0  # no record for soft link (inode not working in windows)

        for src in src_dir.rglob("*"):
            if not src.is_file():
                continue

            # check if the file under any of the ignored dirs
            ignore = False
            for ignore_dir in ignore_dirs:
                if self._is_file_under_dir(src, ignore_dir):
                    ignore = True
                    break
            if ignore or any(
                [fnmatch.fnmatch(str(src), str(src_dir / ex)) for ex in excludes]
            ):
                console.trace(f"ignore to copy file: {src}")
                continue

            file_size = src.stat().st_size
            size += file_size
            console.trace(f"copy file: {src}, size: {pretty_bytes(file_size)}")
            file = self._put(path=src)
            dst = dst_dir / src.relative_to(src_dir)
            dst.parent.mkdir(parents=True, exist_ok=True)
            file.link(dst, soft=self.soft_link)

        return size
