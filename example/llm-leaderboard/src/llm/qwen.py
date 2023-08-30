from __future__ import annotations

import typing as t

import torch

from .base import register, LLMModelDesc, HuggingfaceLLMBase


class QwenBase(HuggingfaceLLMBase):
    @property
    def enable_load_generation_config(self) -> bool:
        return True

    def get_pretrained_model_kwargs(self) -> t.Dict[str, t.Any]:
        kwargs = super().get_pretrained_model_kwargs()
        kwargs["fp16"] = True
        return kwargs


@register()
class Qwen7b(QwenBase):
    def get_hf_repo_id(self) -> str:
        return "Qwen/Qwen-7B"

    @classmethod
    def get_name(cls) -> str:
        return "qwen-7b"

    @classmethod
    def get_description(cls) -> LLMModelDesc:
        return LLMModelDesc(
            params="7b",
            intro=(
                "Qwen-7B is the 7B-parameter version of the large language model series, Qwen (abbr. Tongyi Qianwen), proposed by Alibaba Cloud."
                "Qwen-7B is a Transformer-based large language model, which is pretrained on a large volume of data, including web texts, books, codes, etc."
            ),
            license="qwen",
            author="Alibaba Cloud",
            github="https://github.com/QwenLM/Qwen-7B",
            type="pretrained",
        )


@register()
class Qwen7bChat(QwenBase):
    def get_hf_repo_id(self) -> str:
        return "Qwen/Qwen-7B-Chat"

    @classmethod
    def get_name(cls) -> str:
        return "qwen-7b-chat"

    @torch.no_grad()
    def _do_predict_with_generate(
        self, input_prompt: str, max_new_tokens: int = 50
    ) -> str:
        model = self._get_model()
        tokenizer = self._get_tokenizer()
        # Qwen-7b-chat no supports tokenizer.convert_tokens_to_ids for ABCD choices
        pred, _ = model.chat(
            tokenizer,
            input_prompt,
            history=[],
            max_new_tokens=max_new_tokens,
            **self.get_generate_kwargs(),
        )
        return pred

    @classmethod
    def get_description(cls) -> LLMModelDesc:
        return LLMModelDesc(
            params="7b",
            intro=(
                "Qwen-7b-chat is based on the pretrained Qwen-7B with alignment techniques."
                "Qwen-7b-chat is a large-model-based AI assistant."
            ),
            license="qwen",
            author="Alibaba Cloud",
            github="https://github.com/QwenLM/Qwen-7B",
            type="fine-tuned",
        )
