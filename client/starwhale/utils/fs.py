import os
import errno
import shutil
import typing as t
import hashlib
import tarfile
from enum import IntEnum
from pathlib import Path

from starwhale.utils import console, timestamp_to_datatimestr
from starwhale.utils.error import FormatError, ExistedError, NotFoundError
from starwhale.utils.process import check_call

BLAKE2B_SIGNATURE_ALGO = "blake2b"
_MIN_GUESS_NAME_LENGTH = 5


class FilePosition(IntEnum):
    START = 0
    END = -1


def ensure_file(
    path: t.Union[str, Path],
    content: str,
    mode: int = 0o644,
) -> None:
    p = Path(path)
    try:
        with p.open("r") as f:
            _saved = f.read()
    except IOError as e:
        if e.errno == errno.ENOENT:
            # no such file or directory
            _saved = ""
        else:
            raise
    if _saved != content or not p.exists():
        # TODO: add timestamp for tmp file
        _tmp_f = p.parent / f".{p.name}.tmp"
        _tmp_f.write_text(content)
        # TODO: check whether rename atomic
        _tmp_f.rename(path)

    os.chmod(path, mode)


def empty_dir(p: t.Union[str, Path]) -> None:
    if not p:
        return

    path = Path(p)
    if not path.exists() or path.resolve() == Path("/"):
        return

    def _self_empty() -> None:
        if path.is_dir():
            shutil.rmtree(str(path.resolve()))
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
    _chunk_size = 8192
    fpath = Path(fpath)
    # blake2b is more faster and better than md5,sha1,sha2
    _hash = hashlib.blake2b(digest_size=64)

    with fpath.open("rb") as f:
        _chunk = f.read(_chunk_size)
        while _chunk:
            _hash.update(_chunk)
            _chunk = f.read(_chunk_size)

    return _hash.hexdigest()


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


def is_within_dir(parent: t.Union[str, Path], child: t.Union[str, Path]) -> bool:
    parent = str(parent)
    child = str(child)

    abs_parent = os.path.abspath(parent)
    abs_child = os.path.abspath(child)

    prefix = os.path.commonprefix([abs_parent, abs_child])
    return prefix == abs_parent
