from __future__ import annotations

import re
import random
import typing as t

from starwhale.utils.debug import console
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

        # simplify samples with some fixed questions
        samples_features = [
            {
                "question": "病毒体核心的主要物质是",
                "a": "类脂",
                "b": "核酸",
                "c": "蛋白质",
                "d": "磷酸",
                "answer": "B",
            },
            {
                "question": "流行病学属于什么范畴",
                "a": "临床医学",
                "b": "生物医学",
                "c": "基础医学",
                "d": "预防医学",
                "answer": "D",
            },
            {
                "question": "下列选项中，属于处分行为的是",
                "a": "捐助行为",
                "b": "抛弃所有权的行为",
                "c": "签订货物买卖合同",
                "d": "委托行为",
                "answer": "B",
            },
            {
                "question": "对累犯从重处罚的刑罚制度，体现了我国刑法的",
                "a": "罪刑法定原则",
                "b": "惩罚与教育相结合原则",
                "c": "刑法适用平等原则",
                "d": "罪责刑相适应原则",
                "answer": "D",
            },
            {
                "question": "犯罪分子具有刑法规定的减轻处罚情节的，应当在（）判处刑罚。",
                "a": "法定刑幅度内按照最低刑",
                "b": "法定最高刑以下",
                "c": "法定刑以下",
                "d": "法定刑以内",
                "answer": "C",
            },
            {
                "question": "下列短语中，是定中短语的是",
                "a": "打扫干净",
                "b": "操作方法",
                "c": "张华同学",
                "d": "已经完成",
                "answer": "B",
            },
            {
                "question": "在下面重叠的例子中，表示“适度、适中”意义的是",
                "a": "白白的",
                "b": "坐坐",
                "c": "客客气气的",
                "d": "散散步",
                "answer": "A",
            },
            {
                "question": "“员、祖、乡、分、妊、严”中包含的自由语素是",
                "a": "乡、分、严",
                "b": "祖、分、严",
                "c": "祖、乡、分",
                "d": "员、分、妊",
                "answer": "A",
            },
            {
                "question": "必然王国和自由王国是社会发展的",
                "a": "两条不同的道路",
                "b": "两种不同的理想",
                "c": "两种不同的状态",
                "d": "两种不同的选择",
                "answer": "C",
            },
            {
                "question": "在垄断资本主义阶段占统治地位的资本是",
                "a": "工业资本",
                "b": "金融资本",
                "c": "农业资本",
                "d": "银行资本",
                "answer": "B",
            },
        ]

        random.shuffle(samples_features)
        samples = []
        total = 0
        idx = 0
        for _ in range(0, few_shot):
            features = samples_features[idx]
            idx = (idx + 1) % len(samples_features)
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

    def _ingest_choice(self, content: str) -> str:
        content = content.strip().upper()

        if not content:
            raise ValueError("cannot ingest choice from empty content")

        if content[0] in self._CHOICES:
            return content[0]

        patterns = [
            (r"(答案)?(选项)?(是|为)?(.*?)([ABCD])", 5),
            (r"选(项|择)?(.*?)([ABCD])", 3),
        ]

        for pattern, index in patterns:
            match = re.search(pattern, content)
            if match:
                return match.group(index)

        m = re.findall(r"[ABCD]", content)
        if len(m) >= 1:
            return m[0]

        raise ValueError(f"cannot ingest ABCD choice from {content}")

    def calculate_score(
        self, predict_result: t.Dict | str, input_features: t.Dict
    ) -> t.Dict:
        if isinstance(predict_result, str):
            try:
                choice = self._ingest_choice(predict_result)
            except ValueError:
                console.error(f"cannot ingest choice from {predict_result}")
                choice = "N/A"
            explanation = predict_result
        elif isinstance(predict_result, dict):
            explanation = predict_result["content"]
            choice = predict_result["choice"]
        else:
            raise TypeError(f"invalid predict_result type: {type(predict_result)}")

        score = 1 if input_features["answer"].upper() == choice else 0
        return {
            "explanation": explanation,
            "choice": choice,
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
