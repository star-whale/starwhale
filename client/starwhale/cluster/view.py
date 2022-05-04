
import typing as t
from functools import wraps

from loguru import logger
from rich.layout import Layout
from rich.console import Console, RenderableType
from rich import print as rprint
from rich.panel import Panel
from rich.table import Table
from rich.tree import Tree
from rich import box

from .model import ClusterModel, DEFAULT_PAGE_NUM, DEFAULT_PAGE_SIZE, PROJECT_OBJ_TYPE
from starwhale.utils import pretty_bytes


#TODO: use model-view-control mode to refactor Cluster
class ClusterView(ClusterModel):

    def _pager(func): #type: ignore
        @wraps(func) #type: ignore
        def _wrapper(*args, **kwargs):
            self: ClusterView = args[0]
            def _print(_r):
                p = Panel(
                    f"Counts: [green] {_r['current']}/{_r['total']} [/] :sheep: , [red] {_r['remain']} [/] items does not show.",
                    title="Count Details",
                    title_align="left",
                )
                rprint(p)

            rt = func(*args, **kwargs) #type: ignore
            if isinstance(rt, tuple) and len(rt) == 2 and "total" in rt[1]:
                _print(rt[1])
        return _wrapper

    def _header(func): #type: ignore
        @wraps(func) #type: ignore
        def _wrapper(*args, **kwargs):
            self: ClusterView = args[0]

            def _print():
                grid = Table.grid(expand=True)
                grid.add_column(justify="center", ratio=1)
                grid.add_column(justify="right")
                grid.add_row(
                   f":star: {self.sw_remote_addr} :whale:",
                   f":clown_face:{self.user_name}@{self.user_role}",
                )
                p = Panel(grid, title=f"Starwhale Controller Cluster", title_align="left")
                rprint(p)

            _print()
            return func(*args, **kwargs) #type: ignore
        return _wrapper

    @_pager #type: ignore
    @_header #type: ignore
    def list_jobs(self, project: int, page: int=DEFAULT_PAGE_NUM, size: int=DEFAULT_PAGE_SIZE):
        jobs, pager = self._fetch_jobs(project, page, size)

        table = Table(
            title=f"Project({project}) Jobs", box=box.SIMPLE,
            expand=True
        )
        table.add_column("ID", justify="left", style="cyan", no_wrap=True)
        table.add_column("Model", style="magenta")
        table.add_column("Version", style="magenta")
        table.add_column("State", style="magenta")
        table.add_column("Resource", style="blue")
        table.add_column("Duration")
        table.add_column("Created")
        table.add_column("Finished")

        for j in jobs:
            status = j["jobStatus"]
            style = ""
            icon = ":thinking:"
            if status == "SUCCESS":
                style = "green"
                icon = ":clap:"
            elif status == "FAIL":
                style = "red"
                icon = ":fearful:"

            table.add_row(
                j["id"],
                j["modelName"],
                j["short_model_version"],
                f"[{style}]{icon}{status}[/]",
                f"{j['device']}:{j['deviceAmount']}",
                j["duration_str"],
                j["created_at"],
                j["finished_at"],
            )

        rprint(table)
        return jobs, pager

    @_pager #type: ignore
    @_header #type: ignore
    def list_projects(self, all_users: bool=False, page: int=DEFAULT_PAGE_NUM, size: int=DEFAULT_PAGE_SIZE, fullname: bool=False):
        user_name = "" if all_users else self.user_name
        projects, pager = self._fetch_projects(user_name, page, size)

        def _show_objects(objects: t.List, typ: str) -> Tree:
            tree = Tree(f"[red]{typ}[/]")
            for _o in objects:
                otree = tree.add(f"{_o['name']}")
                for _v in _o["latest_versions"]:
                    _k = 'name' if fullname else 'short_name'
                    if typ == PROJECT_OBJ_TYPE.MODEL:
                        #TODO: add model version for every version
                        _size = _o['files'][0]['size']
                    else:
                        _size = pretty_bytes(_v['meta']['dataset_byte_size'])

                    otree.add(
                        f"[green]{_v[_k]}[/] :timer_clock: {_v['created_at']} :dizzy:{_size}"
                    )
            return tree

        def _details(pid: int):
            _r = self._inspect_project(pid)
            return self.comparsion(
                _show_objects(_r["models"], PROJECT_OBJ_TYPE.MODEL),
                _show_objects(_r["datasets"], PROJECT_OBJ_TYPE.DATASET)
            )

        if not projects:
            rprint("No projects.")
            return projects, pager

        grid = Table.grid(padding=1, pad_edge=True)
        grid.add_column("Project", no_wrap=True, justify="left", style="bold green", min_width=20)
        grid.add_column("")
        grid.add_column("Details")

        for p in projects:
            grid.add_row(
                f"{p['id']} {p['name']}",
                ":arrow_right::arrow_right:",
                f"default:{p['is_default']} :timer_clock:created:{p['created_at']} :clown_face:owner:{p['owner']} ",
            )
            grid.add_row(
                "",
                ":arrow_right::arrow_right:",
                _details(p["id"]),
            )

        p = Panel(grid, title=f"Project Details", title_align="left")
        rprint(p)
        return projects, pager

    @_header #type: ignore
    def info(self):
        #TODO: user async to get
        _baseimages = self._fetch_baseimage()
        _version = self._fetch_version()
        _agents = self._fetch_agents()

        def _agents_table():
            table = Table(
                show_edge=False,
                show_header=True,
                expand=True,
                row_styles=["none", "dim"],
                box=box.SIMPLE,
            )
            table.add_column("id")
            table.add_column("ip", style="green")
            table.add_column("status", style="blue")
            table.add_column("version")
            table.add_column("connected time")

            for i, _agent in enumerate(_agents):
                table.add_row(
                    str(i), _agent["ip"], str(_agent["status"]), _agent["version"], str(_agent["connectedTime"])
                )
            return table

        def _details() -> Panel:
            grid = Table.grid(padding=1, pad_edge=True)
            grid.add_column("Category", no_wrap=True, justify="left", style="bold green")
            grid.add_column("Information")

            grid.add_row(
                "Version",
                _version,
            )

            grid.add_row(
                "BaseImage",
                "\n".join([f"- {i}" for i in _baseimages])
            )
            grid.add_row(
                "Agents",
                _agents_table(),
            )
            return Panel(grid, title_align="left")

        rprint(_details())

    def comparsion(self, r1: RenderableType, r2: RenderableType) -> Table:
        table = Table(show_header=False, pad_edge=False, box=None, expand=False)
        table.add_column("1", ratio=1)
        table.add_column("2", ratio=1)
        table.add_row(r1, r2)
        return table