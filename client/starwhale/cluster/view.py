import typing as t

from rich import print as rprint
from rich.panel import Panel
from rich.table import Table
from rich.tree import Tree
from rich import box

from .model import ClusterModel, DEFAULT_PAGE_NUM, DEFAULT_PAGE_SIZE, ProjectObjType
from starwhale.base.view import BaseView
from starwhale.utils import pretty_bytes, console


# TODO: use model-view-control mode to refactor Cluster
class ClusterView(ClusterModel, BaseView):
    def run_job(
        self,
        model_id: int,
        dataset_ids: t.List[int],
        project: int,
        baseimage_id: int,
        device: str,
        name: str,
        desc: str,
    ) -> None:
        success, msg = self._request_create_job(
            project, model_id, dataset_ids, baseimage_id, device, name, desc
        )

        if success:
            rprint(f":clap: success to create job(project id: {project}) {msg}")
            rprint(
                f":writing_hand: run cmd [green]swcli eval cluster info {project} {msg}[/] to fetch job details"
            )
        else:
            rprint(f":collision: failed to create job, notice: [red]{msg}[/]")

    @BaseView._pager  # type: ignore
    @BaseView._header  # type: ignore
    def info_job(
        self,
        project: int,
        job: int,
        page: int = DEFAULT_PAGE_NUM,
        size: int = DEFAULT_PAGE_SIZE,
    ) -> t.Tuple[t.List[t.Any], t.Dict[str, t.Any]]:
        tasks, pager = self._fetch_tasks(project, job, page, size)
        report = self._fetch_job_report(project, job)

        def _print_tasks() -> None:
            table = Table(box=box.SIMPLE)
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

    def render_job_report(self, report: t.Dict[str, t.Any]) -> None:
        if not report:
            rprint(":turtle: no report")
            return

        labels: t.Dict[str, t.Any] = report.get("labels", {})
        sort_label_names = sorted(list(labels.keys()))

        def _print_report() -> None:
            # TODO: add other kind report
            def _r(_tree: t.Any, _obj: t.Any) -> None:
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
            console.print(self.comparsion(tree, table))

        def _print_confusion_matrix() -> None:
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
            console.print(self.comparsion(mtable, btable))

        _print_report()
        _print_confusion_matrix()

    def _pretty_status(self, status: str) -> t.Tuple[str, str, str]:
        style = "blue"
        icon = ":tractor:"
        if status == "SUCCESS":
            style = "green"
            icon = ":clap:"
        elif status == "FAIL":
            style = "red"
            icon = ":fearful:"
        return status, style, icon

    @BaseView._pager  # type: ignore
    @BaseView._header  # type: ignore
    def list_jobs(
        self, project: int, page: int = DEFAULT_PAGE_NUM, size: int = DEFAULT_PAGE_SIZE
    ) -> t.Tuple[t.List[t.Any], t.Dict[str, t.Any]]:
        jobs, pager = self._fetch_jobs(project, page, size)

        table = Table(
            title=f"Project({project}) Jobs List",
            box=box.SIMPLE,
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
