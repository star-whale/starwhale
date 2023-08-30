from __future__ import annotations

import torch

from .base import register, LLMModelDesc, HuggingfaceLLMBase


class AquilaBase(HuggingfaceLLMBase):
    @torch.no_grad()
    def _do_predict_with_generate(
        self, input_prompt: str, max_new_tokens: int = 50
    ) -> str:
        model = self._get_model()
        tokenizer = self._get_tokenizer()
        inputs = tokenizer.encode_plus(input_prompt)["input_ids"][:-1]
        # fmt: off
        inputs = torch.tensor(inputs)[None, ].to(model.device)
        # fmt: on
        stop_tokens = ["###", "[UNK]", "</s>"]

        outputs = model.generate(
            inputs,
            do_sample=True,
            eos_token_id=100007,
            bad_words_ids=[[tokenizer.encode(token)[0] for token in stop_tokens]],
            max_new_tokens=max_new_tokens,
            **self.get_generate_kwargs(),
        )
        return tokenizer.decode(outputs[0][len(inputs[0]) :], skip_special_tokens=True)


@register()
class Aquila7bChat(AquilaBase):
    def get_hf_repo_id(self) -> str:
        return "BAAI/AquilaChat-7B"

    @classmethod
    def get_name(cls) -> str:
        return "aquila-7b-chat"

    @classmethod
    def get_description(cls) -> LLMModelDesc:
        return LLMModelDesc(
            params="7b",
            intro=(
                "AquilaChat-7B is a conversational language model that supports Chinese-English dialogue. "
                "It is based on the Aquila-7B model and fine-tuned using SFT."
            ),
            license="aquila",
            author="BAAI",
            github="https://github.com/FlagAI-Open/FlagAI",
            type="fine-tuned",
        )


@register()
class Aquila7b(AquilaBase):
    def get_hf_repo_id(self) -> str:
        return "BAAI/Aquila-7B"

    @classmethod
    def get_name(cls) -> str:
        return "aquila-7b"

    @classmethod
    def get_description(cls) -> LLMModelDesc:
        return LLMModelDesc(
            params="7b",
            intro=(
                "The Aquila language model inherits the architectural design advantages of GPT-3 and LLaMA, replacing a batch of more efficient underlying operator implementations and redesigning the tokenizer for Chinese-English bilingual support. "
                "It upgrades the BMTrain parallel training method, achieving nearly 8 times the training efficiency of Magtron+DeepSpeed ZeRO-2 in the training process of Aquila. "
                "The Aquila language model is trained from scratch on high-quality Chinese and English corpora. "
                "Through data quality control and various training optimization methods, it achieves better performance than other open-source models with smaller datasets and shorter training times. "
                "It is also the first large-scale open-source language model that supports Chinese-English-Knowledge, commercial licensing, and complies with domestic data regulations."
            ),
            license="aquila",
            author="BAAI",
            github="https://github.com/FlagAI-Open/FlagAI",
            type="pretrained",
        )
