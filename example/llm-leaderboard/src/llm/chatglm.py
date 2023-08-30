from __future__ import annotations

import os
import typing as t

import torch
from transformers import AutoModel

from starwhale.utils.debug import console

from .base import register, LLMModelDesc, HuggingfaceLLMBase


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

    @classmethod
    def get_description(cls) -> LLMModelDesc:
        return LLMModelDesc(
            params="6b",
            intro=(
                "ChatGLM-6B is an open bilingual language model based on General Language Model (GLM) framework, with 6.2 billion parameters."
                "ChatGLM-6B uses technology similar to ChatGPT, optimized for Chinese QA and dialogue. "
                "The model is trained for about 1T tokens of Chinese and English corpus, supplemented by supervised fine-tuning, feedback bootstrap, and reinforcement learning wit human feedback. "
                "With only about 6.2 billion parameters, the model is able to generate answers that are in line with human preference."
            ),
            license="chatglm",
            author="THUDM",
            github="https://github.com/THUDM/ChatGLM-6B",
            type="pretrained",
        )


@register()
class ChatGLM2_6B(ChatGLMBase):
    def get_hf_repo_id(self) -> str:
        return "THUDM/chatglm2-6b"

    @classmethod
    def get_name(cls) -> str:
        return "chatglm2-6b"

    @classmethod
    def get_description(cls) -> LLMModelDesc:
        return LLMModelDesc(
            params="6b",
            intro=(
                "ChatGLM2-6B is the second-generation version of the open-source bilingual (Chinese-English) chat model ChatGLM-6B. "
                "It retains the smooth conversation flow and low deployment threshold of the first-generation model, while introducing the following new features:"
                "Stronger Performance, Longer Context, More Efficient Inference."
            ),
            license="chatglm2",
            author="THUDM",
            github="https://github.com/THUDM/ChatGLM2-6B",
            type="pretrained",
        )
