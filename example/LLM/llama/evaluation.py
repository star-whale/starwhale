import os
import typing as t

import gradio
from peft import PeftModel
from transformers import LlamaTokenizer, AutoModelForCausalLM

from starwhale import evaluation
from starwhale.api.service import api

try:
    from .utils import get_model_name, PRETRAINED_MODELS_DIR
except ImportError:
    from utils import get_model_name, PRETRAINED_MODELS_DIR

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
        if adapter_path.exists():
            _g_model = PeftModel.from_pretrained(_g_model, adapter_path)

    if _g_tokenizer is None:
        print(f"load tokenizer {model_name} into memory...")
        _g_tokenizer = LlamaTokenizer.from_pretrained(model_path)
        _g_tokenizer.bos_token_id = 1

    return _g_model, _g_tokenizer


@api(gradio.TextArea(), gradio.TextArea())
def _do_predict(input: str) -> str:
    model_name = get_model_name()
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
