from __future__ import annotations

import typing as t
from pathlib import Path

ROOTDIR = Path(__file__).parent
PRETRAINED_MODELS_DIR = ROOTDIR / "pretrained_models"
MODEL_NAME_PATH = ROOTDIR / ".model_name"
SWIGNORE_PATH = ROOTDIR / ".swignore"
DEFAULT_MODEL_NAME = "Stable-diffusion-v1-4"

SUPPORTED_MODELS = {
    "Stable-diffusion-v1-4": "CompVis/stable-diffusion-v1-4",
}


def get_model_name() -> str:
    if MODEL_NAME_PATH.exists():
        model_name = MODEL_NAME_PATH.read_text()
    else:
        model_name = DEFAULT_MODEL_NAME
    return model_name.strip()


def get_base_model_path() -> Path:
    model_name = get_model_name()
    return PRETRAINED_MODELS_DIR / f"base-{model_name}"


def update_swignore(model_name: str) -> None:
    if SWIGNORE_PATH.exists():
        lines = SWIGNORE_PATH.read_text().splitlines()
    else:
        lines = ["*/.git/*"]

    write_lines = [line for line in lines if f"*/base-{model_name}" not in line]
    for m in SUPPORTED_MODELS:
        if m != model_name:
            write_lines.append(f"*/base-{m}/*")

    SWIGNORE_PATH.write_text("\n".join(write_lines))


def prepare_model_package(model_name: str) -> None:
    MODEL_NAME_PATH.write_text(model_name)
    update_swignore(model_name)


def download_hf_model(model_name: str) -> None:
    from huggingface_hub import snapshot_download

    base_repo = SUPPORTED_MODELS[model_name]
    _model_path = PRETRAINED_MODELS_DIR / f"base-{model_name}"
    if _model_path.exists():
        return
    snapshot_download(
        repo_id=base_repo, local_dir=_model_path
    )
