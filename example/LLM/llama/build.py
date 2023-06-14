from __future__ import annotations

import sys

from starwhale.utils import debug

try:
    from .utils import (
        SUPPORTED_MODELS,
        download_hf_model,
        DEFAULT_MODEL_NAME,
        prepare_model_package,
    )
    from .finetune import llama_fine_tuning
    from .evaluation import copilot_predict
except ImportError:
    from utils import (
        SUPPORTED_MODELS,
        download_hf_model,
        DEFAULT_MODEL_NAME,
        prepare_model_package,
    )
    from finetune import llama_fine_tuning
    from evaluation import copilot_predict


debug.init_logger(4)


def build_starwhale_model(model_name: str) -> None:
    """
    Build starwhale model from the raw llama base models.
    """
    if model_name not in SUPPORTED_MODELS:
        raise ValueError(f"model {model_name} not supported")

    print(f"try to download {model_name} from huggingface hub")
    download_hf_model(model_name)

    from starwhale import model as starwhale_model

    prepare_model_package(model_name, skip_adapter=True)
    starwhale_model.build(
        name=f"llama-{model_name}",
        modules=[llama_fine_tuning, copilot_predict],
    )


if __name__ == "__main__":
    argv = sys.argv[1:]
    if len(argv) == 0:
        model_name = DEFAULT_MODEL_NAME
    elif len(argv) == 1:
        model_name = argv[0]
    else:
        raise ValueError(f"invalid argv {argv}")

    build_starwhale_model(model_name)
