from __future__ import annotations

import typing as t

from llm import get_llm, get_supported_llm
from chat import chat_web, chat_terminal
from evaluation import predict_question, evaluation_results

from starwhale import model as starwhale_model
from starwhale.utils.debug import console


def build_starwhale_model(
    model_name: str,
    tags: t.List[str] | None = None,
    skip_download: bool = False,
    push_project: str = "",
) -> None:
    console.print(f":carrot: Building {model_name} ...")
    model = get_llm(model_name)

    if not skip_download:
        console.print(f"try to download {model_name} model ...")
        model.download()
    model.ensure_swignore()
    model.ensure_config_json()
    model.ensure_readme()
    starwhale_model.build(
        name=model.get_name(),
        modules=[predict_question, evaluation_results, chat_web, chat_terminal],
        tags=tags,
    )

    if push_project:
        from starwhale.core.model.view import ModelTermView

        ModelTermView.copy(model_name, push_project, force=True)

    console.print(f":clap: finish building {model_name} :beer:")


def build_all_starwhale_models(
    tags: t.List[str] | None = None,
    skip_download: bool = False,
    push_project: str = "",
) -> None:
    names = get_supported_llm()
    console.print(f"We will build and push {len(names)} models: {names}")
    for name in names:
        build_starwhale_model(
            model_name=name,
            tags=tags,
            skip_download=skip_download,
            push_project=push_project,
        )
