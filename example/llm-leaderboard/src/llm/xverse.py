from __future__ import annotations

import numpy
import torch
from transformers.generation.utils import GenerationConfig

from .base import register, LLMModelDesc, HuggingfaceLLMBase


class XverseBase(HuggingfaceLLMBase):
    @torch.no_grad()
    def _do_predict_with_generate(
        self, input_prompt: str, max_new_tokens: int = 50
    ) -> str:
        model = self._get_model()
        tokenizer = self._get_tokenizer()
        inputs = tokenizer(input_prompt, return_tensors="pt").to(model.device)
        inputs = {k: v for k, v in inputs.items() if k != "token_type_ids"}
        outputs = model.generate(
            **inputs,
            max_new_tokens=max_new_tokens,
            **self.get_generate_kwargs(),
        )
        return tokenizer.decode(
            outputs[0][len(inputs["input_ids"][0]) :], skip_special_tokens=True
        )

    @torch.no_grad()
    def _do_predict_with_logits(self, input_prompt: str) -> str:
        model = self._get_model()
        tokenizer = self._get_tokenizer()
        choices_info = self._get_choices_info()
        inputs = tokenizer(input_prompt, return_tensors="pt").to(model.device)
        inputs = {k: v for k, v in inputs.items() if k != "token_type_ids"}

        outputs = model(**inputs)
        last_logits = outputs.logits[:, -1, :]
        choice_logits = last_logits[:, choices_info["ids"]].detach().cpu().numpy()
        pred_prob_idx = numpy.argmax(choice_logits[0])
        return choices_info["idx_map"][pred_prob_idx]


@register()
class Xverse13B(XverseBase):
    def get_hf_repo_id(self) -> str:
        return "xverse/XVERSE-13B"

    @classmethod
    def get_name(cls) -> str:
        return "xverse-13b"

    @classmethod
    def get_description(cls) -> LLMModelDesc:
        return LLMModelDesc(
            params="13b",
            intro=(
                "XVERSE-13B is a multilingual large language model, independently developed by Shenzhen Yuanxiang Technology."
                "The model has been thoroughly trained on a diversified and high-quality dataset consisting of 1.4 trillion of tokens, including more than 40 languages such as Chinese, English, Russian, and Spanish."
            ),
            license="xverse",
            author="Shenzhen Yuanxiang Technology",
            github="https://github.com/xverse-ai/XVERSE-13B",
            type="pretrained",
        )


@register()
class Xverse13BChat(XverseBase):
    def get_hf_repo_id(self) -> str:
        return "xverse/XVERSE-13B-Chat"

    @classmethod
    def get_name(cls) -> str:
        return "xverse-13b-chat"

    @classmethod
    def get_description(cls) -> LLMModelDesc:
        return LLMModelDesc(
            params="13b",
            intro="XVERSE-13B-Chat is the aligned version of model XVERSE-13B",
            license="xverse",
            author="Shenzhen Yuanxiang Technology",
            github="https://github.com/xverse-ai/XVERSE-13B",
            type="fine-tuned",
        )

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
