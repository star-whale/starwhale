from __future__ import annotations

import os
import json
import typing as t
from abc import ABC, abstractmethod
from pathlib import Path

import numpy
import torch
from peft import PeftModel
from transformers import AutoTokenizer, AutoModelForCausalLM, PreTrainedTokenizerBase

from starwhale.utils.fs import ensure_dir, ensure_file
from starwhale.utils.debug import console

try:
    from ..benchmark import BenchmarkType
except ImportError:
    from benchmark import BenchmarkType


class LLMBase(ABC):
    def __init__(self, rootdir: Path | None = None) -> None:
        self.rootdir = rootdir if rootdir is not None else Path.cwd()

    @classmethod
    @abstractmethod
    def get_name(cls) -> str:
        raise NotImplementedError

    @abstractmethod
    def download(self) -> None:
        raise NotImplementedError

    def ensure_swignore(self) -> None:
        swi_path = self.rootdir / ".swignore"

        if swi_path.exists():
            existed_lines = swi_path.read_text().splitlines()
        else:
            existed_lines = ["*/.git/*"]

        writing_lines = [i for i in existed_lines if not i.startswith("pretrained/")]

        for d in self.get_pretrained_dir().iterdir():
            if d.is_dir() and d.name != self.get_name():
                writing_lines.append(f"pretrained/{d.name}/*")

        ensure_file(swi_path, "\n".join(writing_lines), parents=True)

    def ensure_config_json(self) -> None:
        config_path = self.get_pretrained_dir() / "sw_config.json"
        ensure_file(config_path, json.dumps({"name": self.get_name()}))

    def get_pretrained_dir(self) -> Path:
        return self.rootdir / "pretrained"

    @abstractmethod
    def do_predict(
        self,
        input_prompt: str,
        benchmark_type: BenchmarkType = BenchmarkType.MultipleChoice,
        max_new_tokens: int = 50,
    ) -> t.Any:
        raise NotImplementedError

    def calculate_tokens_length(self, input_prompt: str) -> int:
        return len(input_prompt)


class HuggingfaceLLMBase(LLMBase):
    def __init__(self, rootdir: Path | None = None) -> None:
        super().__init__(rootdir)

        self._tokenizer = None
        self._model = None

    @abstractmethod
    def get_hf_repo_id(self) -> str:
        raise NotImplementedError

    def get_hf_adapter_repo_id(self) -> str | None:
        return None

    def get_base_dir(self) -> Path:
        return self.get_pretrained_dir() / self.get_name() / "base"

    def get_adapter_dir(self) -> Path:
        return self.get_pretrained_dir() / self.get_name() / "adapter"

    def download(self) -> None:
        self.download_base()
        self.download_adapter()

    def download_base(self) -> None:
        from huggingface_hub import snapshot_download

        local_dir = self.get_base_dir()
        ensure_dir(local_dir)
        snapshot_download(
            repo_id=self.get_hf_repo_id(), local_dir=local_dir, max_workers=16
        )

    def download_adapter(self) -> None:
        """Download adapter(lora/qlora) from HuggingFace, it is optional"""
        repo_id = self.get_hf_adapter_repo_id()

        if not repo_id:
            return

        from huggingface_hub import snapshot_download

        local_dir = self.get_adapter_dir()
        ensure_dir(local_dir)
        snapshot_download(repo_id=repo_id, local_dir=local_dir, max_workers=16)

    def _get_tokenizer(self) -> PreTrainedTokenizerBase:
        if self._tokenizer is None:
            self._tokenizer = AutoTokenizer.from_pretrained(
                self.get_base_dir(), trust_remote_code=True
            )

        return self._tokenizer

    def _get_model(self) -> t.Any:
        if self._model is None:
            import torch

            path = self.get_base_dir()
            console.print(f":monkey: try to load base model({path}) into memory...")

            # TODO: support custom kwargs
            self._model = AutoModelForCausalLM.from_pretrained(
                path,
                torch_dtype=torch.float16,
                device_map="auto",
                trust_remote_code=True,
                load_in_8bit=os.environ.get("LOAD_IN_8BIT", "0") == "1",
            )

            self._model.eval()

            adapter_dir = self.get_adapter_dir()
            if adapter_dir.exists():
                console.print(
                    f":monkey: try to load adapter({adapter_dir}) into memory..."
                )
                self._model = PeftModel.from_pretrained(self._model, adapter_dir)

        return self._model

    def calculate_tokens_length(self, input_prompt: str) -> int:
        return len(self._get_tokenizer().encode(input_prompt))

    def do_predict(
        self,
        input_prompt: str,
        benchmark_type: BenchmarkType = BenchmarkType.MultipleChoice,
        max_new_tokens: int = 50,
    ) -> t.Any:
        # TODO: add self prompt wrapper

        if benchmark_type == BenchmarkType.MultipleChoice:
            out = self._do_predict_with_logits(
                input_prompt,
                choices=["A", "B", "C", "D"],
            )
        else:
            out = self._do_predict_with_generate(input_prompt, max_new_tokens)
        return out

    @torch.no_grad()
    def _do_predict_with_logits(self, input_prompt: str, choices: t.List) -> str:
        model = self._get_model()
        tokenizer = self._get_tokenizer()
        inputs = tokenizer(input_prompt, return_tensors="pt").to(model.device)

        choices_token_ids = [tokenizer.convert_tokens_to_ids(i) for i in choices]
        choices_idx_map = {i: j for i, j in enumerate(choices)}

        outputs = model(**inputs)
        last_logits = outputs.logits[:, -1, :]
        choice_logits = last_logits[:, choices_token_ids].detach().cpu().numpy()
        pred_prob_idx = numpy.argmax(choice_logits[0])
        return choices_idx_map[pred_prob_idx]

    @torch.no_grad()
    def _do_predict_with_generate(
        self,
        input_prompt: str,
        max_new_tokens: int = 50,
    ) -> str:
        model = self._get_model()
        tokenizer = self._get_tokenizer()
        inputs = tokenizer(input_prompt, return_tensors="pt").to(model.device)
        # TODO: support custom kwargs
        outputs = model.generate(
            **inputs,
            max_new_tokens=max_new_tokens,
            temperature=float(os.environ.get("TEMPERATURE", 0.7)),
            top_p=float(os.environ.get("TOP_P", 0.9)),
            top_k=float(os.environ.get("TOP_K", 0)),
            repetition_penalty=float(os.environ.get("REPETITION_PENALTY", 1.1)),
        )
        return tokenizer.decode(outputs[0], skip_special_tokens=True)


_SUPPORTED_LLM: t.Dict[str, t.Type[LLMBase]] = {}


def register():
    def _deco(_cls: t.Any) -> LLMBase:
        if not issubclass(_cls, LLMBase):
            raise TypeError(f"not subclass of LLMBase: {_cls}")

        name = _cls.get_name().lower()
        if name in _SUPPORTED_LLM:
            raise ValueError(f"already registered: {name}")

        _SUPPORTED_LLM[name] = _cls
        return _cls

    return _deco


def get_llm(name: str, **kwargs: t.Any) -> LLMBase:
    name = name.lower()
    if name not in _SUPPORTED_LLM:
        raise ValueError(f"not supported: {name}")

    return _SUPPORTED_LLM[name](**kwargs)


def get_built_llm(rootdir: Path | None = None, **kwargs: t.Any) -> LLMBase:
    rootdir = rootdir if rootdir is not None else Path.cwd()
    config_path = rootdir / "pretrained" / "sw_config.json"
    if not config_path.exists():
        raise RuntimeError(f"not found {config_path} to ingest the built model")

    name = json.loads(config_path.read_text())["name"]
    return get_llm(name, **kwargs)


def get_supported_llm() -> t.List[str]:
    return list(_SUPPORTED_LLM.keys())
