from __future__ import annotations

import os
import typing as t

import torch
import gradio
from peft import PeftModel
from transformers import AutoTokenizer, BitsAndBytesConfig, AutoModelForCausalLM
from transformers.generation.utils import GenerationConfig

from starwhale import handler, evaluation

try:
    from .utils import BASE_MODEL_DIR, ADAPTER_MODEL_DIR
except ImportError:
    from utils import BASE_MODEL_DIR, ADAPTER_MODEL_DIR

_g_model = None
_g_tokenizer = None


def _load_model_and_tokenizer() -> t.Tuple:
    global _g_model, _g_tokenizer

    if _g_model is None:
        print(f"load model from {BASE_MODEL_DIR} ...")
        _g_model = AutoModelForCausalLM.from_pretrained(
            BASE_MODEL_DIR,
            device_map="auto",
            torch_dtype=torch.float16,
            trust_remote_code=True,
            load_in_4bit=True,  # for lower gpu memory usage
            quantization_config=BitsAndBytesConfig(
                load_in_4bit=True,
                llm_int8_threshold=6.0,
                llm_int8_has_fp16_weight=False,
                bnb_4bit_compute_dtype=torch.float16,
                bnb_4bit_use_double_quant=True,
                bnb_4bit_quant_type="nf4",
            ),
        )
        _g_model.generation_config = GenerationConfig.from_pretrained(BASE_MODEL_DIR)

        if (ADAPTER_MODEL_DIR / "adapter_config.json").exists():
            print(f"load adapter from {ADAPTER_MODEL_DIR} ...")
            _g_model = PeftModel.from_pretrained(
                _g_model, str(ADAPTER_MODEL_DIR), is_trainable=False
            )

    if _g_tokenizer is None:
        print(f"load tokenizer from {BASE_MODEL_DIR} ...")
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
    # support z-bench-common dataset: https://cloud.starwhale.cn/projects/401/datasets/161/versions/223/files
    messages = [{"role": "user", "content": data["prompt"]}]

    config_dict = model.generation_config.to_dict()
    # TODO: use arguments
    config_dict.update(
        max_new_tokens=int(os.environ.get("MAX_MODEL_LENGTH", 512)),
        do_sample=True,
        temperature=float(os.environ.get("TEMPERATURE", 0.7)),
        top_p=float(os.environ.get("TOP_P", 0.9)),
        top_k=int(os.environ.get("TOP_K", 30)),
        repetition_penalty=float(os.environ.get("REPETITION_PENALTY", 1.3)),
    )
    return model.chat(
        tokenizer,
        messages=messages,
        generation_config=GenerationConfig.from_dict(config_dict),
    )


@handler(expose=17860)
def chatbot() -> None:
    with gradio.Blocks() as server:
        chatbot = gradio.Chatbot(height=800)
        msg = gradio.Textbox(label="chat", show_label=True)
        _max_gen_len = gradio.Slider(
            0, 1024, value=256, step=1.0, label="Max Gen Len", interactive=True
        )
        _top_p = gradio.Slider(
            0, 1, value=0.7, step=0.01, label="Top P", interactive=True
        )
        _temperature = gradio.Slider(
            0, 1, value=0.95, step=0.01, label="Temperature", interactive=True
        )
        gradio.ClearButton([msg, chatbot])

        def response(
            from_user: str,
            chat_history: t.List,
            max_gen_len: int,
            top_p: float,
            temperature: float,
        ) -> t.Tuple[str, t.List]:
            dialog = []
            for _user, _assistant in chat_history:
                dialog.append({"role": "user", "content": _user})
                if _assistant:
                    dialog.append({"role": "assistant", "content": _assistant})
            dialog.append({"role": "user", "content": from_user})

            model, tokenizer = _load_model_and_tokenizer()
            from_assistant = model.chat(
                tokenizer,
                messages=dialog,
                generation_config=GenerationConfig(
                    max_new_tokens=max_gen_len,
                    do_sample=True,
                    temperature=temperature,
                    top_p=top_p,
                ),
            )

            chat_history.append((from_user, from_assistant))
            return "", chat_history

        msg.submit(
            response,
            [msg, chatbot, _max_gen_len, _top_p, _temperature],
            [msg, chatbot],
        )

    server.launch(
        server_name="0.0.0.0",
        server_port=17860,
        share=True,
        root_path=os.environ.get(
            "SW_ONLINE_SERVING_ROOT_PATH"
        ),  # workaround for the embedded web page in starwhale server
    )
