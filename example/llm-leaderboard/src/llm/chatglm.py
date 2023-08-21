from __future__ import annotations

import os
import typing as t

import torch
from transformers import AutoModel

from starwhale.utils.debug import console

from .base import register, HuggingfaceLLMBase


class ChatGLMBase(HuggingfaceLLMBase):
    def _get_model(self) -> t.Any:
        if self._model is None:
            path = self.get_base_dir()
            console.print(f"Loading model from {path}...")

            self._model = (
                AutoModel.from_pretrained(
                    path,
                    trust_remote_code=True,
                    load_in_8bit=os.environ.get("LOAD_IN_8BIT", "0") == "1",
                )
                .half()
                .cuda()
            )
            self._model.eval()

        return self._model

    @torch.no_grad()
    def _do_predict_with_generate(
        self, input_prompt: str, max_new_tokens: int = 50
    ) -> str:
        model = self._get_model()
        tokenizer = self._get_tokenizer()
        pred, _ = model.chat(
            tokenizer,
            input_prompt,
            history=[],
            max_new_tokens=max_new_tokens,
            **self.get_generate_kwargs(),
        )
        return pred


@register()
class ChatGLM_6B(ChatGLMBase):
    def get_hf_repo_id(self) -> str:
        return "THUDM/chatglm-6b"

    @classmethod
    def get_name(cls) -> str:
        return "chatglm-6b"


@register()
class ChatGLM2_6B(ChatGLMBase):
    def get_hf_repo_id(self) -> str:
        return "THUDM/chatglm2-6b"

    @classmethod
    def get_name(cls) -> str:
        return "chatglm2-6b"
