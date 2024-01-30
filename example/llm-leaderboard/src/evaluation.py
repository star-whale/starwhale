from __future__ import annotations

import typing as t
import threading
import dataclasses
from collections import defaultdict

import numpy

from starwhale import argument, evaluation
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


@dataclasses.dataclass
class ModelGenerateArguments:
    max_prompt_length: int = dataclasses.field(
        default=2048, metadata={"help": "max length of prompt"}
    )
    max_new_tokens: int = dataclasses.field(
        default=256, metadata={"help": "max length of generated text"}
    )
    batch: int = dataclasses.field(
        default=1, metadata={"help": "batch size for inference"}
    )
    temperature: float = dataclasses.field(
        default=0.8, metadata={"help": "temperature"}
    )
    top_p: float = dataclasses.field(default=0.95, metadata={"help": "top p"})
    tensor_parallel: int = dataclasses.field(
        default=1, metadata={"help": "tensor parallel for vllm"}
    )
    max_model_len: int = dataclasses.field(
        default=16384, metadata={"help": "max model len for vllm kv cache"}
    )


# TODO: support multi-gpus evaluation
# TODO: enhance selected features
@argument(ModelGenerateArguments)
@evaluation.predict(
    resources={"nvidia.com/gpu": 1},
    replicas=1,
    batch_size=32,
    auto_log=False,
)
def predict_question(
    data: t.List[dict], external: dict, argument: ModelGenerateArguments
) -> None:
    # TODO: record cpu/gpu/memory info per predict pod
    global _g_llm

    with threading.Lock():
        if _g_llm is None:
            _g_llm = get_built_llm(
                tensor_parallel=argument.tensor_parallel,
                max_model_len=argument.max_model_len,
            )

    global _g_benchmarks
    dataset_uri = external["dataset_uri"]
    dataset_name = dataset_uri.name
    if dataset_name not in _g_benchmarks:
        # TODO: use dataset_info to get benchmark
        _g_benchmarks[dataset_name] = get_benchmark(dataset_name)

    benchmark = _g_benchmarks[dataset_name]()

    inputs = []
    for _index, _data in zip(external["index"], data):
        # dev split is used for few shot samples
        if _data.get("_hf_split", "") == "dev":
            continue

        for _shot, _show_name in few_shot_choices.items():
            _prompt = benchmark.generate_prompt(
                _data,
                few_shot=_shot,
                dataset_uri=dataset_uri,
                max_length=argument.max_prompt_length,
                len_tokens=_g_llm.calculate_tokens_length,
            )
            inputs.append((_index, _show_name, _data, _prompt))

    predict_results = []
    for idx in range(0, len(inputs), argument.batch):
        batch_prompts = [x[-1] for x in inputs[idx : idx + argument.batch]]

        if _g_llm.support_batch_inference():
            _results = _g_llm.do_predict(
                batch_prompts,
                benchmark_type=benchmark.get_type(),
                max_new_tokens=argument.max_new_tokens,
                predict_choice_by_logits=True,
            )
            predict_results.extend(_results)
        else:
            for _prompt in batch_prompts:
                _result = _g_llm.do_predict(
                    _prompt,
                    benchmark_type=benchmark.get_type(),
                    max_new_tokens=argument.max_new_tokens,
                    predict_choice_by_logits=True,
                )
                predict_results.append(_result)

    for (_index, _show_name, _data, _prompt), predict_result in zip(
        inputs, predict_results
    ):
        score = benchmark.calculate_score(predict_result, _data)
        console.trace(f"prompt:\n {_prompt}")
        console.trace(f"answer: {_data['answer']}, predict: {score}")

        evaluation.log(
            category="results",
            id=f"{benchmark.get_name()}-{_index}",
            metrics={
                "input": benchmark.make_input_features_display(_data),
                "output": {_show_name: score},
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

    for _, scores in category_weight_scores.items():
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
