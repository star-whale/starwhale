from __future__ import annotations

from .base import register, vLLMBase, LLMModelDesc


@register()
class Mistral7BInstruct(vLLMBase):
    def get_hf_repo_id(self) -> str:
        return "mistralai/Mistral-7B-Instruct-v0.2"

    @classmethod
    def get_name(cls) -> str:
        return "mistral-7b-instruct"

    @classmethod
    def get_description(cls) -> LLMModelDesc:
        return LLMModelDesc(
            params="7b",
            intro=(
                "We introduce Mistral 7B v0.1, a 7-billion-parameter language model engineered for superior performance and efficiency. "
                "Mistral 7B outperforms Llama 2 13B across all evaluated benchmarks, and Llama 1 34B in reasoning, mathematics, and code generation."
                "Our model leverages grouped-query attention (GQA) for faster inference, coupled with sliding window attention (SWA) to effectively handle sequences of arbitrary length with a reduced inference cost. "
                "We also provide a model fine-tuned to follow instructions, Mistral 7B -- Instruct, that surpasses the Llama 2 13B -- Chat model both on human and automated benchmarks. Our models are released under the Apache 2.0 license."
            ),
            license="apache-2.0",
            author="Mistral",
            github="https://github.com/mistralai/mistral-src",
            type="fine-tuned",
        )

    def download(self) -> None:
        super().download()

        local_dir = self.get_base_dir()
        # We only need safetensors files.
        useless_fnames = (
            "pytorch_model-00001-of-00003.bin",
            "pytorch_model-00002-of-00003.bin",
            "pytorch_model-00003-of-00003.bin",
            "pytorch_model.bin.index.json",
        )
        for fname in useless_fnames:
            (local_dir / fname).unlink()
