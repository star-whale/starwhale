import typing as t
from functools import wraps

from rich import print as rprint
from rich.panel import Panel
from rich.table import Table
from rich.tree import Tree
from rich import box

from .model import ClusterModel, DEFAULT_PAGE_NUM, DEFAULT_PAGE_SIZE, ProjectObjType
from starwhale.utils import pretty_bytes, console
from starwhale.utils.ui import comparsion


# TODO: use model-view-control mode to refactor Cluster
class ClusterView(ClusterModel):
    def _pager(func):  # type: ignore
        @wraps(func)  # type: ignore
        def _wrapper(*args, **kwargs):
            def _print(_r):
                p = Panel(
                    (
                        f"Counts: [green] {_r['current']}/{_r['total']} [/] :sheep: ,"
                        f"[red] {_r['remain']} [/] items does not show."
                    ),
                    title="Count Details",
                    title_align="left",
                )
                rprint("\n", p)

            rt = func(*args, **kwargs)  # type: ignore
            if isinstance(rt, tuple) and len(rt) == 2 and "total" in rt[1]:
                _print(rt[1])

        return _wrapper

    def _header(func):  # type: ignore
        @wraps(func)  # type: ignore
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
                p = Panel(
                    grid, title="Starwhale Controller Cluster", title_align="left"
                )
                rprint(p, "\n")

            _print()
            return func(*args, **kwargs)  # type: ignore

        return _wrapper

    def run_job(
        self,
        model: int,
        datasets: t.List[int],
        project: int,
        baseimage: int,
        resource: str,
        name: str,
        desc: str,
    ):
        pass

    @_pager  # type: ignore
    @_header  # type: ignore
    def info_job(
        self,
        project: int,
        job: int,
        page: int = DEFAULT_PAGE_NUM,
        size: int = DEFAULT_PAGE_SIZE,
    ):
        tasks, pager = self._fetch_tasks(project, job, page, size)
        report = self._fetch_job_report(project, job)

        def _print_tasks():
            table = Table(box=box.SIMPLE, expand=True)
            table.add_column("ID", justify="left", style="cyan", no_wrap=True)
            table.add_column("UUID")
            table.add_column("Status", style="magenta")
            table.add_column("Agent")
            table.add_column("Duration")
            table.add_column("Created")
            table.add_column("Finished")

            for _t in tasks:
                status, style, icon = self._pretty_status(_t["taskStatus"])
                table.add_row(
                    _t["id"],
                    _t["uuid"],
                    f"[{style}]{icon}{status}[/]",
                    _t["agent"]["ip"],
                    "",
                    _t["created_at"],
                    "",
                )

            console.rule(f"[bold green]Project({project} Job({job}) Tasks List")
            console.print(table)

        _print_tasks()
        self.render_job_report(report)

        return tasks, pager

    def render_job_report(self, report: dict) -> None:
        labels: dict = report.get("labels", {})
        sort_label_names = sorted(list(labels.keys()))

        def _print_report():
            # TODO: add other kind report
            def _r(_tree, _obj):
                if not isinstance(_obj, dict):
                    _tree.add(str(_obj))

                for _k, _v in _obj.items():
                    if isinstance(_v, (list, tuple)):
                        _k = f"{_k}: [green]{'|'.join(_v)}"
                    elif isinstance(_v, dict):
                        _k = _k
                    else:
                        _k = f"{_k}: [green]{_v:.4f}"

                    _ntree = _tree.add(_k)
                    if isinstance(_v, dict):
                        _r(_ntree, _v)

            tree = Tree("Summary")
            _r(tree, report["summary"])
            if len(labels) == 0:
                return

            table = Table(box=box.SIMPLE)
            table.add_column("Label", style="cyan")
            keys = labels[sort_label_names[0]]
            for _k in keys:
                table.add_column(_k.capitalize())

            for _k, _v in labels.items():
                table.add_row(_k, *(f"{_v[_k2]:.4f}" for _k2 in keys))

            console.rule(f"[bold green]{report['kind'].upper()} Report")
            console.print(comparsion(tree, table))

        def _print_confusion_matrix():
            cm = report.get("confusion_matrix", {})
            if not cm:
                return

            btable = Table(box=box.SIMPLE)
            btable.add_column("", style="cyan")
            for n in sort_label_names:
                btable.add_column(n)
            for idx, bl in enumerate(cm.get("binarylabel", [])):
                btable.add_row(sort_label_names[idx], *[f"{_:.4f}" for _ in bl])

            mtable = Table(box=box.SIMPLE)
            mtable.add_column("Label", style="cyan")
            for n in ("TP", "TN", "FP", "FN"):
                mtable.add_column(n)
            for idx, ml in enumerate(cm.get("multilabel", [])):
                mtable.add_row(sort_label_names[idx], *[str(_) for _ in ml[0] + ml[1]])

            console.rule(f"[bold green]{report['kind'].upper()} Confusion Matrix")
            console.print(comparsion(mtable, btable))

        _print_report()
        _print_confusion_matrix()

    def _pretty_status(self, status: str) -> t.Tuple[str, str, str]:
        style = ""
        icon = ":thinking:"
        if status == "SUCCESS":
            style = "green"
            icon = ":clap:"
        elif status == "FAIL":
            style = "red"
            icon = ":fearful:"
        return status, style, icon

    @_pager  # type: ignore
    @_header  # type: ignore
    def list_jobs(
        self, project: int, page: int = DEFAULT_PAGE_NUM, size: int = DEFAULT_PAGE_SIZE
    ):
        jobs, pager = self._fetch_jobs(project, page, size)

        table = Table(
            title=f"Project({project}) Jobs List", box=box.SIMPLE, expand=True
        )
        table.add_column("ID", justify="left", style="cyan", no_wrap=True)
        table.add_column("Model", style="magenta")
        table.add_column("Version", style="magenta")
        table.add_column("Status", style="magenta")
        table.add_column("Resource", style="blue")
        table.add_column("Duration")
        table.add_column("Created")
        table.add_column("Finished")

        for j in jobs:
            status, style, icon = self._pretty_status(j["jobStatus"])
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

    @_pager  # type: ignore
    @_header  # type: ignore
    def list_projects(
        self,
        all_users: bool = False,
        page: int = DEFAULT_PAGE_NUM,
        size: int = DEFAULT_PAGE_SIZE,
        fullname: bool = False,
    ):
        user_name = "" if all_users else self.user_name
        projects, pager = self._fetch_projects(user_name, page, size)

        def _show_objects(objects: t.List, typ: str) -> Tree:
            tree = Tree(f"[red]{typ}[/]")
            for _o in objects:
                otree = tree.add(f"{_o['name']}")
                for _v in _o["latest_versions"]:
                    _k = "name" if fullname else "short_name"
                    if typ == ProjectObjType.MODEL:
                        # TODO: add model version for every version
                        _size = _o["files"][0]["size"]
                    else:
                        _size = pretty_bytes(_v["meta"]["dataset_byte_size"])

                    otree.add(
                        f"[green]{_v[_k]}[/] :timer_clock: {_v['created_at']} :dizzy:{_size}"
                    )
            return tree

        def _details(pid: int):
            _r = self._inspect_project(pid)
            return comparsion(
                _show_objects(_r["models"], ProjectObjType.MODEL),
                _show_objects(_r["datasets"], ProjectObjType.DATASET),
            )

        if not projects:
            rprint("No projects.")
            return projects, pager

        grid = Table.grid(padding=1, pad_edge=True)
        grid.add_column(
            "Project", no_wrap=True, justify="left", style="bold green", min_width=20
        )
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

        p = Panel(grid, title="Project Details", title_align="left")
        rprint(p)
        return projects, pager

    @_header  # type: ignore
    def info(self):
        # TODO: user async to get
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
                    str(i),
                    _agent["ip"],
                    str(_agent["status"]),
                    _agent["version"],
                    str(_agent["connectedTime"]),
                )
            return table

        def _details() -> Panel:
            grid = Table.grid(padding=1, pad_edge=True)
            grid.add_column(
                "Category", no_wrap=True, justify="left", style="bold green"
            )
            grid.add_column("Information")

            grid.add_row(
                "Version",
                _version,
            )

            grid.add_row("BaseImage", "\n".join([f"- {i}" for i in _baseimages]))
            grid.add_row(
                "Agents",
                _agents_table(),
            )
            return Panel(grid, title_align="left")

        rprint(_details())
