import os
import shutil
import typing as t
import tarfile
import tempfile
from pathlib import Path

from starwhale.utils.error import FormatError, ExistedError


def pack(prefix: t.Union[str, Path], output: str, force: bool = False) -> None:
    if os.path.exists(output) and not force:
        raise ExistedError(output)

    from .venv import check_valid_venv_prefix

    if not check_valid_venv_prefix(prefix):
        raise FormatError(f"venv prefix: {prefix}")

    _, temp_fpath = tempfile.mkstemp(
        prefix="starwhale-venv-", dir=os.path.dirname(output)
    )

    # TODO: check editable packages
    try:
        with tarfile.open(temp_fpath, "w:gz") as tar:
            tar.add(str(prefix), arcname="")

        shutil.move(temp_fpath, output)
    finally:
        if os.path.exists(temp_fpath):
            os.unlink(temp_fpath)
