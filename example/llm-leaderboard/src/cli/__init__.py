from __future__ import annotations

import time
import random
import typing as t

import click

from starwhale import init_logger


def create_llm_cli() -> click.core.Group:
    @click.group()
    @click.option(
        "verbose_cnt",
        "-v",
        "--verbose",
        count=True,
        help="Increase verbosity of output.",
    )
    def cli(verbose_cnt: int) -> None:
        init_logger(verbose_cnt)

    random.seed(time.time_ns())

    @cli.command("build", help="Build LLM as the Starwhale Model.")
    @click.option(
        "models",
        "-m",
        "--model",
        required=True,
        multiple=True,
        help="supported llm name",
    )
    @click.option(
        "tags",
        "-t",
        "--tag",
        multiple=True,
        help="Tags to add to the model. The option can be used multiple times.",
    )
    @click.option(
        "--skip-download",
        is_flag=True,
        default=False,
        help="Skip downloading the model weights.",
    )
    @click.option(
        "--push",
        default="",
        show_default=True,
        help="Copy model into the remote instance.",
    )
    def _build(
        models: t.List[str], tags: t.List[str], skip_download: bool, push: str
    ) -> None:
        from .build import build_starwhale_model, build_all_starwhale_models

        if models == ["all"]:
            build_all_starwhale_models(tags, skip_download, push)
        else:
            for model in models:
                build_starwhale_model(model, tags, skip_download, push)

    @cli.command(
        "submit", help="Submit LLMs to the Starwhale Cloud for Model Evaluation."
    )
    @click.option(
        "-r", "--resource-pool", help="Resource pool to use for the submission."
    )
    @click.option(
        "models",
        "-m",
        "--model",
        help="LLM Model, the option can be used multiple times.",
    )
    def _submit(resource_pool: str, models: t.List[str]) -> None:
        from .submit import submit

        submit(resource_pool, models)

    return cli


cli = create_llm_cli()
