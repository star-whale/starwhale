from __future__ import annotations

import os
import typing as t
import threading
from collections import defaultdict

import numpy

from starwhale import evaluation
from starwhale.utils.debug import console

try:
    from .llm import get_built_llm
    from .benchmark import BenchmarkBase, get_benchmark
    from .benchmark.consts import categories as first_level_categories
    from .benchmark.consts import level1_to_level2, level1_to_level3
except ImportError:
    from llm import get_built_llm
    from benchmark import BenchmarkBase, get_benchmark
    from benchmark.consts import categories as first_level_categories
    from benchmark.consts import level1_to_level2, level1_to_level3


few_shot_choices = {
    0: "zero_shot",
    1: "one_shot",
    5: "five_shot",
}
_g_llm = None
_g_benchmarks: t.Dict[str, BenchmarkBase] = {}

max_prompt_length = int(os.environ.get("MAX_PROMPT_LENGTH", 2048))
max_new_tokens = int(os.environ.get("MAX_NEW_TOKENS", 32))


# TODO: support multi-gpus evaluation
# TODO: enhance selected features
@evaluation.predict(
    resources={"nvidia.com/gpu": 1},
    replicas=1,
    auto_log=False,
)
def predict_question(data: dict, external: dict) -> None:
    # dev split is used for few shot samples
    if data.get("_hf_split", "") == "dev":
        return

    # TODO: record cpu/gpu/memory info per predict pod
    global _g_llm
    with threading.Lock():
        if _g_llm is None:
            _g_llm = get_built_llm()

    global _g_benchmarks
    dataset_uri = external["dataset_uri"]
    dataset_name = dataset_uri.name
    if dataset_name not in _g_benchmarks:
        # TODO: use dataset_info to get benchmark
        _g_benchmarks[dataset_name] = get_benchmark(dataset_name)

    result = {}
    benchmark = _g_benchmarks[dataset_name]()
    for shot, show_name in few_shot_choices.items():
        prompt = benchmark.generate_prompt(
            data,
            few_shot=shot,
            dataset_uri=dataset_uri,
            max_length=max_prompt_length,
            len_tokens=_g_llm.calculate_tokens_length,
        )
        predict_result = _g_llm.do_predict(
            prompt, benchmark_type=benchmark.get_type(), max_new_tokens=max_new_tokens
        )
        result[show_name] = benchmark.calculate_score(predict_result, data)
        console.trace(f"prompt: {prompt}")
        console.trace(f"answer: {data['answer']}, predict: {result[show_name]}")

    evaluation.log(
        category="results",
        id=f"{benchmark.get_name()}-{external['index']}",
        metrics={
            "input": benchmark.make_input_features_display(data),
            "output": result,
        },
    )


@evaluation.evaluate(needs=[predict_question], use_predict_auto_log=False)
def evaluation_results() -> None:
    category_scores = defaultdict(lambda: defaultdict(list))
    """
    input data examples:
      "input/category/first-level": ["STEM"],
      "input/category/second-level": ["History"],
      "input/category/third-level": ["History of the United States"],
      "output/zero_shot/score": 0.0,
      "output/one_shot/score": 0.0,
      "output/five_shot/score": 0.0,
    """
    benchmark_name = ""
    count = 0
    for data in evaluation.iter("results"):
        count += 1
        if not benchmark_name:
            benchmark_name = data["id"].split("-")[0]

        _categories = []
        for k in data:
            if k.startswith("input/category/"):
                _categories.extend(data[k])

        scores = {}
        for shot in few_shot_choices.values():
            key = f"output/{shot}/score"
            scores[shot] = data[key]

        for category in _categories:
            for shot, score in scores.items():
                category_scores[category][shot].append(score)

    category_weight_scores = defaultdict(dict)
    for category, scores in category_scores.items():
        for shot, score_list in scores.items():
            weight = len(score_list)
            score = numpy.mean(score_list) * 100
            category_weight_scores[category][shot] = (score, weight)

    """
    {
        "category": {
            "zero_shot": (score, weight),
            "one_shot": (score, weight),
            "five_shot": (score, weight),
        }
    }
    """
    llm = get_built_llm()
    benchmark = get_benchmark(benchmark_name)

    shot_weight_scores = defaultdict(lambda: defaultdict(list))
    all_scores = []
    all_weights = []

    for category, scores in category_weight_scores.items():
        for shot, (score, weight) in scores.items():
            shot_weight_scores[shot]["score"].append(score)
            shot_weight_scores[shot]["weight"].append(weight)
            all_scores.append(score)
            all_weights.append(weight)

    score_f = lambda s: numpy.average(
        shot_weight_scores[s]["score"], weights=shot_weight_scores[s]["weight"]
    )

    summary = {
        "llm": llm.get_name(),
        "benchmark/name": benchmark.get_name(),
        "benchmark/type": benchmark.get_type().value,
        "benchmark/count": count,
        "accuracy": numpy.average(all_scores, weights=all_weights),
        "accuracy/zero_shot": score_f("zero_shot"),
        "accuracy/one_shot": score_f("one_shot"),
        "accuracy/five_shot": score_f("five_shot"),
    }

    for level1 in first_level_categories:
        if level1 not in category_weight_scores:
            continue

        _scores = []
        _weights = []
        _metrics = {}
        for shot, (score, weight) in category_weight_scores[level1].items():
            summary[f"category/{level1}/count/{shot}"] = weight
            summary[f"category/{level1}/accuracy/{shot}"] = score
            _scores.append(score)
            _weights.append(weight)
            _metrics[shot] = score

        summary[f"category/{level1}/accuracy"] = numpy.average(
            _scores, weights=_weights
        )
        summary[f"category/{level1}/count"] = sum(_weights)
        evaluation.log(category="first-level-summary", id=level1, metrics=_metrics)

        _ingest_f = lambda x: {
            k: v[0] for k, v in category_weight_scores.get(x, {}).items()
        }

        for level2 in level1_to_level2[level1]:
            evaluation.log(
                category=f"{level1}/category/summary",
                id=level2,
                metrics=_ingest_f(level2),
            )

        for level3 in level1_to_level3[level1]:
            evaluation.log(
                category=f"{level1}/subcategory/summary",
                id=level3,
                metrics=_ingest_f(level3),
            )

    evaluation.log_summary(summary)
