from __future__ import annotations

from pathlib import Path

# huggingface checkpoint address mapping
# small (300M), text to music, # see: https://huggingface.co/facebook/musicgen-small
# medium (1.5B), text to music, # see: https://huggingface.co/facebook/musicgen-medium
# melody (1.5B) text to music and text+melody to music, # see: https://huggingface.co/facebook/musicgen-melody
# large (3.3B), text to music, # see: https://huggingface.co/facebook/musicgen-large
SUPPORTED_MODELS = {
    "melody": "facebook/musicgen-melody",
    "medium": "facebook/musicgen-medium",
    "small": "facebook/musicgen-small",
    "large": "facebook/musicgen-large",
}

ROOTDIR = Path(__file__).parent
PRETRAINED_MODELS_DIR = ROOTDIR / "pretrained_models"
MODEL_NAME_PATH = ROOTDIR / ".model_name"
SWIGNORE_PATH = ROOTDIR / ".swignore"

DEFAULT_MODEL_NAME = "medium"


def prepare_build_model_package(model_name: str) -> None:
    MODEL_NAME_PATH.write_text(model_name)
    update_swignore(model_name)


def update_swignore(model_name: str) -> None:
    write_lines = []

    for m in SUPPORTED_MODELS:
        if m != model_name:
            write_lines.append(f"*/{m}/")

    SWIGNORE_PATH.write_text("\n".join(write_lines))


def get_model_name() -> str:
    if MODEL_NAME_PATH.exists():
        model_name = MODEL_NAME_PATH.read_text()
    else:
        model_name = DEFAULT_MODEL_NAME
    return model_name.strip()
