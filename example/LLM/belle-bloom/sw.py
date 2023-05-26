from __future__ import annotations

import os
import sys
import typing as t
from pathlib import Path

import torch
import gradio
from transformers import AutoTokenizer, BloomForCausalLM

from starwhale import evaluation
from starwhale.api.service import api

ROOTDIR = Path(__file__).parent
PRETRAINED_MODELS_DIR = ROOTDIR / "pretrained_models"

gpu_device = torch.device("cuda")

# The minimum length of the sequence to be generated
min_length = int(os.environ.get("MIN_LENGTH", 5))
# The maximum length of the sequence to be generated
max_length = int(os.environ.get("MAX_LENGTH", 4096))
# If set to float < 1, only the smallest set of most probable tokens with probabilities that add up to top_p or higher are kept for generation
top_p = float(os.environ.get("TOP_P", 0.95))
# The temperature value used to module the next token probabilities
temperature = float(os.environ.get("TEMPERATURE", 0.8))
# Group size to use for quantization; default uses 128.
group_size = int(os.environ.get("GROUP_SIZE", 128))


class _PretrainedModelInfo(t.NamedTuple):
    name: str
    huggingface: str
    model: str
    desc: str
    wbits: int


# TODO: add other belle pretrained models info
pretrained_models_map = {
    "bloom-4bit": _PretrainedModelInfo(
        "BELLE_BLOOM_GPTQ_4BIT",
        "https://huggingface.co/BelleGroup/BELLE_BLOOM_GPTQ_4BIT",
        "bloom7b-2m-4bit-128g.pt",
        "Belle Bloom7B GPTQ 4bit",
        4,
    )
}


def build_starwhale_model(model_name: str) -> None:
    import subprocess

    if model_name not in pretrained_models_map:
        raise ValueError(f"model {model_name} not supported")

    info = pretrained_models_map[model_name]

    belle_dir = ROOTDIR / "BELLE"
    if not (belle_dir / ".git").exists():
        print("start downloading BELLE repo from github ...")
        print(
            subprocess.check_output(
                [
                    "git",
                    "clone",
                    "https://github.com/LianjiaTech/BELLE.git",
                    "--depth=1",
                ],
                cwd=ROOTDIR,
            ).decode()
        )
        # cf191f9 commit has been verified, other commits may be ok.
        print(
            subprocess.check_output(
                ["git", "checkout", "cf191f9d178326782e01dceacd8357d507b9aab8"],
                cwd=belle_dir,
            ).decode()
        )
    else:
        print("repo:BELLE already exists, skip downloading")

    model_dir = PRETRAINED_MODELS_DIR / info.name
    if not model_dir.exists():
        model_dir.mkdir(parents=True, exist_ok=True)
        print(
            f"start downloading {info.name} model from huggingface {info.huggingface} ..."
        )
        print(
            subprocess.check_output(["git", "lfs", "install"], cwd=model_dir).decode()
        )
        print(
            subprocess.check_output(
                ["git", "clone", info.huggingface], cwd=model_dir
            ).decode()
        )
    else:
        print(f"model:{info.name} already exists, skip downloading")

    from starwhale import model as starwhale_model
    from starwhale.utils import debug

    # show debug log for starwhale sdk
    debug.init_logger(2)
    starwhale_model.build(name=f"belle-{model_name}")


_g_model = None
_g_tokenizer = None


def load_model_and_tokenizer(
    model_name: str,
) -> t.Tuple[BloomForCausalLM, AutoTokenizer]:
    global _g_model, _g_tokenizer

    info = pretrained_models_map[model_name]
    model_rootdir = PRETRAINED_MODELS_DIR / info.name

    if _g_model is None:
        # hack belle library for relative import
        sys.path.insert(0, str(ROOTDIR / "BELLE" / "models" / "gptq"))
        from BELLE.models.gptq.bloom_inference import load_quant  # type: ignore

        _g_model = load_quant(
            model=str(model_rootdir),
            checkpoint=str(model_rootdir / info.model),
            wbits=info.wbits,
            groupsize=group_size,
        )
        _g_model.to(gpu_device)

    if _g_tokenizer is None:
        _g_tokenizer = AutoTokenizer.from_pretrained(model_rootdir)

    return _g_model, _g_tokenizer  # type: ignore


def _do_pre_process(data: dict, external: dict) -> str:
    supported_datasets = {
        "mkqa": "query",
        "mkqa-mini": "query",
        "z_bench_common": "prompt",
        "webqsp": "rawquestion",
    }
    ds_name = external["dataset_uri"].name
    keyword = "question"
    for k, v in supported_datasets.items():
        if ds_name.startswith(k):
            keyword = v
            break
    return data[keyword]


@api(gradio.Text(), gradio.Text())
def _do_predict(input: str) -> str:
    _model, _tokenizer = load_model_and_tokenizer("bloom-4bit")
    encode_input = _tokenizer.encode(input, return_tensors="pt").to(gpu_device)
    generated_ids = _model.generate(
        encode_input,
        do_sample=True,
        min_length=min_length,
        max_length=max_length,
        top_p=top_p,
        temperature=temperature,
    )
    output = _tokenizer.decode([el.item() for el in generated_ids[0]])
    output = output[len(input) :]
    if output.endswith("</s>"):
        output = output[: -len("</s>")]
    return output


@torch.no_grad()
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
        action, model_name = "build", "bloom-4bit"
    elif len(argv) == 2:
        action, model_name = argv
    else:
        raise ValueError(f"invalid argv {argv}")

    if action == "build":
        build_starwhale_model(model_name)
    else:
        raise ValueError(f"invalid action {action}")
