from __future__ import annotations

import sys

from starwhale import model as starwhale_model
from starwhale.utils import debug

try:
    from .utils import SUPPORTED_MODELS, DEFAULT_MODEL_NAME, prepare_model_package
    from .evaluation import chatbot, copilot_predict
except ImportError:
    from utils import SUPPORTED_MODELS, DEFAULT_MODEL_NAME, prepare_model_package
    from evaluation import chatbot, copilot_predict


debug.init_logger(3)


def build_starwhale_model(model_name: str) -> None:
    """
    Build starwhale model from the raw llama2 models.
    """
    if model_name not in SUPPORTED_MODELS:
        raise ValueError(f"model {model_name} not supported")

    prepare_model_package(model_name)
    starwhale_model.build(
        name=f"llama2-{model_name}",
        modules=[chatbot, copilot_predict],
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
