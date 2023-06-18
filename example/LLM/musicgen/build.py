from __future__ import annotations

import sys

from huggingface_hub import snapshot_download

from starwhale import model as starwhale_model
from starwhale.utils import debug

try:
    from .utils import (
        SUPPORTED_MODELS,
        PRETRAINED_MODELS_DIR,
        prepare_build_model_package,
    )
    from .evaluation import music_predict
except ImportError:
    from utils import (
        SUPPORTED_MODELS,
        PRETRAINED_MODELS_DIR,
        prepare_build_model_package,
    )
    from evaluation import music_predict

debug.init_logger(4)


def build_starwhale_model(model_name: str) -> None:
    """
    Build starwhale model from musicgen pretrained models.
    """
    if model_name not in SUPPORTED_MODELS:
        raise ValueError(f"model {model_name} is not supported")

    print(f"try to download musicgen {model_name} model")
    snapshot_download(
        repo_id=SUPPORTED_MODELS[model_name],
        local_dir=PRETRAINED_MODELS_DIR / model_name,
    )

    prepare_build_model_package(model_name)
    starwhale_model.build(name=f"musicgen-{model_name}", modules=[music_predict])


if __name__ == "__main__":
    model_name = sys.argv[1]
    build_starwhale_model(model_name)
