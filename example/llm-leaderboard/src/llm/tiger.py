from __future__ import annotations

from .base import register, LLMModelDesc, HuggingfaceLLMBase


@register()
class TigerBot13B(HuggingfaceLLMBase):
    def get_hf_repo_id(self) -> str | None:
        return "TigerResearch/tigerbot-13b-base"

    @classmethod
    def get_name(cls) -> str:
        return "tigerbot-13b"

    @classmethod
    def get_description(cls) -> LLMModelDesc:
        return LLMModelDesc(
            params="13b",
            intro=(
                "TigerBot-13B is based on Llama-2-13B and continuing pre-training with an additional 300B tokens, "
                "the Chinese vocabulary is expanded to 60K vocabulary, and holistic training is adopted to directly give the model a capability of over 90% instruction completion during pre-training. "
                "It outperforms Llama-2-13B-base by 7% in mainstream English benchmark tests, exceeding Llama-2-13B-base by 49% in terms of comprehensive ability in Chinese tests, "
                "and occupying a leading position among mainstream open-source base models at home and abroad."
            ),
            license="llama",
            author="TigerResearch",
            github="https://github.com/TigerResearch/TigerBot",
            type="pretrained",
        )


@register()
class TigerBot13BChat(HuggingfaceLLMBase):
    def get_hf_repo_id(self) -> str | None:
        return "TigerResearch/tigerbot-13b-chat"

    @classmethod
    def get_name(cls) -> str:
        return "tigerbot-13b-chat"

    @classmethod
    def get_description(cls) -> LLMModelDesc:
        return LLMModelDesc(
            params="13b",
            intro=(
                "TigerBot-13B-Chat is based on TigerBot-13B-base and finetuned with 5M instructions data, rejection sampling fine-tuning is used to align human needs. "
                "In mainstream English benchmark tests, it reaches 101% of Llama-2-13B-chat's performance; in Chinese tests, its overall ability surpasses that of Llama-2-13B-chat by 47%, "
                "also maintaining a leading position compared to popular open-source models both domestically and internationally."
            ),
            license="llama",
            author="TigerResearch",
            github="https://github.com/TigerResearch/TigerBot",
            type="fine-tuned",
        )


@register()
class TigerBot7B(HuggingfaceLLMBase):
    def get_hf_repo_id(self) -> str | None:
        return "TigerResearch/tigerbot-7b-base"

    @classmethod
    def get_name(cls) -> str:
        return "tigerbot-7b"

    @classmethod
    def get_description(cls) -> LLMModelDesc:
        return LLMModelDesc(
            params="7b",
            intro=(
                "TigerBot-7B is based on Llama-2-7B and continuing pre-training with 1.5TB high-quality data."
            ),
            license="llama",
            author="TigerResearch",
            github="https://github.com/TigerResearch/TigerBot",
            type="pretrained",
        )


@register()
class TigerBot7BChat(HuggingfaceLLMBase):
    def get_hf_repo_id(self) -> str | None:
        return "TigerResearch/tigerbot-7b-chat"

    @classmethod
    def get_name(cls) -> str:
        return "tigerbot-7b-chat"

    @classmethod
    def get_description(cls) -> LLMModelDesc:
        return LLMModelDesc(
            params="7b",
            intro=(
                "TigerBot-7B-Chat is based on TigerBot-7B as the starting point for supervised tuning of chatting mode, "
                "this version has been fully trained using a dataset consisting of approximately 20 million (20 GB) "
                "high-quality cleaned and balanced transcript pairs from well curated dialogue corpora."
            ),
            license="llama",
            author="TigerResearch",
            github="https://github.com/TigerResearch/TigerBot",
            type="fine-tuned",
        )
