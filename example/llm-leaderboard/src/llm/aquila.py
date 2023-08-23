from __future__ import annotations

from .base import register, HuggingfaceLLMBase


@register()
class Aquila7bChat(HuggingfaceLLMBase):
    def get_hf_repo_id(self) -> str:
        return "BAAI/AquilaChat-7B"

    @classmethod
    def get_name(cls) -> str:
        return "aquila-7b-chat"


@register()
class Aquila7b(HuggingfaceLLMBase):
    def get_hf_repo_id(self) -> str:
        return "BAAI/Aquila-7B"

    @classmethod
    def get_name(cls) -> str:
        return "aquila-7b"
