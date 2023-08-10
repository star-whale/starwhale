from __future__ import annotations

from .base import register, HuggingfaceLLMBase


@register()
class Baichuan7B(HuggingfaceLLMBase):
    def get_hf_repo_id(self) -> str:
        return "baichuan-inc/Baichuan-7B"

    @classmethod
    def get_name(cls) -> str:
        return "baichuan-7b"


@register()
class Baichuan13B(HuggingfaceLLMBase):
    def get_hf_repo_id(self) -> str:
        return "baichuan-inc/Baichuan-13B"

    @classmethod
    def get_name(cls) -> str:
        return "baichuan-13b"
