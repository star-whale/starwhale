from __future__ import annotations

import io
import os
import errno
import shutil
import typing as t
import difflib
import hashlib
import tarfile
import tempfile
from enum import IntEnum
from pathlib import Path

import requests

from starwhale.utils import console, timestamp_to_datatimestr
from starwhale.base.type import PathLike
from starwhale.utils.error import FormatError, ExistedError, NotFoundError
from starwhale.utils.retry import http_retry
from starwhale.utils.process import check_call

BLAKE2B_SIGNATURE_ALGO = "blake2b"
_MIN_GUESS_NAME_LENGTH = 5
DIGEST_SIZE = 32


class FilePosition(IntEnum):
    START = 0
    END = -1


def ensure_file(
    path: t.Union[str, Path],
    content: t.Union[str, bytes],
    mode: int = 0o644,
    parents: bool = False,
) -> None:
    p = Path(path)
    if parents:
        p.parent.mkdir(parents=True, exist_ok=True)
    bin_mode = isinstance(content, bytes)
    try:
        with p.open("rb" if bin_mode else "r") as f:
            _saved = f.read()
    except IOError as e:
        if e.errno == errno.ENOENT:
            # no such file or directory
            _saved = ""
        else:
            raise

    if _saved != content or not p.exists():
        _tmp_f = Path(tempfile.mktemp(dir=p.parent, suffix=f".{p.name}.tmp"))
        if isinstance(content, bytes):
            _tmp_f.write_bytes(content)
        elif isinstance(content, str):
            _tmp_f.write_text(content)
        else:
            raise TypeError(
                f"content({type(content)}-{content}) only accepts bytes or str type"
            )
        _tmp_f.replace(path)

    os.chmod(path, mode)


def empty_dir(
    p: t.Union[str, Path],
    ignore_errors: bool = False,
    onerror: t.Optional[t.Callable] = None,
) -> None:
    if not p:
        return

    path = Path(p)
    if not path.exists() or path.resolve() == Path("/"):
        return

    def _self_empty() -> None:
        if path.is_dir():
            shutil.rmtree(str(path.resolve()), ignore_errors, onerror)
        else:
            path.unlink()

    def _sudo_empty() -> None:
        console.print(
            f":bell: try to use [red bold]SUDO[/] privilege to empty : {path}"
        )
        check_call(f"sudo rm -rf {path}", shell=True)

    try:
        _self_empty()
    except PermissionError:
        _sudo_empty()


def ensure_dir(
    path: t.Union[str, Path], mode: int = 0o755, recursion: bool = True
) -> None:
    p = Path(path)
    if p.exists() and p.is_dir():
        return

    try:
        if recursion:
            os.makedirs(path, mode)
        else:
            os.mkdir(path, mode)
    except OSError as e:
        if e.errno != errno.EEXIST:
            # TODO: add more human friendly exception log
            raise


def ensure_link(src: t.Union[str, Path], dest: t.Union[str, Path]) -> None:
    src, dest = str(src), str(dest)
    dirname = os.path.dirname(dest)
    if dirname and not os.path.exists(dirname):
        os.makedirs(dirname)

    if os.path.lexists(dest):
        if os.path.islink(dest) and os.readlink(dest) == src:
            return
        os.unlink(dest)
    os.symlink(src, dest)


def blake2b_file(fpath: t.Union[str, Path]) -> str:
    _chunk_size = 100 * 1024 * 1024
    fpath = Path(fpath)
    # blake2b is more faster and better than md5,sha1,sha2
    _hash = hashlib.blake2b(digest_size=DIGEST_SIZE)

    with fpath.open("rb") as f:
        _chunk = f.read(_chunk_size)
        while _chunk:
            _hash.update(_chunk)
            _chunk = f.read(_chunk_size)

    return _hash.hexdigest()


def blake2b_content(content: bytes) -> str:
    _hash = hashlib.blake2b(digest_size=DIGEST_SIZE)
    _hash.update(content)
    return _hash.hexdigest()


def cmp_file_content(
    base_path: t.Union[str, Path], cmp_path: t.Union[str, Path]
) -> t.List[str]:
    base_path = Path(base_path)
    cmp_path = Path(cmp_path)
    res = []
    with base_path.open("r") as base, cmp_path.open("r") as cmp:
        diff = difflib.unified_diff(
            base.readlines(),
            cmp.readlines(),
            fromfile=base_path.name,
            tofile=cmp_path.name,
            n=0,
        )
        for line in diff:

            def skip(content: str) -> bool:
                prefixes = ["---", "+++", "@@", " "]
                return not any([content.startswith(prefix) for prefix in prefixes])

            if skip(content=line):
                res.append(line)
    return res


def get_path_created_time(p: Path) -> str:
    created_at = os.path.getctime(str(p.absolute()))
    return timestamp_to_datatimestr(created_at)


def guess_real_path(
    rootdir: Path, name: str, ftype: str = ""
) -> t.Tuple[Path, str, bool]:
    # TODO: support more guess method, such as tag
    _path = rootdir / name
    if _path.exists():
        return _path, name, True

    if not rootdir.exists():
        return _path, name, False

    if len(name) < _MIN_GUESS_NAME_LENGTH:
        return _path, name, False

    ftype = ftype.strip()
    for fd in rootdir.iterdir():
        if ftype and not fd.name.endswith(ftype):
            continue

        if fd.name.startswith(name) or name.startswith(fd.name):
            return fd, fd.name.rsplit(ftype, 1)[0] if ftype else fd.name, True
    else:
        return _path, name, False


def move_dir(src: Path, dest: Path, force: bool = False) -> t.Tuple[bool, str]:
    if not src.exists():
        return False, f"src:{src} not found"

    if dest.exists() and not force:
        return False, f"dest:{dest} existed"

    ensure_dir(dest.parent)

    try:
        shutil.move(str(src.absolute()), str(dest.absolute()))
    except Exception as e:
        return False, f"failed to move {src} -> {dest}, reason: {e}"
    else:
        return True, f"{src} move to {dest}"


def extract_tar(tar_path: Path, dest_dir: Path, force: bool = False) -> None:
    if not tar_path.exists():
        raise NotFoundError(tar_path)

    if dest_dir.exists() and not force:
        raise ExistedError(str(dest_dir))

    empty_dir(dest_dir)
    ensure_dir(dest_dir)

    with tarfile.open(tar_path, "r") as tar:
        for member in tar.getmembers():
            if not is_within_dir(dest_dir, dest_dir / member.name):
                raise Exception("Attempted path traversal in tar file")
        tar.extractall(path=str(dest_dir.absolute()))


def copy_file(src: Path, dest: Path) -> None:
    if not src.exists():
        raise NotFoundError(src)

    if not src.is_file():
        raise FormatError(f"{src} is not file type")

    ensure_dir(dest.parent)
    shutil.copyfile(str(src.absolute()), str(dest.absolute()))


def copy_dir(src_dir: Path, dest_dir: Path, force: bool = False) -> None:
    if not src_dir.exists():
        raise NotFoundError(src_dir)

    for src in src_dir.rglob("*"):
        if not src.is_file():
            continue
        dest = dest_dir / src.relative_to(src_dir)
        if dest.exists() and not force:
            raise ExistedError(str(dest))
        copy_file(src, dest)


def is_within_dir(parent: t.Union[str, Path], child: t.Union[str, Path]) -> bool:
    parent = str(parent)
    child = str(child)

    abs_parent = os.path.abspath(parent)
    abs_child = os.path.abspath(child)

    prefix = os.path.commonprefix([abs_parent, abs_child])
    return prefix == abs_parent


def file_stat(path: t.Union[str, Path]) -> os.stat_result:
    path = str(path)
    abs_path = os.path.abspath(path)
    return Path(abs_path).stat()


def iter_pathlike_io(
    path: PathLike | t.List[PathLike],
    encoding: str | None = None,
    newline: str | None = None,
    accepted_file_types: t.List[str] | None = None,
) -> t.Iterator[t.Tuple[io.IOBase, str]]:
    def _chk_type(p: PathLike) -> bool:
        if not accepted_file_types:
            return True

        return Path(p).suffix in accepted_file_types

    if isinstance(path, (list, tuple)):
        for p in path:
            yield from iter_pathlike_io(
                p,
                encoding=encoding,
                newline=newline,
                accepted_file_types=accepted_file_types,
            )
    elif isinstance(path, str) and path.startswith(("http://", "https://")):

        @http_retry
        def _r(url: str) -> str:
            r = requests.get(url, verify=False, timeout=90)
            if encoding:
                r.encoding = encoding
            return r.text

        if _chk_type(path):
            yield io.StringIO(_r(path), newline=newline), Path(path).suffix
    else:
        path = Path(path)

        if path.is_file():
            paths = [path]
        else:
            paths = [p for p in path.rglob("*") if p.is_file()]

        for p in paths:
            if _chk_type(p):
                with p.open(encoding=encoding, newline=newline) as f:
                    yield f, p.suffix
