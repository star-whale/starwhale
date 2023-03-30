from __future__ import annotations

import json
import queue
import typing as t
import threading
from pathlib import Path
from collections import defaultdict

from starwhale.consts import VERSION_PREFIX_CNT
from starwhale.utils.fs import (
    ensure_dir,
    ensure_file,
    blake2b_content,
    BLAKE2B_SIGNATURE_ALGO,
)
from starwhale.utils.error import NoSupportError
from starwhale.core.dataset.type import Link, BaseArtifact
from starwhale.api._impl.data_store import TableWriter, LocalDataStore

from .base import (
    TrackRecord,
    ParamsRecord,
    MetricsRecord,
    ArtifactsRecord,
    MetricsTabularRow,
    ArtifactsTabularRow,
    HandleQueueElementType,
)


class HandlerThread(threading.Thread):
    def __init__(
        self,
        workdir: t.Union[Path, str],
        handle_queue: queue.Queue[HandleQueueElementType],
        sync_queue: t.Optional[queue.Queue[t.Any]] = None,
    ) -> None:
        super().__init__(name="HandlerThread")

        self.handle_queue = handle_queue
        self.sync_queue = sync_queue

        self._run_exception: t.Optional[Exception] = None
        self._workdir = Path(workdir)
        self._data_store = LocalDataStore(str(self._workdir))
        self._table_writers: t.Dict[str, TableWriter] = {}
        self._params: t.Dict[str, t.Dict] = defaultdict(dict)
        # TODO: support non-in-memory artifacts auto-incr counter with datastore
        self._artifacts_counter: t.Dict = defaultdict(int)

        self.daemon = True

    def _raise_run_exception(self) -> None:
        if self._run_exception is not None:
            raise threading.ThreadError(
                f"HandlerThread raise exception: {self._run_exception}"
            )

    def flush(self) -> None:
        self._dump_params()

        for writer in self._table_writers.values():
            writer.flush()

        self._data_store.dump()

    def close(self) -> None:
        self.handle_queue.put(None)
        self.join()
        self.flush()

        for writer in self._table_writers.values():
            writer.close()

        self._raise_run_exception()

    def _handle_metrics(self, record: MetricsRecord) -> None:
        row = MetricsTabularRow.from_record(record)
        table_name = f"{record.typ.value}/{record.source.value}"
        self._update_table(table_name, row.asdict(), "__step")

    def _handle_artifacts(self, record: ArtifactsRecord) -> None:
        table_name = f"{record.typ.value}/{record.source.value}"
        store_dir = self._workdir / record.typ.value / "_files"

        def _convert_data_to_link(data: BaseArtifact) -> Link:
            raw_content = data.to_bytes()
            sign_name = blake2b_content(raw_content)
            fpath = (
                store_dir
                / BLAKE2B_SIGNATURE_ALGO
                / sign_name[:VERSION_PREFIX_CNT]
                / sign_name
            )
            ensure_file(fpath, raw_content, parents=True)

            return Link(
                uri=sign_name,
                offset=0,
                size=len(raw_content),
                use_plain_type=True,
                data_type=data,
            )

        for k, v in record.data.items():
            if isinstance(v, Link):
                data = v
            elif isinstance(v, BaseArtifact):
                data = _convert_data_to_link(v)
            else:
                raise NoSupportError(
                    f"artifact only accepts BaseArtifact or Link type: {v}"
                )

            row = ArtifactsTabularRow(
                name=k,
                index=self._artifacts_counter[k],
                created_at=record.clock_time,
                data=data,
                link_wrapper=not isinstance(v, Link),
            )
            self._update_table(table_name, row.asdict(), "__key")
            self._artifacts_counter[k] += 1

    def _handle_params(self, record: ParamsRecord) -> None:
        self._params[record.source.value].update(record.data)

    def _dump_params(self) -> None:
        params_dir = self._workdir / "params"
        ensure_dir(params_dir)

        for k, v in self._params.items():
            if not v:
                continue
            ensure_file(params_dir / f"{k}.json", json.dumps(v, separators=(",", ":")))

    def _dispatch(self, record: TrackRecord) -> None:
        if isinstance(record, MetricsRecord):
            self._handle_metrics(record)
        elif isinstance(record, ParamsRecord):
            self._handle_params(record)
        elif isinstance(record, ArtifactsRecord):
            self._handle_artifacts(record)
        else:
            raise NoSupportError(f"no support to handle {record}({type(record)})")

    def _update_table(self, table_name: str, data: t.Dict, key_column: str) -> None:
        if table_name not in self._table_writers:
            self._table_writers[table_name] = TableWriter(
                table_name=table_name,
                key_column=key_column,
                data_store=self._data_store,
                run_exceptions_limits=10,
            )

        writer = self._table_writers[table_name]
        writer.insert(data)

    def run(self) -> None:
        try:
            while True:
                record = self.handle_queue.get(block=True, timeout=None)
                if record is None:
                    if self.handle_queue.qsize() > 0:
                        continue
                    else:
                        break  # pragma: no cover

                self._dispatch(record)
        except Exception as e:
            self._run_exception = e
            raise
