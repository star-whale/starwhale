from __future__ import annotations

from transformers import LlamaTokenizer, PreTrainedTokenizerBase

from .base import register, HuggingfaceLLMBase


class LlamaBase(HuggingfaceLLMBase):
    def _get_tokenizer(self) -> PreTrainedTokenizerBase:
        if self._tokenizer is None:
            self._tokenizer = LlamaTokenizer.from_pretrained(
                self.get_base_dir(), trust_remote_code=True
            )
        return self._tokenizer


@register()
class Llama7B(LlamaBase):
    def get_hf_repo_id(self) -> str:
        return "huggyllama/llama-7b"

    @classmethod
    def get_name(cls) -> str:
        return "llama-7b"


@register()
class Llama13B(LlamaBase):
    def get_hf_repo_id(self) -> str:
        return "huggyllama/llama-13b"

    @classmethod
    def get_name(cls) -> str:
        return "llama-13b"


@register()
class Llama2_7B(LlamaBase):
    def get_hf_repo_id(self) -> str:
        return "meta-llama/Llama-2-7b-hf"

    @classmethod
    def get_name(cls) -> str:
        return "llama2-7b"


@register()
class Llama2_7B_Chat(LlamaBase):
    def get_hf_repo_id(self) -> str:
        return "meta-llama/Llama-2-7b-chat-hf"

    @classmethod
    def get_name(cls) -> str:
        return "llama2-7b-chat"


@register()
class Llama2_13B(LlamaBase):
    def get_hf_repo_id(self) -> str:
        return "huggyllama/llama-13b"

    @classmethod
    def get_name(cls) -> str:
        return "llama2-13b"


@register()
class Llama2_13B_Chat(LlamaBase):
    def get_hf_repo_id(self) -> str:
        return "meta-llama/Llama-2-13b-chat-hf"

    @classmethod
    def get_name(cls) -> str:
        return "llama2-13b-chat"


@register()
class LLama2_7B_Chinese(LlamaBase):
    def get_hf_repo_id(self) -> str:
        return "ziqingyang/chinese-llama-2-7b"

    @classmethod
    def get_name(cls) -> str:
        return "llama2-7b-chinese"


@register()
class LLama2_7B_ChineseAlpaca(LlamaBase):
    def get_hf_repo_id(self) -> str:
        return "ziqingyang/chinese-alpaca-2-7b"

    @classmethod
    def get_name(cls) -> str:
        return "llama2-7b-chinese-alpaca"


@register()
class LLama2_13B_Chinese(LlamaBase):
    def get_hf_repo_id(self) -> str:
        return "ziqingyang/chinese-llama-2-13b"

    @classmethod
    def get_name(cls) -> str:
        return "llama2-13b-chinese"


@register()
class LLama2_13B_ChineseAlpaca(LlamaBase):
    def get_hf_repo_id(self) -> str:
        return "ziqingyang/chinese-alpaca-2-13b"

    @classmethod
    def get_name(cls) -> str:
        return "llama2-13b-chinese-alpaca"
