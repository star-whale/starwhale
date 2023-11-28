from __future__ import annotations

import os
import typing as t

import torch
from transformers import AutoModel, AutoConfig, AutoTokenizer

from starwhale import evaluation
from starwhale.api.service import api, LLMChat

try:
    from .consts import PT_DIR, BASE_MODEL_DIR
except ImportError:
    from consts import PT_DIR, BASE_MODEL_DIR

_g_model = None
_g_tokenizer = None


def _load_model_and_tokenizer() -> t.Tuple:
    global _g_model, _g_tokenizer

    if _g_model is None:
        # TODO: after starwhale supports parameters, we can remove os environ.
        config = AutoConfig.from_pretrained(
            BASE_MODEL_DIR,
            trust_remote_code=True,
            pre_seq_len=int(os.environ.get("PT_PRE_SEQ_LEN", "128")),
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


@evaluation.predict(
    resources={"nvidia.com/gpu": 1},
    replicas=1,
    log_mode="plain",
)
def copilot_predict(data: dict) -> str:
    model, tokenizer = _load_model_and_tokenizer()
    print(data["prompt"])
    response, _ = model.chat(
        tokenizer,
        data["prompt"],
        history=[],
        max_length=int(os.environ.get("MAX_LENGTH", "512")),
        top_p=float(os.environ.get("TOP_P", "0.9")),
        temperature=float(os.environ.get("TEMPERATURE", "1.2")),
    )
    return response


@api(
    inference_type=LLMChat(
        args={"user_input", "history", "temperature", "top_p", "max_new_tokens"}
    )
)
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
