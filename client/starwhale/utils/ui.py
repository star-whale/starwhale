from rich.table import Table
from rich.console import RenderableType


def comparsion(r1: RenderableType, r2: RenderableType) -> Table:
    table = Table(show_header=False, pad_edge=False, box=None, expand=True)
    table.add_column("1", ratio=1)
    table.add_column("2", ratio=1)
    table.add_row(r1, r2)
    return table
