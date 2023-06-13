from __future__ import annotations

import sys

from starwhale import model as starwhale_model
from starwhale.utils import debug

try:
    from .utils import (
        SUPPORTED_MODELS,
        PRETRAINED_MODELS_DIR,
        prepare_build_model_package,
    )
except ImportError:
    from utils import (
        SUPPORTED_MODELS,
        PRETRAINED_MODELS_DIR,
        prepare_build_model_package,
    )

debug.init_logger(4)


def build_starwhale_model(model_name: str) -> None:
    """
    Build starwhale model from raw llama model and delta vicuna model.
    """
    if model_name not in SUPPORTED_MODELS:
        raise ValueError(f"model {model_name} is not supported")

    print(f"try to download and merge vicuna {model_name} model")
    name = f"vicuna-{model_name}"
    vicuna_model_dir = PRETRAINED_MODELS_DIR / name
    if not (vicuna_model_dir / "config.json").exists():
        from fastchat.model.apply_delta import apply_delta

        llama_repo, vicuna_repo = SUPPORTED_MODELS[model_name]
        apply_delta(
            base_model_path=llama_repo,
            target_model_path=vicuna_model_dir,
            delta_path=vicuna_repo,
        )

    prepare_build_model_package(model_name)
    starwhale_model.build(name=name)


if __name__ == "__main__":
    model_name = sys.argv[1]
    build_starwhale_model(model_name)
