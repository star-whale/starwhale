import typing as t

from rich.progress import Progress, SpinnerColumn, TimeElapsedColumn
from rich.console import Console


def run_with_progress_bar(
    title: str, operations: t.List[t.Tuple[t.Callable, int, str]], console: Console
):
    with Progress(
        SpinnerColumn(),
        *Progress.get_default_columns(),
        TimeElapsedColumn(),
        console=console,
        refresh_per_second=1,
    ) as progress:
        task = progress.add_task(f"[red]{title}", total=sum([o[1] for o in operations]))

        for idx, op in enumerate(operations):
            progress.update(task, description=f"[red]{op[2]}...")
            op[0]()
            progress.update(
                task,
                advance=op[1],
                description=f"[green]{idx+1} out of {len(operations)} steps finished",
            )
