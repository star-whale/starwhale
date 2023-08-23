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
    @click.option("-m", "--model", required=True, help="supported llm name")
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
    def _build(model: str, tags: t.List[str], skip_download: bool) -> None:
        from .build import build_starwhale_model

        build_starwhale_model(model, tags, skip_download)

    @cli.command("leaderboard", help="Show the LLMs leaderboard from starwhale cloud.")
    def _leaderboard() -> None:
        from .leaderboard import leaderboard

        leaderboard()

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
