import os
import typing as t
import tarfile
import tempfile
from pathlib import Path

from fs.copy import copy_fs

from starwhale.utils.error import FormatError, ExistedError


def pack(prefix: t.Union[str, Path], output: str, force: bool = False) -> None:
    if os.path.exists(output) and not force:
        raise ExistedError(output)

    from .fs import empty_dir
    from .venv import check_valid_venv_prefix

    if not check_valid_venv_prefix(prefix):
        raise FormatError(f"venv prefix: {prefix}")

    # TODO: check editable packages
    _, temp_dir = tempfile.mkdtemp(
        prefix="starwhale-venv-", dir=os.path.dirname(output)
    )

    try:
        copy_fs(str(prefix), temp_dir)

        with tarfile.open(output, "w:gz") as tar:
            tar.add(str(empty_dir), arcname="")
    finally:
        empty_dir(temp_dir)
