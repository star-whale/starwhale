from __future__ import annotations

import typing as t
import dataclasses

import torch
from transformers import AutoModel, AutoConfig, AutoTokenizer

from starwhale import argument, evaluation
from starwhale.api.service import api, LLMChat

try:
    from .consts import PT_DIR, BASE_MODEL_DIR
except ImportError:
    from consts import PT_DIR, BASE_MODEL_DIR

_g_model = None
_g_tokenizer = None


@dataclasses.dataclass
class ModelGenerateArguments:
    max_length: int = dataclasses.field(
        default=512, metadata={"help": "max length of generated text"}
    )
    top_p: float = dataclasses.field(default=0.9, metadata={"help": "top p"})
    temperature: float = dataclasses.field(
        default=1.2, metadata={"help": "temperature"}
    )
    pre_seq_len: int = dataclasses.field(default=128, metadata={"help": "pre seq len"})


def _load_model_and_tokenizer(pre_seq_len: int = 128) -> t.Tuple:
    global _g_model, _g_tokenizer

    if _g_model is None:
        config = AutoConfig.from_pretrained(
            BASE_MODEL_DIR,
            trust_remote_code=True,
            pre_seq_len=pre_seq_len,
        )
        _g_model = (
            AutoModel.from_pretrained(
                BASE_MODEL_DIR,
                config=config,
                device_map="cuda:0",
                torch_dtype=torch.float16,
                trust_remote_code=True,
            )
            .quantize(4)
            .cuda()
            .eval()
        )

        ptuning_path = PT_DIR / "pytorch_model.bin"
        if ptuning_path.exists():
            print(f"load p-tuning model: {ptuning_path}")
            prefix_state_dict = torch.load(ptuning_path)
            new_prefix_state_dict = {}
            for k, v in prefix_state_dict.items():
                if k.startswith("transformer.prefix_encoder."):
                    new_prefix_state_dict[k[len("transformer.prefix_encoder.") :]] = v
            _g_model.transformer.prefix_encoder.load_state_dict(new_prefix_state_dict)

    if _g_tokenizer is None:
        _g_tokenizer = AutoTokenizer.from_pretrained(
            BASE_MODEL_DIR, use_fast=False, trust_remote_code=True
        )

    return _g_model, _g_tokenizer


@argument(ModelGenerateArguments)
@evaluation.predict(
    resources={"nvidia.com/gpu": 1},
    replicas=1,
    log_mode="plain",
)
def copilot_predict(data: dict, argument: ModelGenerateArguments) -> str:
    model, tokenizer = _load_model_and_tokenizer(argument.pre_seq_len)
    print(data["prompt"])
    response, _ = model.chat(
        tokenizer,
        data["prompt"],
        history=[],
        max_length=argument.max_length,
        top_p=argument.top_p,
        temperature=argument.temperature,
    )
    return response


@api(inference_type=LLMChat())
def chatbot(user_input, history, temperature, top_p, max_new_tokens):
    model, tokenizer = _load_model_and_tokenizer()
    response, history = model.chat(
        tokenizer,
        user_input,
        history=history,
        max_new_tokens=max_new_tokens,
        top_p=top_p,
        temperature=temperature,
    )
    return history
