import typing as t
import yaml

import click
from rich import box
from rich.table import Table
from rich.pretty import Pretty

from starwhale.base.store import LocalStorage
from starwhale.consts import (
    DEFAULT_MANIFEST_NAME,
    SHORT_VERSION_CNT,
    VERSION_PREFIX_CNT,
    CURRENT_FNAME,
)
from starwhale.utils.fs import empty_dir


class EvalLocalStorage(LocalStorage):
    def list(self, filter: str = "", title: str = "", caption: str = "") -> None:
        title = title or "List StarWhale Evaluation Result in local storage"
        caption = caption or f"@{self.eval_run_dir}"

        table = Table(title=title, caption=caption, box=box.SIMPLE, expand=True)
        table.add_column("Name", justify="left", style="cyan", no_wrap=False)
        table.add_column("Version", style="cyan")
        table.add_column("Model")
        table.add_column("Datasets")
        table.add_column("Phase")
        table.add_column("Status", style="red")
        table.add_column("Created At", style="magenta")
        table.add_column("Finished At", style="magenta")

        def _s(x: str) -> str:
            if ":" in x:
                _n, _v = x.split(":")
                return f"{_n}:{_v[:SHORT_VERSION_CNT]}"
            else:
                return x[:SHORT_VERSION_CNT]

        for _, _r in self.iter_run_result(filter):
            table.add_row(
                _r["name"] or "--",
                _s(_r["version"]),
                _s(_r["model"]),
                "\n".join([_s(d) for d in _r["datasets"]]),
                _r["phase"],
                _r.get("status", "--"),
                _r["created_at"],
                _r["finished_at"],
            )
        self._console.print(table)

    # TODO: add yield typing hint
    def iter_run_result(self, filter: str) -> t.Any:
        for _mf in self.eval_run_dir.glob(f"**/**/{DEFAULT_MANIFEST_NAME}"):
            yield _mf, yaml.safe_load(_mf.open())

    def info(self, version: str) -> None:
        from .executor import EvalTaskType, render_cmp_report, RunSubDirType

        _dir = self._guess(self.eval_run_dir / version[:VERSION_PREFIX_CNT], version)
        _mf = _dir / DEFAULT_MANIFEST_NAME
        if not _mf.exists():
            self._console.print(f":tea: not found {_mf}")
        else:
            _m = yaml.safe_load(_mf.open())
            self._console.rule(
                f"[green bold]Inspect {DEFAULT_MANIFEST_NAME} for eval:{version}"
            )
            self._console.print(Pretty(_m, expand_all=True))

        _rpath = _dir / EvalTaskType.CMP / RunSubDirType.RESULT / CURRENT_FNAME
        if _rpath.exists():
            render_cmp_report(_rpath)
        else:
            self._console.print(":bomb: no report to render")

        self._console.rule("Evaluation process dirs")
        self._console.print(f":cactus: ppl: {_dir/EvalTaskType.PPL}")
        self._console.print(f":camel: cmp: {_dir/EvalTaskType.CMP}")

    def delete(self, version: str) -> None:
        _dir = self._guess(self.eval_run_dir / version[:VERSION_PREFIX_CNT], version)
        if _dir.exists() and _dir.is_dir():
            click.confirm(f"continue to delete {_dir}", abort=True)
            empty_dir(_dir)
            self._console.print(f":bomb delete eval run dir: {_dir}")
        else:
            self._console.print(
                f":diving_mask: not found or no dir for {_dir}, skip to delete it"
            )

    def gc(self, dry_run: bool = False) -> None:
        pass

    def pull(self, sw_name: str) -> None:
        pass

    def push(self, sw_name: str) -> None:
        pass

    def iter_local_swobj(self) -> t.Generator["LocalStorage.SWobjMeta", None, None]:
        return super().iter_local_swobj()
