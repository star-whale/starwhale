import os
import sys
import typing as t
from pathlib import Path

import gradio
from peft import PeftModel
from transformers import LlamaTokenizer, AutoModelForCausalLM

from starwhale import evaluation
from starwhale.api.service import api

ROOTDIR = Path(__file__).parent
PRETRAINED_MODELS_DIR = ROOTDIR / "pretrained_models"
MODEL_NAME_PATH = ROOTDIR / ".model_name"
SWIGNORE_PATH = ROOTDIR / ".swignore"

SUPPORTED_MODELS = {
    "7b": ("decapoda-research/llama-7b-hf", "timdettmers/guanaco-7b"),
    "13b": ("decapoda-research/llama-13b-hf", "timdettmers/guanaco-13b"),
    "33b": ("decapoda-research/llama-30b-hf", "timdettmers/guanaco-33b"),
    "65b": ("decapoda-research/llama-65b-hf", "timdettmers/guanaco-65b"),
}
DEFAULT_MODEL_NAME = "7b"

_env = os.environ.get
max_new_tokens = int(_env("MAX_NEW_TOKENS", 1536))
temperature = int(_env("TEMPERATURE", 0.7))
top_p = float(_env("TOP_P", 0.9))
top_k = int(_env("TOP_K", 0))
repetition_penalty = float(_env("REPETITION_PENALTY", 1.1))

PROMPT_TEMPLATE = """
A chat between a curious human and an artificial intelligence assistant. The assistant gives helpful, detailed, and polite answers to the user's questions.
### Human: {question}\n
### Assistant:
"""


def download_hf_model(model_name: str) -> None:
    from huggingface_hub import snapshot_download as download

    base_repo, adapter_repo = SUPPORTED_MODELS[model_name]
    download(repo_id=base_repo, local_dir=PRETRAINED_MODELS_DIR / f"base-{model_name}")
    download(
        repo_id=adapter_repo, local_dir=PRETRAINED_MODELS_DIR / f"adapter-{model_name}"
    )


def _update_swignore(model_name: str) -> None:
    if SWIGNORE_PATH.exists():
        lines = SWIGNORE_PATH.read_text().splitlines()
    else:
        lines = []

    write_lines = []
    for line in lines:
        if "*/base-" in line or "*/adapter-" in line:
            continue
        write_lines.append(line)

    for m in SUPPORTED_MODELS:
        if m == model_name:
            continue
        write_lines.append(f"*/base-{m}/*")
        write_lines.append(f"*/adapter-{m}/*")

    SWIGNORE_PATH.write_text("\n".join(write_lines))


def build_starwhale_model(model_name: str) -> None:
    if model_name not in SUPPORTED_MODELS:
        raise ValueError(f"model {model_name} not supported")

    print(f"try to download {model_name} from huggingface hub")
    download_hf_model(model_name)

    from starwhale import model as starwhale_model
    from starwhale.utils import debug

    debug.init_logger(3)
    MODEL_NAME_PATH.write_text(model_name)

    _update_swignore(model_name)

    starwhale_model.build(name=f"guanaco-{model_name}")


_g_model = None
_g_tokenizer = None


def _load_model_and_tokenizer(model_name: str) -> t.Tuple:
    import torch

    global _g_model, _g_tokenizer

    model_path = PRETRAINED_MODELS_DIR / f"base-{model_name}"
    adapter_path = PRETRAINED_MODELS_DIR / f"adapter-{model_name}"

    if _g_model is None:
        print(f"load model {model_name} into memory...")
        _g_model = AutoModelForCausalLM.from_pretrained(
            model_path,
            torch_dtype=torch.bfloat16,
            device_map="auto",
        )
        _g_model = PeftModel.from_pretrained(_g_model, adapter_path)

    if _g_tokenizer is None:
        print(f"load tokenizer {model_name} into memory...")
        _g_tokenizer = LlamaTokenizer.from_pretrained(model_path)
        _g_tokenizer.bos_token_id = 1

    return _g_model, _g_tokenizer


@api(gradio.TextArea(), gradio.TextArea())
def _do_predict(input: str) -> str:
    if MODEL_NAME_PATH.exists():
        model_name = MODEL_NAME_PATH.read_text()
    else:
        model_name = DEFAULT_MODEL_NAME

    model, tokenizer = _load_model_and_tokenizer(model_name)
    prompt_question = PROMPT_TEMPLATE.format(question=input)
    input_ids = tokenizer(prompt_question, return_tensors="pt").input_ids.to(
        model.device
    )
    generated_ids = model.generate(
        input_ids=input_ids,
        max_new_tokens=max_new_tokens,
        temperature=temperature,
        do_sample=temperature > 0.0,
        top_p=top_p,
        top_k=top_k,
        repetition_penalty=repetition_penalty,
    )
    output = tokenizer.decode(generated_ids[0], skip_special_tokens=True)
    return output[len(prompt_question) :]


def _do_pre_process(data: dict, external: dict) -> str:
    supported_datasets = {
        "mkqa": "query",
        "z_ben_common": "prompt",
        "webqsp": "rawquestion",
        "vicuna": "text",
    }
    ds_name = external["dataset_uri"].name
    keyword = "question"
    for k, v in supported_datasets.items():
        if ds_name.startswith(k):
            keyword = v
            break
    return data[keyword]


@evaluation.predict(
    resources={"nvidia.com/gpu": 1},
    replicas=1,
    log_mode="plain",
    log_dataset_features=["query", "text", "question", "rawquestion", "prompt"],
)
def copilot_predict(data: dict, external: dict) -> str:
    question = _do_pre_process(data, external)
    answer = _do_predict(question)
    return answer


if __name__ == "__main__":
    argv = sys.argv[1:]
    if len(argv) == 0:
        action, model_name = "build", DEFAULT_MODEL_NAME
    elif len(argv) == 2:
        action, model_name = argv
    else:
        raise ValueError(f"invalid argv {argv}")

    if action == "build":
        build_starwhale_model(model_name)
    else:
        raise ValueError(f"invalid action {action}")
