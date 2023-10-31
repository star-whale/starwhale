from __future__ import annotations

import tarfile
import zipfile
from pathlib import Path

import requests
from tqdm import tqdm

from starwhale.utils import console


def _extract_zip(from_path: Path, to_path: Path) -> None:
    with zipfile.ZipFile(from_path, "r", zipfile.ZIP_STORED) as z:
        for file in tqdm(
            iterable=z.namelist(),
            total=len(z.namelist()),
            desc=f"extract {from_path.name}",
        ):
            z.extract(member=file, path=to_path)


def _extract_tar(from_path: Path, to_path: Path) -> None:
    with tarfile.open(from_path, "r") as t:
        for file in tqdm(
            iterable=t.getmembers(),
            total=len(t.getmembers()),
            desc=f"extract {from_path.name}",
        ):
            t.extract(member=file, path=to_path)


def extract(from_path: Path, to_path: Path, chk_path: Path) -> None:
    if not from_path.exists() or from_path.suffix not in (".zip", ".tar"):
        raise ValueError(f"invalid zip file: {from_path}")

    if chk_path.exists() and chk_path.is_dir():
        console.log(f"skip extract {from_path}, dir {chk_path} already exists")
        return

    console.log(f"extract {from_path} to {to_path} ...")
    if from_path.suffix == ".zip":
        _extract_zip(from_path, to_path)
    elif from_path.suffix == ".tar":
        _extract_tar(from_path, to_path)
    else:
        raise ValueError(f"invalid zip file: {from_path}")


def download(url: str, to_path: Path) -> None:
    if to_path.exists():
        console.log(f"skip download {url}, file {to_path} already exists")
        return

    to_path.parent.mkdir(parents=True, exist_ok=True)

    with requests.get(url, timeout=60, stream=True) as r:
        r.raise_for_status()
        size = int(r.headers.get("content-length", 0))
        with tqdm(
            total=size,
            unit="B",
            unit_scale=True,
            desc=f"download {url}",
            initial=0,
            unit_divisor=1024,
        ) as pbar:
            with to_path.open("wb") as f:
                for chunk in r.iter_content(chunk_size=8192):
                    f.write(chunk)
                    pbar.update(len(chunk))
