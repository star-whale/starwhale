from __future__ import annotations

import typing as t
from pathlib import Path

ROOTDIR = Path(__file__).parent
PRETRAINED_MODELS_DIR = ROOTDIR / "pretrained_models"
MODEL_NAME_PATH = ROOTDIR / ".model_name"
SWIGNORE_PATH = ROOTDIR / ".swignore"
DEFAULT_MODEL_NAME = "7b-chat"

SUPPORTED_MODELS = {
    "7b": "llama-2-7b",
    "7b-chat": "llama-2-7b-chat",
    "13b": "llama-2-13b",
    "13b-chat": "llama-2-13b-chat",
    "70b": "llama-2-70b",
    "70b-chat": "llama-2-70b-chat",
    "7b-hf": "meta-llama/Llama-2-7b-hf",
    "7b-chat-hf": "meta-llama/Llama-2-7b-chat-hf",
    "13b-hf": "meta-llama/Llama-2-13b-hf",
    "13b-chat-hf": "meta-llama/Llama-2-13b-chat-hf",
    "70b-hf": "meta-llama/Llama-2-70b-hf",
    "70b-chat-hf": "meta-llama/Llama-2-70b-chat-hf",
}


def get_model_name() -> str:
    if MODEL_NAME_PATH.exists():
        model_name = MODEL_NAME_PATH.read_text()
    else:
        model_name = DEFAULT_MODEL_NAME
    return model_name.strip()


def get_base_model_path() -> Path:
    model_name = get_model_name()
    return PRETRAINED_MODELS_DIR / f"base-{model_name}"


def update_swignore(model_name: str) -> None:
    if SWIGNORE_PATH.exists():
        lines = SWIGNORE_PATH.read_text().splitlines()
    else:
        lines = ["*/.git/*"]

    write_lines = [line for line in lines if f"*/base-{model_name}" not in line]
    for m in SUPPORTED_MODELS:
        if m != model_name:
            write_lines.append(f"*/base-{m}/*")

    SWIGNORE_PATH.write_text("\n".join(write_lines))


def prepare_model_package(model_name: str) -> None:
    MODEL_NAME_PATH.write_text(model_name)
    update_swignore(model_name)


def download_hf_model(model_name: str) -> None:
    from huggingface_hub import snapshot_download

    base_repo = SUPPORTED_MODELS[model_name]
    snapshot_download(
        repo_id=base_repo, local_dir=PRETRAINED_MODELS_DIR / f"base-{model_name}"
    )


def preprocessed_input(input_s: str | t.List[t.Dict], typ: str) -> str:
    try:
        from .llama.generation import (
            B_SYS,
            E_SYS,
            B_INST,
            E_INST,
            DEFAULT_SYSTEM_PROMPT,
        )
    except ImportError:
        from llama.generation import B_SYS, E_SYS, B_INST, E_INST, DEFAULT_SYSTEM_PROMPT

    if typ == "text":
        if not isinstance(input_s, str):
            raise ValueError(f"invalid input {input_s}")
        # TODO: add some default prompt?
        return input_s
    else:
        if isinstance(input_s, str):
            dialogs = [{"role": "user", "content": input_s}]
        else:
            dialogs = input_s

        if dialogs[0]["role"] != "system":
            dialogs = [{"role": "system", "content": DEFAULT_SYSTEM_PROMPT}] + dialogs

        dialogs = [
            {
                "role": dialogs[1]["role"],
                "content": B_SYS
                + dialogs[0]["content"]
                + E_SYS
                + dialogs[1]["content"],
            }
        ] + dialogs[2:]

        full_prompt = ""
        for prompt, answer in zip(dialogs[::2], dialogs[1::2]):
            full_prompt += f"{B_INST} {(prompt['content']).strip()} {E_INST} {(answer['content']).strip()} "
        full_prompt += f"{B_INST} {(dialogs[-1]['content']).strip()} {E_INST}"
        return full_prompt
