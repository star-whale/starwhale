import os
import typing as t

from rich.progress import Progress, SpinnerColumn, TimeElapsedColumn

from starwhale.utils import console
from starwhale.consts import ENV_DISABLE_PROGRESS_BAR


def run_with_progress_bar(
    title: str,
    operations: t.Sequence[t.Tuple[t.Any, ...]],
    **kw: t.Any,
) -> None:
    if os.environ.get(ENV_DISABLE_PROGRESS_BAR) == "1":
        for op in operations:
            if len(op) == 4:
                op[0](**op[3])
            else:
                op[0]()
    else:
        with Progress(
            SpinnerColumn(),
            *Progress.get_default_columns(),
            TimeElapsedColumn(),
            refresh_per_second=1,
            console=console.rich_console,
        ) as progress:
            task = progress.add_task(
                f"[red]{title}", total=sum([o[1] for o in operations])
            )

            for idx, op in enumerate(operations):
                progress.update(task, description=f"[red]{op[2]}...")
                if len(op) == 4:
                    op[0](**op[3])
                else:
                    op[0]()
                progress.update(
                    task,
                    advance=op[1],
                    description=f"[green]{idx+1} out of {len(operations)} steps finished",
                )
