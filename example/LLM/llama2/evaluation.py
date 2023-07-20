from __future__ import annotations

import os
import typing as t
from pathlib import Path

import gradio

from starwhale import handler, evaluation
from starwhale.api.service import api

try:
    from .llama import Llama
    from .utils import get_base_model_path
except ImportError:
    from llama import Llama
    from utils import get_base_model_path

# hack for torchrun
os.environ["WORLD_SIZE"] = "1"
os.environ["RANK"] = "0"
os.environ["MASTER_ADDR"] = "localhost"
os.environ["MASTER_PORT"] = "1234"

ROOTDIR = Path(__file__).parent
PRETRAINED_MODELS_DIR = ROOTDIR / "pretrained_models"

_env = os.environ.get
max_new_tokens = int(_env("MAX_NEW_TOKENS", 1024))
max_seq_len = int(_env("MAX_SEQ_LEN", 10240))
max_batch_size = int(_env("MAX_BATCH_SIZE", 1))
temperature = int(_env("TEMPERATURE", 0.7))
top_p = float(_env("TOP_P", 0.9))
repetition_penalty = float(_env("REPETITION_PENALTY", 1.1))


_g_model = None


def _load_llama_model() -> Llama:
    global _g_model

    if _g_model is None:
        _g_model = Llama.build(
            ckpt_dir=str(get_base_model_path()),
            tokenizer_path=str(PRETRAINED_MODELS_DIR / "tokenizer.model"),
            max_seq_len=max_seq_len,
            max_batch_size=max_batch_size,
            model_parallel_size=1,
        )
    return _g_model


@api(gradio.TextArea(), gradio.TextArea())
def _do_predict(input: str) -> str:
    model = _load_llama_model()

    results = model.text_completion(
        prompts=[input],
        temperature=temperature,
        top_p=top_p,
        max_gen_len=max_new_tokens,
    )
    return results[0]["generation"]


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


@handler(expose=7860)
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

            model = _load_llama_model()
            results = model.chat_completion(
                dialogs=[dialog],
                max_gen_len=max_gen_len,
                top_p=top_p,
                temperature=temperature,
            )

            from_assistant = results[0]["generation"]["content"]
            chat_history.append((from_user, from_assistant))
            return "", chat_history

        msg.submit(
            response,
            [msg, chatbot, _max_gen_len, _top_p, _temperature],
            [msg, chatbot],
        )

    server.launch(server_name="0.0.0.0", server_port=7860, share=True)
