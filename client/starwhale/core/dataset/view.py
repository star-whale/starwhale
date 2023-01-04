import sys
import base64
import typing as t
from pathlib import Path

from rich import box
from rich.table import Table
from rich.pretty import Pretty

from starwhale.utils import console, pretty_bytes, pretty_merge_list
from starwhale.consts import DEFAULT_PAGE_IDX, DEFAULT_PAGE_SIZE, SHORT_VERSION_CNT
from starwhale.base.uri import URI
from starwhale.base.type import URIType, InstanceType
from starwhale.base.view import BaseTermView
from starwhale.core.dataset.type import DatasetConfig
from starwhale.core.runtime.process import Process as RuntimeProcess

from .model import Dataset


class DatasetTermView(BaseTermView):
    def __init__(self, dataset_uri: str) -> None:
        super().__init__()

        self.raw_uri = dataset_uri
        self.uri = URI(dataset_uri, expected_type=URIType.DATASET)
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
        fullname = fullname or self.uri.instance_type == InstanceType.CLOUD
        return self._print_history(
            title="Dataset History List",
            history=self.dataset.history(),
            fullname=fullname,
        )

    @BaseTermView._header
    def info(self, fullname: bool = False) -> None:
        self._print_info(self.dataset.info(), fullname=fullname)

    def summary(self) -> None:
        _summary = self.dataset.summary()
        if _summary:
            console.print(Pretty(_summary.asdict(), expand_all=True))
        else:
            console.print(":tea: not found dataset summary")
            sys.exit(1)

    def _do_diff(self, compare_uri: URI) -> t.Dict[str, t.Any]:
        r = self.dataset.diff(compare_uri)
        r["diff_merged_output"] = {
            "added": pretty_merge_list(r["diff"]["added"]),
            "deleted": pretty_merge_list(r["diff"]["deleted"]),
            "updated": pretty_merge_list(
                [row["id"] for row, _ in r["diff"]["updated"]]
            ),
        }
        return r

    def diff(self, compare_uri: URI, show_details: bool = False) -> None:
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
        project_uri: t.Union[str, URI] = "",
        fullname: bool = False,
        show_removed: bool = False,
        page: int = DEFAULT_PAGE_IDX,
        size: int = DEFAULT_PAGE_SIZE,
        filters: t.Optional[t.List[str]] = None,
    ) -> t.Tuple[t.List[t.Dict[str, t.Any]], t.Dict[str, t.Any]]:
        filters = filters or []
        if isinstance(project_uri, str):
            _uri = URI(project_uri, expected_type=URIType.PROJECT)
        else:
            _uri = project_uri

        fullname = fullname or (_uri.instance_type == InstanceType.CLOUD)
        _datasets, _pager = Dataset.list(_uri, page, size, filters)
        _data = BaseTermView.list_data(_datasets, show_removed, fullname)
        return _data, _pager

    @classmethod
    @BaseTermView._only_standalone
    def build(
        cls,
        workdir: str,
        config: DatasetConfig,
        disable_copy_src: bool = False,
    ) -> URI:
        dataset_uri = cls.prepare_build_bundle(
            project=config.project_uri, bundle_name=config.name, typ=URIType.DATASET
        )
        ds = Dataset.get_dataset(dataset_uri)

        kwargs = dict(
            workdir=Path(workdir), config=config, disable_copy_src=disable_copy_src
        )

        if config.runtime_uri:
            RuntimeProcess.from_runtime_uri(
                uri=config.runtime_uri, target=ds.build, kwargs=kwargs
            ).run()
        else:
            ds.build(**kwargs)
        return dataset_uri

    @classmethod
    def copy(
        cls,
        src_uri: str,
        dest_uri: str,
        force: bool = False,
        dest_local_project_uri: str = "",
    ) -> None:
        Dataset.copy(src_uri, dest_uri, force, dest_local_project_uri)
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
    def head(self, rows: int, show_raw_data: bool = False) -> None:
        from starwhale.api._impl.data_store import _get_type

        for row in self.dataset.head(rows, show_raw_data):
            console.rule(f"row [{row['index']}]", align="left")
            size = pretty_bytes(row["data"]["size"])
            output = (
                f":deciduous_tree: id: {row['data']['id']} \n"
                ":cyclone: data:\n"
                f"\t :dim_button: type: {row['data']['type']['type']} \n"
                f"\t :duck: mime: {row['data']['type']['mime_type']} \n"
                f"\t :palm_tree: display name: {row['data']['type'].get('display_name', '')} \n"
                f"\t :diving_mask: size: {size} \n"
                f"\t :dizzy: uri: {row['data']['link'].uri} \n"
                f":dna: annotations({len(row['annotations'])}): \n"
            )
            for _k, _v in row["annotations"].items():
                output += f"\t :droplet: {_k}: value[{_v}], type[{_get_type(_v)} | {type(_v)}] \n"

            if show_raw_data:
                output += f":parrot: raw data({size}): \n \t {row['data']['raw']}"

            console.print(output)


class DatasetTermViewRich(DatasetTermView):
    @classmethod
    @BaseTermView._pager
    @BaseTermView._header
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

    def info(self, fullname: bool = False) -> None:
        self.pretty_json(self.get_info_data(self.dataset.info(), fullname))

    def head(self, rows: int, show_raw_data: bool = False) -> None:
        from starwhale.base.mixin import _do_asdict_convert

        info = self.dataset.head(rows, show_raw_data)
        if show_raw_data:
            for i in info:
                i["data"]["raw"] = base64.b64encode(i["data"].get("raw", b"")).decode()
        self.pretty_json(_do_asdict_convert(info))

    def diff(self, compare_uri: URI, show_details: bool = False) -> None:
        r = self._do_diff(compare_uri)
        if not show_details:
            r.pop("diff", None)

        self.pretty_json(r)

    def history(self, fullname: bool = False) -> None:
        fullname = fullname or self.uri.instance_type == InstanceType.CLOUD
        self.pretty_json(
            BaseTermView.get_history_data(self.dataset.history(), fullname)
        )


def get_term_view(ctx_obj: t.Dict) -> t.Type[DatasetTermView]:
    return (
        DatasetTermViewJson if ctx_obj.get("output") == "json" else DatasetTermViewRich
    )
