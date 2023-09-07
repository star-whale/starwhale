from __future__ import annotations

import torch
from transformers.generation.utils import GenerationConfig

from .base import register, LLMModelDesc, HuggingfaceLLMBase


class BaichuanChatBase(HuggingfaceLLMBase):
    def enable_load_generation_config(self) -> bool:
        return True

    @torch.no_grad()
    def _do_predict_with_generate(
        self, input_prompt: str, max_new_tokens: int = 50
    ) -> str:
        model = self._get_model()
        tokenizer = self._get_tokenizer()
        messages = [{"role": "user", "content": input_prompt}]

        config_dict = model.generation_config.to_dict()
        config_dict.update(**self.get_generate_kwargs(), max_new_tokens=max_new_tokens)

        return model.chat(
            tokenizer,
            messages=messages,
            generation_config=GenerationConfig.from_dict(config_dict),
        )


@register()
class Baichuan_7B(HuggingfaceLLMBase):
    def get_hf_repo_id(self) -> str:
        return "baichuan-inc/Baichuan-7B"

    @classmethod
    def get_name(cls) -> str:
        return "baichuan-7b"

    @classmethod
    def get_description(cls) -> LLMModelDesc:
        return LLMModelDesc(
            params="7b",
            intro=(
                "Baichuan-7B is an open-source, large-scale pre-trained language model developed by Baichuan Intelligent Technology. "
                "Baichuan-7B is based on Transformer architecture, which contains 7 billion parameters and trained on approximately 1.2 trillion tokens. "
                "It supports both Chinese and English languages with a context window length of 4096. "
            ),
            license="baichuan",
            author="Baichuan Inc.",
            github="https://github.com/baichuan-inc/Baichuan-7B",
            type="pretrained",
        )


@register()
class Baichuan2_7B(HuggingfaceLLMBase):
    def get_hf_repo_id(self) -> str:
        return "baichuan-inc/Baichuan2-7B-Base"

    @classmethod
    def get_name(cls) -> str:
        return "baichuan2-7b"

    @classmethod
    def get_description(cls) -> LLMModelDesc:
        return LLMModelDesc(
            params="7b",
            intro=(
                "Baichuan 2 is the new generation of large-scale open-source language models launched by Baichuan Intelligence inc.."
                "It is trained on a high-quality corpus with 2.6 trillion tokens and has achieved the best performance in authoritative Chinese and English benchmarks of the same size."
                "Baichuan2-7b is the base model of Baichuan 2, which contains 7 billion parameters."
            ),
            license="baichuan",
            author="Baichuan Inc.",
            github="https://github.com/baichuan-inc/Baichuan2",
            type="pretrained",
        )


@register()
class Baichuan_13B(HuggingfaceLLMBase):
    def get_hf_repo_id(self) -> str:
        return "baichuan-inc/Baichuan-13B"

    @classmethod
    def get_name(cls) -> str:
        return "baichuan-13b"

    @classmethod
    def get_description(cls) -> LLMModelDesc:
        return LLMModelDesc(
            params="13b",
            intro=(
                "Baichuan-13B is an open-source, commercially available large-scale language model developed by Baichuan Intelligent Technology following Baichuan-7B, containing 13 billion parameters. "
                "It achieves the best results of the same size on both authoritative Chinese and English benchmarks."
            ),
            license="baichuan",
            author="Baichuan Inc.",
            github="https://github.com/baichuan-inc/Baichuan-13B",
            type="pretrained",
        )


@register()
class Baichuan2_13B(HuggingfaceLLMBase):
    def get_hf_repo_id(self) -> str:
        return "baichuan-inc/Baichuan2-13B-Base"

    @classmethod
    def get_name(cls) -> str:
        return "baichuan2-13b"

    @classmethod
    def get_description(cls) -> LLMModelDesc:
        return LLMModelDesc(
            params="13b",
            intro=(
                "Baichuan 2 is the new generation of large-scale open-source language models launched by Baichuan Intelligence inc.."
                "It is trained on a high-quality corpus with 2.6 trillion tokens and has achieved the best performance in authoritative Chinese and English benchmarks of the same size."
                "Baichuan2-13b is the base model of Baichuan 2, which contains 13 billion parameters."
            ),
            license="baichuan",
            author="Baichuan Inc.",
            github="https://github.com/baichuan-inc/Baichuan2",
            type="pretrained",
        )


@register()
class Baichuan_13BChat(BaichuanChatBase):
    def get_hf_repo_id(self) -> str:
        return "baichuan-inc/Baichuan-13B-Chat"

    @classmethod
    def get_name(cls) -> str:
        return "baichuan-13b-chat"

    @classmethod
    def get_description(cls) -> LLMModelDesc:
        return LLMModelDesc(
            params="13b",
            intro=(
                "Baichuan-13B-Chat is the aligned version in the Baichuan-13B series of models, and the pre-trained model can be found at Baichuan-13B-Base."
            ),
            license="baichuan",
            author="Baichuan Inc.",
            github="https://github.com/baichuan-inc/Baichuan-13B",
            type="fine-tuned",
        )


@register()
class Baichuan2_13BChat(BaichuanChatBase):
    def get_hf_repo_id(self) -> str:
        return "baichuan-inc/Baichuan2-13B-Chat"

    @classmethod
    def get_name(cls) -> str:
        return "baichuan2-13b-chat"

    @classmethod
    def get_description(cls) -> LLMModelDesc:
        return LLMModelDesc(
            params="13b",
            intro=(
                "Baichuan 2 is the new generation of large-scale open-source language models launched by Baichuan Intelligence inc.."
                "It is trained on a high-quality corpus with 2.6 trillion tokens and has achieved the best performance in authoritative Chinese and English benchmarks of the same size."
                "Baichuan2-13b-chat is chat model of Baichuan 2, which contains 13 billion parameters."
            ),
            license="baichuan",
            author="Baichuan Inc.",
            github="https://github.com/baichuan-inc/Baichuan2",
            type="fine-tuned",
        )


@register()
class Baichuan2_7BChat(BaichuanChatBase):
    def get_hf_repo_id(self) -> str:
        return "baichuan-inc/Baichuan2-7B-Chat"

    @classmethod
    def get_name(cls) -> str:
        return "baichuan2-7b-chat"

    @classmethod
    def get_description(cls) -> LLMModelDesc:
        return LLMModelDesc(
            params="7b",
            intro=(
                "Baichuan 2 is the new generation of large-scale open-source language models launched by Baichuan Intelligence inc.."
                "It is trained on a high-quality corpus with 2.6 trillion tokens and has achieved the best performance in authoritative Chinese and English benchmarks of the same size."
                "Baichuan2-7b-chat is chat model of Baichuan 2, which contains 7 billion parameters."
            ),
            license="baichuan",
            author="Baichuan Inc.",
            github="https://github.com/baichuan-inc/Baichuan2",
            type="fine-tuned",
        )
