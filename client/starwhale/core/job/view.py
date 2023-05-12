import sys
import typing as t

from rich import box
from rich.panel import Panel
from rich.table import Table
from rich.pretty import Pretty
from rich.columns import Columns

from starwhale.utils import Order, console, sort_obj_list
from starwhale.consts import (
    CREATED_AT_KEY,
    DEFAULT_PAGE_IDX,
    DEFAULT_PAGE_SIZE,
    SHORT_VERSION_CNT,
    DEFAULT_REPORT_COLS,
    DEFAULT_MANIFEST_NAME,
)
from starwhale.base.type import JobOperationType
from starwhale.base.view import BaseTermView
from starwhale.api._impl.metric import MetricKind
from starwhale.base.uri.project import Project
from starwhale.base.uri.instance import Instance
from starwhale.base.uri.resource import Resource, ResourceType

from .model import Job


class JobTermView(BaseTermView):
    def __init__(self, job_uri: str) -> None:
        super().__init__()
        self.raw_uri = job_uri
        self.uri = Resource(job_uri, typ=ResourceType.job)
        self.job = Job.get_job(self.uri)
        self._action_run_map = {
            JobOperationType.CANCEL: self.job.cancel,
            JobOperationType.RESUME: self.job.resume,
            JobOperationType.REMOVE: self.job.remove,
            JobOperationType.RECOVER: self.job.recover,
            JobOperationType.PAUSE: self.job.pause,
        }

    def recover(self, force: bool = False) -> None:
        self._do_action(JobOperationType.RECOVER, force)

    def remove(self, force: bool = False) -> None:
        self._do_action(JobOperationType.REMOVE, force)

    def cancel(self, force: bool = False) -> None:
        self._do_action(JobOperationType.CANCEL, force)

    def resume(self, force: bool = False) -> None:
        self._do_action(JobOperationType.RESUME, force)

    def pause(self, force: bool = False) -> None:
        self._do_action(JobOperationType.PAUSE, force)

    @BaseTermView._simple_action_print
    def _do_action(self, action: str, force: bool = False) -> t.Tuple[bool, str]:
        return self._action_run_map[action](force)

    @BaseTermView._header
    def info(
        self,
        page: int = DEFAULT_PAGE_IDX,
        size: int = DEFAULT_PAGE_SIZE,
        max_report_cols: int = DEFAULT_REPORT_COLS,
        web: bool = False,
    ) -> None:
        _rt = self.job.info(page, size)
        if not _rt:
            console.print(":tea: not found info")
            return

        manifest = _rt.get("manifest", {})
        if web:
            from starwhale.web.server import Server

            ver = manifest.get("id")

            # if id is numeric, it's a remote eval
            if ver and ver.isnumeric():
                svr = Server.proxy(Instance())
            else:
                # local eval
                if not manifest.get("version"):
                    console.print(":tea: eval id not found")
                    sys.exit(1)
                ver = manifest.get("version")
                svr = Server.default()

            # TODO support changing host and port
            host = "127.0.0.1"
            port = 8000
            url = f"http://{host}:{port}/projects/{self.uri.project.name}/evaluations/{ver}/results?token=local"
            console.print(f":tea: open {url} in browser")
            import uvicorn

            uvicorn.run(svr, host=host, port=port, log_level="error")
            return

        if manifest:
            console.rule(
                f"[green bold]Inspect {DEFAULT_MANIFEST_NAME} for eval:{self.uri}"
            )
            console.print(Pretty(_rt["manifest"], expand_all=True))

        if "location" in _rt:
            console.rule("Process dirs")
            console.print(f":cactus: predict: {_rt['location']['predict']}")
            console.print(f":camel: evaluate: {_rt['location']['evaluate']}")

        if "tasks" in _rt:
            self._print_tasks(_rt["tasks"][0])

        if "report" in _rt:
            _report = _rt["report"]
            _kind = _rt["report"].get("kind", "")

            if "summary" in _report:
                self._render_summary_report(_report["summary"], _kind)

            if _kind == MetricKind.MultiClassification.value:
                self._render_multi_classification_job_report(
                    _rt["report"], max_report_cols
                )

    def _render_summary_report(self, summary: t.Dict[str, t.Any], kind: str) -> None:
        console.rule(f"[bold green]{kind.upper()} Summary")
        contents = [
            Panel(f"[b]{k}[/b]\n[yellow]{v}", expand=True) for k, v in summary.items()
        ]
        console.print(Columns(contents))

    def _print_tasks(self, tasks: t.List[t.Dict[str, t.Any]]) -> None:
        table = Table(box=box.SIMPLE)
        table.add_column("ID", justify="left", style="cyan", no_wrap=True)
        table.add_column("UUID")
        table.add_column("Status", style="magenta")
        table.add_column("Duration")
        table.add_column("Created")
        table.add_column("Finished")

        for _t in tasks:
            status, style, icon = self.pretty_status(_t["taskStatus"])
            table.add_row(
                _t["id"],
                _t["uuid"],
                f"[{style}]{icon}{status}[/]",
                "",
                _t[CREATED_AT_KEY],
                "",
            )

        console.rule(
            f"[bold green]Project({self.uri.project} Job({self.job.name}) Tasks List"
        )
        console.print(table)

    def _render_multi_classification_job_report(
        self, report: t.Dict[str, t.Any], max_report_cols: int
    ) -> None:
        if not report:
            console.print(":turtle: no report")
            return

        labels: t.Dict[str, t.Any] = report.get("labels", {})
        sort_label_names = sorted(list(labels.keys()))

        def _print_labels() -> None:
            if len(labels) == 0:
                return

            table = Table(box=box.SIMPLE)
            table.add_column("Label", style="cyan")
            keys = labels[sort_label_names[0]]
            for idx, _k in enumerate(keys):
                if _k == "id":
                    continue
                # add 1: because "id" is first col and should be removed
                if idx < max_report_cols + 1:
                    table.add_column(_k.capitalize())
                else:
                    table.add_column("...")
                    break

            for _k, _v in labels.items():
                table.add_row(
                    _k,
                    *(
                        f"{float(_v[_k2]):.4f}"
                        for idx, _k2 in enumerate(keys)
                        if _k2 != "id" and idx < max_report_cols + 1
                    ),
                )

            console.rule(f"[bold green]{report['kind'].upper()} Label Metrics Report")
            console.print(table)

        def _print_confusion_matrix() -> None:
            cm = report.get("confusion_matrix", {})
            if not cm:
                return

            btable = Table(box=box.ROUNDED)
            btable.add_column("", style="cyan")
            for idx, n in enumerate(sort_label_names):
                if idx < max_report_cols:
                    btable.add_column(n)
                else:
                    btable.add_column("...")
                    break
            for idx, bl in enumerate(cm.get("binarylabel", [])):
                btable.add_row(
                    sort_label_names[idx],
                    *[
                        f"{float(bl[i]):.4f}"
                        for idx, i in enumerate(bl)
                        if i != "id" and idx < max_report_cols + 1
                    ],
                )
            console.rule(f"[bold green]{report['kind'].upper()} Confusion Matrix")
            console.print(btable)

        _print_labels()
        _print_confusion_matrix()

    @classmethod
    def list(
        cls,
        project_uri: str = "",
        fullname: bool = False,
        show_removed: bool = False,
        page: int = DEFAULT_PAGE_IDX,
        size: int = DEFAULT_PAGE_SIZE,
    ) -> t.Tuple[t.List[t.Any], t.Dict[str, t.Any]]:
        _uri = Project(project_uri)
        cls.must_have_project(_uri)
        jobs, pager = Job.list(_uri, page, size)
        jobs = sort_obj_list(jobs, [Order("manifest.created_at", True)])
        return (jobs, pager)


class JobTermViewRich(JobTermView):
    @classmethod
    @BaseTermView._pager
    def list(
        cls,
        project_uri: str = "",
        fullname: bool = False,
        show_removed: bool = False,
        page: int = DEFAULT_PAGE_IDX,
        size: int = DEFAULT_PAGE_SIZE,
    ) -> t.Tuple[t.List[t.Any], t.Dict[str, t.Any]]:
        _jobs, _pager = super().list(project_uri, fullname, show_removed, page, size)
        table = Table(title="Job List", box=box.SIMPLE, expand=True)
        table.add_column("Name", justify="left", style="cyan", no_wrap=True)
        table.add_column("Model", no_wrap=True)
        table.add_column("Datasets")
        table.add_column("Runtime")
        table.add_column("Status", style="red")
        table.add_column("Resource", style="blue")
        table.add_column("Created At", style="magenta")
        table.add_column("Finished At", style="magenta")

        def _s(x: str) -> str:
            _end = -1 if fullname else SHORT_VERSION_CNT
            if ":" in x:
                _n, _v = x.split(":")
                return f"{_n}:{_v[:_end]}"
            else:
                return x[:_end]

        for _job in _jobs:
            if show_removed ^ _job.get("is_removed", False):
                continue

            _m = _job["manifest"]
            _status, _style, _icon = cls.pretty_status(
                _m.get("jobStatus") or _m.get("status")
            )
            _datasets = "--"
            if _m.get("datasets"):
                _datasets = "\n".join([_s(d) for d in _m["datasets"]])

            _model = "--"
            if "model" in _m:
                _model = _s(_m["model"])
            elif "modelName" in _m:
                _model = f"{_m['modelName']}:{_s(_m['modelVersion'])}"

            _name = "--"
            if "id" in _m:
                _name = _m["id"]
            else:
                _name = _m["version"] if show_removed else _s(_m["version"])

            _runtime = "--"
            if "runtime" in _m:
                if isinstance(_m["runtime"], str):
                    _runtime = _m["runtime"]
                else:
                    _r = _m["runtime"]
                    _runtime = f"[{_r['version']['id']}] {_r['name']}:{_r['version']['name'][:SHORT_VERSION_CNT]}"

            table.add_row(
                _name,
                _model,
                _datasets,
                _runtime,
                f"[{_style}]{_icon}{_status}[/]",
                f"{_m['device']}:{_m['deviceAmount']}" if "device" in _m else "--",
                _m[CREATED_AT_KEY],
                _m["finished_at"],
            )
            # TODO: add duration
        cls.print_header()
        console.print(table)
        return _jobs, _pager


class JobTermViewJson(JobTermView):
    @classmethod
    def list(  # type: ignore
        cls,
        project_uri: str = "",
        fullname: bool = False,
        show_removed: bool = False,
        page: int = DEFAULT_PAGE_IDX,
        size: int = DEFAULT_PAGE_SIZE,
    ) -> None:
        _data, _ = super().list(project_uri, fullname, show_removed, page, size)
        cls.pretty_json(_data)

    def info(
        self,
        page: int = DEFAULT_PAGE_IDX,
        size: int = DEFAULT_PAGE_SIZE,
        max_report_cols: int = DEFAULT_REPORT_COLS,
        web: bool = False,
    ) -> None:
        _rt = self.job.info(page, size)
        self.pretty_json(_rt)


def get_term_view(ctx_obj: t.Dict) -> t.Type[JobTermView]:
    return JobTermViewJson if ctx_obj.get("output") == "json" else JobTermViewRich
