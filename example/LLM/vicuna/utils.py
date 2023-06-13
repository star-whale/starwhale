from __future__ import annotations

from pathlib import Path

SUPPORTED_MODELS = {
    "7b": ("huggyllama/llama-7b", "lmsys/vicuna-7b-delta-v1.1"),
    "13b": ("huggyllama/llama-13b", "lmsys/vicuna-13b-delta-v1.1"),
}

ROOTDIR = Path(__file__).parent
PRETRAINED_MODELS_DIR = ROOTDIR / "pretrained_models"
MODEL_NAME_PATH = ROOTDIR / ".model_name"
SWIGNORE_PATH = ROOTDIR / ".swignore"


def update_swignore(model_name: str) -> None:
    write_lines = []

    for m in SUPPORTED_MODELS:
        if m == model_name:
            continue
        write_lines.append(f"*/vicuna-{m}/")

    SWIGNORE_PATH.write_text("\n".join(write_lines))


def prepare_build_model_package(model_name: str) -> None:
    MODEL_NAME_PATH.write_text(model_name)
    update_swignore(model_name)
