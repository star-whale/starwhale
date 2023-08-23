from __future__ import annotations

import typing as t
from abc import ABC, abstractmethod
from enum import Enum, unique

from starwhale.base.uri.resource import Resource

from .consts import subject_en2zh_map


@unique
class BenchmarkType(Enum):
    MultipleChoice = "multiple_choice"  # The answer is one of the choices[A,B,C,D]
    QuestionAnswering = "question_answering"  # The answer is a span of the context


class BenchmarkBase(ABC):
    @classmethod
    @abstractmethod
    def get_name(cls) -> str:
        raise NotImplementedError

    @abstractmethod
    def generate_prompt(
        self,
        features: t.Dict,
        few_shot: int = 0,
        dataset_uri: Resource | None = None,
        max_length: int = 2048,
        len_tokens: t.Callable[[str], int] | None = None,
    ) -> str:
        raise NotImplementedError

    @abstractmethod
    def calculate_score(self, predict_result: str, input_features: t.Dict) -> t.Dict:
        raise NotImplementedError

    @classmethod
    @abstractmethod
    def get_type(cls) -> BenchmarkType:
        raise NotImplementedError

    def get_en2zh_subject(self, subject: str) -> str:
        return subject_en2zh_map.get(subject, subject)

    def make_input_features_display(self, features: t.Dict) -> t.Dict:
        return features


_SUPPORTED_BENCHMARKS: t.Dict[str, BenchmarkBase] = {}


def register():
    def _deco(_cls: t.Any) -> BenchmarkBase:
        if not issubclass(_cls, BenchmarkBase):
            raise TypeError(f"not subclass of BenchmarkBase: {_cls}")

        name = _cls.get_name().lower()
        if name in _SUPPORTED_BENCHMARKS:
            raise ValueError(f"already registered: {name}")

        _SUPPORTED_BENCHMARKS[name] = _cls
        return _cls

    return _deco


def get_benchmark(name: str) -> BenchmarkBase:
    name = name.lower()
    if name in _SUPPORTED_BENCHMARKS:
        return _SUPPORTED_BENCHMARKS[name]

    _candidates = []
    for _bmk, _cls in _SUPPORTED_BENCHMARKS.items():
        if _bmk in name:
            _candidates.append((_bmk, _cls))

    if len(_candidates) == 1:
        return _candidates[0][1]
    elif len(_candidates) > 1:
        raise ValueError(f"ambiguous benchmark name: {name}, candidates: {_candidates}")
    else:
        raise ValueError(f"not supported benchmark: {name}")
