from __future__ import annotations

import typing as t

from starwhale import dataset
from starwhale.base.uri.resource import Resource

from .base import register, BenchmarkBase, BenchmarkType
from .consts import level3_to_level1, level3_to_level2


# TODO: abstract class for multiple choice
@register()
class Cmmlu(BenchmarkBase):
    """cmmlu benchmark dataset.
    ref: https://huggingface.co/datasets/haonan-li/cmmlu
    example:
        {
            "question": "What is the capital of China?",
            "a": "北京",
            "b": "上海",
            "c": "广州",
            "d": "深圳",
            "answer": "A"
            "_hf_subset": "agronomy",
            "_hf_split": "test",
        }
    """

    _CHOICES = ("A", "B", "C", "D")

    @classmethod
    def get_name(cls) -> str:
        return "cmmlu"

    @classmethod
    def get_type(cls) -> BenchmarkType:
        return BenchmarkType.MultipleChoice

    def generate_prompt(
        self,
        features: t.Dict,
        few_shot: int = 0,
        dataset_uri: Resource | None = None,
        max_length: int = 2048,
        len_tokens: t.Callable[[str], int] | None = None,
    ) -> str:
        if few_shot < 0 or (few_shot > 0 and dataset_uri is None):
            raise ValueError(
                "few_shot must be >= 0 and dataset_uri must be provided if few_shot > 0"
            )

        subject = features["_hf_subset"]
        show_subject = self.get_en2zh_subject(subject)

        system_prompt = f"以下是关于{show_subject}的单项选择题，请直接给出正确答案的选项。 \n\n"
        question = self.generate_question(features, include_answer=False)

        if len_tokens is None:
            len_tokens = len

        remaining_length = max_length - len_tokens(system_prompt) - len_tokens(question)
        samples_prompt = self.generate_samples_prompt(
            few_shot, subject, dataset_uri, remaining_length, len_tokens  # type: ignore
        )

        return system_prompt + samples_prompt + question

    def generate_samples_prompt(
        self,
        few_shot: int,
        subject: str,
        dataset_uri: Resource,
        max_length: int,
        len_tokens: t.Callable[[str], int],
    ) -> str:
        if few_shot <= 0:
            return ""

        ds = dataset(dataset_uri)
        samples = []
        total = 0
        for i in range(0, few_shot):
            features = ds[f"{subject}/dev/{i}"].features
            question = self.generate_question(features, include_answer=True)
            total += len_tokens(question)
            if total > max_length:
                break
            samples.append(question)

        return "\n".join(samples) + "\n\n"

    def generate_question(self, features: t.Dict, include_answer: bool = False) -> str:
        question = "题目：" + features["question"]
        for choice in self._CHOICES:
            question += f"\n{choice}. {features[choice.lower()]}"
        question += "\n答案是："

        if include_answer:
            question += f"{features['answer']} \n\n"
        return question

    def calculate_score(self, predict_result: str, input_features: t.Dict) -> t.Dict:
        # TODO: support ingest A,B,C,D from the complex string, for example Chatgpt Answer
        predict_result = predict_result.strip().upper()
        score = 1 if input_features["answer"].upper() == predict_result else 0
        return {
            "predict": predict_result,
            "score": score,
        }

    def make_input_features_display(self, features: t.Dict) -> t.Dict:
        choices = ", ".join([f"{c}. {features[c.lower()]}" for c in self._CHOICES])
        subset = features["_hf_subset"]
        return {
            "question": features["question"],
            "choices": choices,
            "answer": features["answer"],
            "category": {
                "first-level": level3_to_level1.get(subset) or [subset],
                "second-level": level3_to_level2.get(subset) or [subset],
                "third-level": [subset],
            },
        }
