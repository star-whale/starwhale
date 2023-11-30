from __future__ import annotations

import sys
from pathlib import Path

from starwhale import model as starwhale_model

ROOT = Path(__file__).parent
CHECKPOINTS_DIR = ROOT / "checkpoints"

SUPPORT_MODELS = (
    "yolov8n",
    "yolov8s",
    "yolov8m",
    "yolov8l",
    "yolov8x",
    "yolov5nu",
    "yolov5su",
    "yolov5xu",
    "yolov5mu",
    "yolov5lu",
)


def build(model: str) -> None:
    print(f"start to build {model} yolo model...")
    fpath = CHECKPOINTS_DIR / "cache" / f"{model}.pt"
    if not fpath.exists():
        from torch.hub import download_url_to_file

        fpath.parent.mkdir(parents=True, exist_ok=True)
        download_url_to_file(
            url=f"https://github.com/ultralytics/assets/releases/download/v0.0.0/{model}.pt",
            dst=str(fpath),
        )

    (CHECKPOINTS_DIR / ".model").write_text(model)
    for pt in CHECKPOINTS_DIR.glob("*.pt"):
        pt.unlink()
    fpath.link_to(CHECKPOINTS_DIR / f"{model}.pt")

    starwhale_model.build(name=model, modules=["evaluation"])


if __name__ == "__main__":
    if len(sys.argv[1:]) == 0:
        print(f"please specify model name, supported: {SUPPORT_MODELS}")
        sys.exit(1)
    elif sys.argv[1] == "all":
        print("build all supported yolo models")
        models = SUPPORT_MODELS
    else:
        models = [sys.argv[1]]

    for model in models:
        build(model)
