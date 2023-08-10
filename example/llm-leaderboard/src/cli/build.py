from __future__ import annotations

import typing as t

from llm import get_llm
from chat import chat_web, chat_terminal
from evaluation import predict_question, evaluation_results

from starwhale import model as starwhale_model
from starwhale.utils.debug import console


def build_starwhale_model(
    model_name: str, tags: t.List[str] | None = None, skip_download: bool = False
) -> None:
    model = get_llm(model_name)

    if not skip_download:
        console.print(f"try to download {model_name} model...")
        model.download()
    model.ensure_swignore()
    model.ensure_config_json()
    starwhale_model.build(
        name=model.get_name(),
        modules=[predict_question, evaluation_results, chat_web, chat_terminal],
        tags=tags,
    )
