from __future__ import annotations

import typing as t
from pathlib import Path

ROOTDIR = Path(__file__).parent
# base models from https://huggingface.co/huggyllama
PRETRAINED_MODELS_DIR = ROOTDIR / "pretrained_models"
MODEL_NAME_PATH = ROOTDIR / ".model_name"
SWIGNORE_PATH = ROOTDIR / ".swignore"
DEFAULT_MODEL_NAME = "7b"

SUPPORTED_MODELS = {
    "7b": "huggyllama/llama-7b",
    "13b": "huggyllama/llama-13b",
    "30b": "huggyllama/llama-30b",
    "65b": "huggyllama/llama-65b",
}


def get_model_name() -> str:
    if MODEL_NAME_PATH.exists():
        model_name = MODEL_NAME_PATH.read_text()
    else:
        model_name = DEFAULT_MODEL_NAME
    return model_name.strip()


def get_base_and_adapter_model_path() -> t.Tuple[Path, Path]:
    model_name = get_model_name()
    base = PRETRAINED_MODELS_DIR / f"base-{model_name}"
    adapter = PRETRAINED_MODELS_DIR / f"adapter-{model_name}"
    return base, adapter


def download_hf_model(model_name: str) -> None:
    from huggingface_hub import snapshot_download as download

    base_repo = SUPPORTED_MODELS[model_name]
    download(repo_id=base_repo, local_dir=PRETRAINED_MODELS_DIR / f"base-{model_name}")


def update_swignore(model_name: str, skip_adapter: bool = False) -> None:
    if SWIGNORE_PATH.exists():
        lines = SWIGNORE_PATH.read_text().splitlines()
    else:
        lines = ["*/.git/*"]

    write_lines = []
    for line in lines:
        if "*/base-" in line or "*/adapter-" in line:
            continue
        write_lines.append(line)

    for m in SUPPORTED_MODELS:
        if m == model_name:
            if skip_adapter:
                write_lines.append(f"*/adapter-{m}/*")
            else:
                write_lines.append(f"*/adapter-{m}/checkpoint*")
                write_lines.append(f"*/adapter-{m}/runs")
        else:
            write_lines.append(f"*/base-{m}/*")
            write_lines.append(f"*/adapter-{m}/*")

    SWIGNORE_PATH.write_text("\n".join(write_lines))


def prepare_model_package(model_name: str, skip_adapter: bool = False) -> None:
    MODEL_NAME_PATH.write_text(model_name)
    update_swignore(model_name, skip_adapter=skip_adapter)
