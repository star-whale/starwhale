from __future__ import annotations

import zipfile
from pathlib import Path

import requests
from tqdm import tqdm

from starwhale.utils import console

_COCO_CLASSES_MAP = None


def get_name_by_coco_category_id(category_id: int | None) -> str:
    global _COCO_CLASSES_MAP

    if _COCO_CLASSES_MAP is None:
        import sys

        sys.path.append(str(Path(__file__).parent.parent / "models/yolo"))

        from consts import COCO_CLASSES_MAP

        _COCO_CLASSES_MAP = COCO_CLASSES_MAP

    return (
        _COCO_CLASSES_MAP[category_id] if category_id is not None else "uncategorized"
    )


def extract_zip(from_path: Path, to_path: Path, chk_path: Path) -> None:
    if chk_path.exists():
        console.log(f"skip extract {from_path}, dir {chk_path} already exists")
        return

    with zipfile.ZipFile(from_path, "r", zipfile.ZIP_STORED) as z:
        for file in tqdm(
            iterable=z.namelist(),
            total=len(z.namelist()),
            desc=f"extract {from_path.name}",
        ):
            z.extract(member=file, path=to_path)


def download(url: str, to_path: Path) -> None:
    if to_path.exists():
        console.log(f"skip download {url}, file {to_path} already exists")
        return

    to_path.parent.mkdir(parents=True, exist_ok=True)

    with requests.get(url, timeout=60, stream=True) as r:
        r.raise_for_status()
        size = int(r.headers.get("content-length", 0))
        with tqdm(
            iterable=r.iter_content(chunk_size=1024),
            total=size,
            unit="B",
            unit_scale=True,
            desc=f"download {url}",
        ) as pbar:
            with open(to_path, "wb") as f:
                for chunk in pbar:
                    f.write(chunk)
                    pbar.update(len(chunk))
