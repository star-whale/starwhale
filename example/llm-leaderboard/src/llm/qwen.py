from __future__ import annotations

import torch

from .base import register, HuggingfaceLLMBase


@register()
class Qwen7b(HuggingfaceLLMBase):
    def get_hf_repo_id(self) -> str:
        return "Qwen/Qwen-7B"

    @classmethod
    def get_name(cls) -> str:
        return "qwen-7b"

    @property
    def enable_load_generation_config(self) -> bool:
        return True


@register()
class Qwen7bChat(HuggingfaceLLMBase):
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

    @property
    def enable_load_generation_config(self) -> bool:
        return True
