from __future__ import annotations

import typing as t
from pathlib import Path

from rich import box
from rich.table import Table
from rich.pretty import Pretty

from starwhale.utils import console, pretty_bytes, pretty_merge_list
from starwhale.consts import DEFAULT_PAGE_IDX, DEFAULT_PAGE_SIZE, SHORT_VERSION_CNT
from starwhale.base.type import DatasetChangeMode, DatasetFolderSourceType
from starwhale.base.view import BaseTermView
from starwhale.base.uri.project import Project
from starwhale.base.uri.resource import Resource, ResourceType
from starwhale.core.dataset.type import Text, DatasetConfig
from starwhale.core.runtime.process import Process as RuntimeProcess

from .model import Dataset


class DatasetTermView(BaseTermView):
    def __init__(self, dataset_uri: str | Resource) -> None:
        super().__init__()

        if isinstance(dataset_uri, Resource):
            self.uri = dataset_uri
        else:
            self.uri = Resource(dataset_uri, typ=ResourceType.dataset)
        self.dataset = Dataset.get_dataset(self.uri)

    @BaseTermView._simple_action_print
    def remove(self, force: bool = False) -> t.Tuple[bool, str]:
        return self.dataset.remove(force)

    @BaseTermView._simple_action_print
    def recover(self, force: bool = False) -> t.Tuple[bool, str]:
        return self.dataset.recover(force)

    @BaseTermView._pager
    @BaseTermView._header
    def history(self, fullname: bool = False) -> t.List[t.Dict[str, t.Any]]:
        fullname = fullname or self.uri.instance.is_cloud
        return self._print_history(
            title="Dataset History List",
            history=self.dataset.history(),
            fullname=fullname,
        )

    def info(self) -> None:
        info = self.dataset.info()
        if info:
            console.print(Pretty(info, expand_all=True))
        else:
            console.print(f":bird: Dataset info not found: [bold red]{self.uri}[/]")

    def summary(self) -> None:
        summary = self.dataset.summary()
        if summary:
            console.print(Pretty(summary.asdict(), expand_all=True))
        else:
            console.print(":tea: not found dataset summary")

    def _do_diff(self, compare_uri: Resource) -> t.Dict[str, t.Any]:
        r = self.dataset.diff(compare_uri)
        r["diff_merged_output"] = {
            "added": pretty_merge_list(r["diff"]["added"]),
            "deleted": pretty_merge_list(r["diff"]["deleted"]),
            "updated": pretty_merge_list(
                [row["id"] for row, _ in r["diff"]["updated"]]
            ),
        }
        return r

    def diff(self, compare_uri: Resource, show_details: bool = False) -> None:
        _print_dict: t.Callable[[t.Dict], str] = lambda _s: "\n".join(
            [f"{k}:{v}" for k, v in _s.items()]
        )
        r = self._do_diff(compare_uri)
        table = Table(box=box.SIMPLE, expand=False, show_lines=True)
        table.add_column()
        table.add_column(
            f"base: {r['version']['base'][:SHORT_VERSION_CNT]}",
            style="magenta",
            justify="left",
        )
        table.add_column(
            f"compare: {r['version']['compare'][:SHORT_VERSION_CNT]}",
            style="cyan",
            justify="left",
        )

        table.add_row(
            "diff summary",
            _print_dict(r["diff_rows"]),
            style="red",
        )
        table.add_row(
            "dataset version",
            r["version"]["base"],
            r["version"]["compare"],
        )
        table.add_row(
            "dataset digest",
            _print_dict(r["summary"]["base"]),
            _print_dict(r["summary"]["compare"]),
        )

        def _print_diff(_diff: t.Union[str, list]) -> str:
            if isinstance(_diff, (list, tuple)):
                _diff = ",".join([str(_d) for _d in _diff])
            return _diff or "--"

        def _str_row(row: t.Dict) -> str:
            data_uri = (
                row["data_link"].uri[:SHORT_VERSION_CNT]
                if row["object_store_type"] == "local"
                else row["data_link"].uri
            )
            return f"{row['id']}:offset-{row['data_offset']}:size-{row['data_size']}:uri-{data_uri}"

        table.add_row("diff-deleted", _print_diff(r["diff_merged_output"]["deleted"]))
        table.add_row("diff-added", "", _print_diff(r["diff_merged_output"]["added"]))

        if show_details:
            _bout, _cout = [], []
            for _brow, _crow in r["diff"]["updated"]:
                _bout.append(_str_row(_brow))
                _cout.append(_str_row(_crow))

            table.add_row("diff-updated", "\n".join(_bout), "\n".join(_cout))
        else:
            table.add_row(
                "diff-updated", _print_diff(r["diff_merged_output"]["updated"])
            )

        console.print(table)

    @classmethod
    def list(
        cls,
        project_uri: t.Union[str, Project] = "",
        fullname: bool = False,
        show_removed: bool = False,
        page: int = DEFAULT_PAGE_IDX,
        size: int = DEFAULT_PAGE_SIZE,
        filters: t.Optional[t.List[str]] = None,
    ) -> t.Tuple[t.List[t.Dict[str, t.Any]], t.Dict[str, t.Any]]:
        filters = filters or []
        if isinstance(project_uri, str):
            _uri = Project(project_uri)
        else:
            _uri = project_uri

        cls.must_have_project(_uri)
        fullname = fullname or _uri.instance.is_cloud
        _datasets, _pager = Dataset.list(_uri, page, size, filters)
        _data = BaseTermView.list_data(_datasets, show_removed, fullname)
        return _data, _pager

    @classmethod
    @BaseTermView._only_standalone
    def build_from_huggingface(
        cls,
        repo: str,
        name: str,
        project_uri: str,
        alignment_size: int | str,
        volume_size: int | str,
        subset: str | None = None,
        split: str | None = None,
        revision: str = "main",
        mode: DatasetChangeMode = DatasetChangeMode.PATCH,
        cache: bool = True,
    ) -> None:
        dataset_uri = cls.prepare_build_bundle(
            project=project_uri,
            bundle_name=name,
            typ=ResourceType.dataset,
            auto_gen_version=False,
        )
        ds = Dataset.get_dataset(dataset_uri)
        ds.build_from_huggingface(
            repo=repo,
            subset=subset,
            split=split,
            revision=revision,
            alignment_size=alignment_size,
            volume_size=volume_size,
            mode=mode,
            cache=cache,
        )

    @classmethod
    @BaseTermView._only_standalone
    def build_from_json_file(
        cls,
        json_file_path: str,
        name: str,
        project_uri: str,
        alignment_size: int | str,
        volume_size: int | str,
        field_selector: str = "",
        mode: DatasetChangeMode = DatasetChangeMode.PATCH,
    ) -> None:
        dataset_uri = cls.prepare_build_bundle(
            project=project_uri,
            bundle_name=name,
            typ=ResourceType.dataset,
            auto_gen_version=False,
        )
        ds = Dataset.get_dataset(dataset_uri)
        ds.build_from_json_file(
            json_file_path=json_file_path,
            field_selector=field_selector,
            alignment_size=alignment_size,
            volume_size=volume_size,
            mode=mode,
        )

    @classmethod
    @BaseTermView._only_standalone
    def build_from_folder(
        cls,
        folder: Path,
        kind: DatasetFolderSourceType,
        name: str,
        project_uri: str,
        auto_label: bool,
        alignment_size: int | str,
        volume_size: int | str,
        mode: DatasetChangeMode = DatasetChangeMode.PATCH,
    ) -> None:
        dataset_uri = cls.prepare_build_bundle(
            project=project_uri,
            bundle_name=name,
            typ=ResourceType.dataset,
            auto_gen_version=False,
        )
        ds = Dataset.get_dataset(dataset_uri)
        ds.build_from_folder(
            folder=folder,
            kind=kind,
            auto_label=auto_label,
            alignment_size=alignment_size,
            volume_size=volume_size,
            mode=mode,
        )

    @classmethod
    @BaseTermView._only_standalone
    def build(
        cls,
        workdir: str | Path,
        config: DatasetConfig,
        mode: DatasetChangeMode = DatasetChangeMode.PATCH,
    ) -> None:
        if config.runtime_uri:
            RuntimeProcess(uri=config.runtime_uri).run()
        else:
            dataset_uri = cls.prepare_build_bundle(
                project=config.project_uri,
                bundle_name=config.name,
                typ=ResourceType.dataset,
                auto_gen_version=False,
            )
            ds = Dataset.get_dataset(dataset_uri)
            ds.build(workdir=Path(workdir), config=config, mode=mode)

    @classmethod
    def copy(
        cls,
        src_uri: str,
        dest_uri: str,
        mode: DatasetChangeMode = DatasetChangeMode.PATCH,
        dest_local_project_uri: str = "",
        force: bool = False,
    ) -> None:
        Dataset.copy(
            src_uri=Resource(src_uri, typ=ResourceType.dataset),
            dest_uri=dest_uri,
            mode=mode,
            dest_local_project_uri=dest_local_project_uri,
            force=force,
        )
        console.print(":clap: copy done")

    @BaseTermView._header
    def tag(
        self, tags: t.List[str], remove: bool = False, ignore_errors: bool = False
    ) -> None:
        if remove:
            console.print(f":golfer: remove tags {tags} @ {self.uri}...")
            self.dataset.remove_tags(tags, ignore_errors)
        else:
            console.print(f":surfer: add tags {tags} @ {self.uri}...")
            self.dataset.add_tags(tags, ignore_errors)

    @BaseTermView._header
    def head(
        self, rows: int, show_raw_data: bool = False, show_types: bool = False
    ) -> None:
        from starwhale.api._impl.data_store import _get_type

        for row in self.dataset.head(rows, show_raw_data):
            console.rule(f"row [{row['index']}]", align="left")
            output = f":deciduous_tree: id: {row['index']} \n" ":cyclone: features:\n"
            for _k, _v in row["features"].items():
                if show_raw_data and isinstance(_v, Text):
                    _v = _v.link_to_content()
                output += f"\t :dim_button: [bold green]{_k}[/] : {_v} \n"

            if show_types:
                output += ":school_satchel: features types:\n"
                for _k, _v in row["features"].items():
                    ds_type: t.Any
                    try:
                        ds_type = _get_type(_v)
                    except RuntimeError:
                        ds_type = type(_v)
                    output += (
                        f"\t :droplet: [bold green]{_k}[/] : {ds_type} | {type(_v)} \n"
                    )

            console.print(output)


class DatasetTermViewRich(DatasetTermView):
    @classmethod
    @BaseTermView._pager
    def list(
        cls,
        project_uri: str = "",
        fullname: bool = False,
        show_removed: bool = False,
        page: int = DEFAULT_PAGE_IDX,
        size: int = DEFAULT_PAGE_SIZE,
        filters: t.Optional[t.List[str]] = None,
    ) -> t.Tuple[t.List[t.Dict[str, t.Any]], t.Dict[str, t.Any]]:
        filters = filters or []
        _datasets, _pager = super().list(
            project_uri, fullname, show_removed, page, size, filters
        )
        custom_column: t.Dict[str, t.Callable[[t.Any], str]] = {
            "tags": lambda x: ",".join(x),
            "size": lambda x: pretty_bytes(x),
            "runtime": cls.place_holder_for_empty(),
        }

        cls.print_header()
        cls.print_table("Dataset List", _datasets, custom_column=custom_column)
        return _datasets, _pager


class DatasetTermViewJson(DatasetTermView):
    @classmethod
    def list(  # type: ignore
        cls,
        project_uri: str = "",
        fullname: bool = False,
        show_removed: bool = False,
        page: int = DEFAULT_PAGE_IDX,
        size: int = DEFAULT_PAGE_SIZE,
        filters: t.Optional[t.List[str]] = None,
    ) -> None:
        filters = filters or []
        _datasets, _pager = super().list(
            project_uri, fullname, show_removed, page, size, filters
        )
        cls.pretty_json(_datasets)

    def info(self) -> None:
        self.pretty_json(self.dataset.info())

    def head(
        self, rows: int, show_raw_data: bool = False, show_types: bool = False
    ) -> None:
        from starwhale.base.mixin import _do_asdict_convert

        # TODO: support show_types in the json format output

        info = self.dataset.head(rows, show_raw_data)
        self.pretty_json(_do_asdict_convert(info))

    def diff(self, compare_uri: Resource, show_details: bool = False) -> None:
        r = self._do_diff(compare_uri)
        if not show_details:
            r.pop("diff", None)

        self.pretty_json(r)

    def history(self, fullname: bool = False) -> None:
        fullname = fullname or self.uri.instance.is_cloud
        self.pretty_json(
            BaseTermView.get_history_data(self.dataset.history(), fullname)
        )


def get_term_view(ctx_obj: t.Dict) -> t.Type[DatasetTermView]:
    return (
        DatasetTermViewJson if ctx_obj.get("output") == "json" else DatasetTermViewRich
    )
