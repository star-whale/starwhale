from __future__ import annotations

from collections import defaultdict

from starwhale import Job, handler, evaluation
from starwhale.utils.debug import console

PROJECT_URI = "https://cloud.starwhale.cn/project/349"
JOB_URI_TEMPLATE = "%s/job/{job_id}" % PROJECT_URI
JOB_IDS = [
    "845",
    "844",
    "843",
    "842",
    "830",
    "828",
    "827",
    "818",
    "817",
    "816",
    "814",
    "813",
    "810",
    "796",
    "759",
]
SHOT_GROUPS = ("zero_shot", "one_shot", "five_shot")
CATEGORY_GROUPS = ("first-level", "second-level", "third-level")


@handler(replicas=1)
def analysis_leaderboard() -> None:
    r_score = defaultdict(lambda: defaultdict(lambda: defaultdict(int)))
    r_score_models = defaultdict(lambda: defaultdict(lambda: defaultdict(list)))
    r_benchmark = defaultdict(dict)
    r_jobs = {}
    job_cnt = len(JOB_IDS)

    for idx, job_id in enumerate(JOB_IDS):
        uri = JOB_URI_TEMPLATE.format(job_id=job_id)
        job = Job.get(uri)

        console.print(f"{idx+1}/{job_cnt} processing [{job_id}] {job.model} ...")
        if job.model:
            r_jobs[job_id] = job.model.name

        for row in job.get_table_rows("results"):
            benchmark_id = row["id"]

            if benchmark_id not in r_benchmark:
                r_benchmark[benchmark_id] = {
                    "question": row["input/question"],
                    "answer": row["input/answer"],
                    "choices": row["input/choices"],
                }
                for category in CATEGORY_GROUPS:
                    r_benchmark[benchmark_id][f"category/{category}"] = row[
                        f"input/category/{category}"
                    ]

            for shot in SHOT_GROUPS:
                score = row[f"output/{shot}/score"]
                score = f"score-{score}"

                if score == "score-1":
                    r_score[benchmark_id][shot]["right_count"] += 1

                r_score[benchmark_id][shot][score] += 1
                if job.model:
                    r_score_models[benchmark_id][shot][score].append(job.model.name)

    r_right_distribution = defaultdict(lambda: defaultdict(int))

    for benchmark_id, scores in r_score.items():
        evaluation.log(
            category="leaderboard-analysis",
            id=benchmark_id,
            metrics={
                "benchmark": r_benchmark[benchmark_id],
                "scores": scores,
                "models": r_score_models[benchmark_id],
            },
        )

        for shot, score_values in scores.items():
            score_one_cnt = score_values.get("right_count") or 0
            r_right_distribution[shot][score_one_cnt] += 1

    for shot, score_values in r_right_distribution.items():
        for count_name, count_value in score_values.items():
            metrics = {
                f"{shot}/count": count_value,
                f"{shot}/percentage": f"{count_value/len(r_benchmark):.2%}",
            }
            evaluation.log(
                category="right-answer-distribution",
                id=count_name,
                metrics=metrics,
            )
            console.log(f"{count_name} - {shot}: {metrics}")

    evaluation.log_summary(
        {
            "project": PROJECT_URI,
            "benchmark/name": "cmmlu",
            "benchmark/questions_count": len(r_benchmark),
            "analysis/job_models": list(r_jobs.values()),
            "analysis/job_ids": JOB_IDS,
        }
    )

    console.print(":clap: finished!")
