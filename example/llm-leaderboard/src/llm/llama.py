from __future__ import annotations

from transformers import LlamaTokenizer, PreTrainedTokenizerBase

from .base import register, LLMModelDesc, HuggingfaceLLMBase


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

    @classmethod
    def get_description(cls) -> LLMModelDesc:
        return LLMModelDesc(
            params="7b",
            intro=(
                "LLaMA (Large Language Model Meta AI), a state-of-the-art foundational large language model designed to help researchers advance their work in this subfield of AI."
                "Llama7b is a pretrained generative text models with 7 billion parameters."
            ),
            license="llama",
            author="Meta",
            github="https://github.com/facebookresearch/llama/tree/llama_v1",
            type="pretrained",
        )


@register()
class Llama13B(LlamaBase):
    def get_hf_repo_id(self) -> str:
        return "huggyllama/llama-13b"

    @classmethod
    def get_name(cls) -> str:
        return "llama-13b"

    @classmethod
    def get_description(cls) -> LLMModelDesc:
        return LLMModelDesc(
            params="13b",
            intro=(
                "LLaMA (Large Language Model Meta AI), a state-of-the-art foundational large language model designed to help researchers advance their work in this subfield of AI."
                "Llama13b is a pretrained generative text models with 13 billion parameters."
            ),
            license="llama",
            author="Meta",
            github="https://github.com/facebookresearch/llama/tree/llama_v1",
            type="pretrained",
        )


@register()
class Llama2_7B(LlamaBase):
    def get_hf_repo_id(self) -> str:
        return "meta-llama/Llama-2-7b-hf"

    @classmethod
    def get_name(cls) -> str:
        return "llama2-7b"

    @classmethod
    def get_description(cls) -> LLMModelDesc:
        return LLMModelDesc(
            params="7b",
            intro=(
                "LLaMA (Large Language Model Meta AI), a state-of-the-art foundational large language model designed to help researchers advance their work in this subfield of AI."
                "Llama2-7b is a pretrained generative text models with 7 billion parameters."
            ),
            license="llama",
            author="Meta",
            github="https://github.com/facebookresearch/llama/",
            type="pretrained",
        )


@register()
class Llama2_7B_Chat(LlamaBase):
    def get_hf_repo_id(self) -> str:
        return "meta-llama/Llama-2-7b-chat-hf"

    @classmethod
    def get_name(cls) -> str:
        return "llama2-7b-chat"

    @classmethod
    def get_description(cls) -> LLMModelDesc:
        return LLMModelDesc(
            params="7b",
            intro=(
                "LLaMA (Large Language Model Meta AI), a state-of-the-art foundational large language model designed to help researchers advance their work in this subfield of AI."
                "Llama2-7b-chat is a fine-tuned model that was trained for dialogue applications."
                "Llama2-7b-chat model outperform open-source chat models on most benchmarks we tested, and in our human evaluations for helpfulness and safety, are on par with some popular closed-source models like ChatGPT and PaLM."
            ),
            license="llama",
            author="Meta",
            github="https://github.com/facebookresearch/llama/",
            type="fine-tuned",
        )


@register()
class Llama2_13B(LlamaBase):
    def get_hf_repo_id(self) -> str:
        return "huggyllama/llama-13b"

    @classmethod
    def get_name(cls) -> str:
        return "llama2-13b"

    @classmethod
    def get_description(cls) -> LLMModelDesc:
        return LLMModelDesc(
            params="13b",
            intro=(
                "LLaMA (Large Language Model Meta AI), a state-of-the-art foundational large language model designed to help researchers advance their work in this subfield of AI."
                "Llama2-13b is a pretrained generative text models with 13 billion parameters."
            ),
            license="llama",
            author="Meta",
            github="https://github.com/facebookresearch/llama/",
            type="pretrained",
        )


@register()
class Llama2_13B_Chat(LlamaBase):
    def get_hf_repo_id(self) -> str:
        return "meta-llama/Llama-2-13b-chat-hf"

    @classmethod
    def get_name(cls) -> str:
        return "llama2-13b-chat"

    @classmethod
    def get_description(cls) -> LLMModelDesc:
        return LLMModelDesc(
            params="13b",
            intro=(
                "LLaMA (Large Language Model Meta AI), a state-of-the-art foundational large language model designed to help researchers advance their work in this subfield of AI."
                "Llama2-13b-chat is a fine-tuned model that was trained for dialogue applications."
                "Llama2-13b-chat model outperform open-source chat models on most benchmarks we tested, and in our human evaluations for helpfulness and safety, are on par with some popular closed-source models like ChatGPT and PaLM."
            ),
            license="llama",
            author="Meta",
            github="https://github.com/facebookresearch/llama/",
            type="fine-tuned",
        )


@register()
class LLama2_7B_Chinese(LlamaBase):
    def get_hf_repo_id(self) -> str:
        return "ziqingyang/chinese-llama-2-7b"

    @classmethod
    def get_name(cls) -> str:
        return "llama2-7b-chinese"

    @classmethod
    def get_description(cls) -> LLMModelDesc:
        return LLMModelDesc(
            params="7b",
            intro=(
                "Llama2-7b-chinese is a incremental pre-trained model based on Llama-2, released by Meta, and it is the second generation of the Chinese LLaMA & Alpaca LLM project."
                "The model has been expanded and optimized with Chinese vocabulary beyond the original Llama-2. The large scale Chinese data have been used for incremental pre-training."
            ),
            license="llama",
            author="ymcui",
            github="https://github.com/ymcui/Chinese-LLaMA-Alpaca-2",
            type="pretrained",
        )


@register()
class LLama2_7B_ChineseAlpaca(LlamaBase):
    def get_hf_repo_id(self) -> str:
        return "ziqingyang/chinese-alpaca-2-7b"

    @classmethod
    def get_name(cls) -> str:
        return "llama2-7b-chinese-alpaca"

    @classmethod
    def get_description(cls) -> LLMModelDesc:
        return LLMModelDesc(
            params="7b",
            intro=(
                "Llama2-7b-chinese-alpaca is a fine-tuned model(instruction-following) based on llama2-7b-chinese."
                "The model has been expanded and optimized with Chinese vocabulary beyond the original Llama-2. The large scale Chinese data have been used for incremental pre-training."
            ),
            license="llama",
            author="ymcui",
            github="https://github.com/ymcui/Chinese-LLaMA-Alpaca-2",
            type="fine-tuned",
        )


@register()
class LLama2_13B_Chinese(LlamaBase):
    def get_hf_repo_id(self) -> str:
        return "ziqingyang/chinese-llama-2-13b"

    @classmethod
    def get_name(cls) -> str:
        return "llama2-13b-chinese"

    @classmethod
    def get_description(cls) -> LLMModelDesc:
        return LLMModelDesc(
            params="13b",
            intro=(
                "Llama2-13b-chinese is a incremental pre-trained model based on Llama-2, released by Meta, and it is the second generation of the Chinese LLaMA & Alpaca LLM project."
                "The model has been expanded and optimized with Chinese vocabulary beyond the original Llama-2. The large scale Chinese data have been used for incremental pre-training."
            ),
            license="llama",
            author="ymcui",
            github="https://github.com/ymcui/Chinese-LLaMA-Alpaca-2",
            type="pretrained",
        )


@register()
class LLama2_13B_ChineseAlpaca(LlamaBase):
    def get_hf_repo_id(self) -> str:
        return "ziqingyang/chinese-alpaca-2-13b"

    @classmethod
    def get_name(cls) -> str:
        return "llama2-13b-chinese-alpaca"

    @classmethod
    def get_description(cls) -> LLMModelDesc:
        return LLMModelDesc(
            params="13b",
            intro=(
                "Llama2-13b-chinese-alpaca is a fine-tuned model(instruction-following) based on llama2-13b-chinese."
                "The model has been expanded and optimized with Chinese vocabulary beyond the original Llama-2. The large scale Chinese data have been used for incremental pre-training."
            ),
            license="llama",
            author="ymcui",
            github="https://github.com/ymcui/Chinese-LLaMA-Alpaca-2",
            type="fine-tuned",
        )
