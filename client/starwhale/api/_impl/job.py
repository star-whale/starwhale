import typing as t

from starwhale.core.job.model import Step, Parser


def step(
    job_name: str = "default",
    resources: str = "cpu=1",
    concurrency: int = 1,
    task_num: int = 1,
    dependency: str = "",
) -> t.Any:
    def decorator(func: t.Any) -> t.Any:
        if Parser.is_parse_stage():
            _step = Step(
                job_name,
                func.__qualname__,
                resources,
                concurrency,
                task_num,
                dependency,
            )
            Parser.add_job(job_name, _step)

        return func

    return decorator
