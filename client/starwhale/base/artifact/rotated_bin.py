from __future__ import annotations

import os
import queue
import struct
import typing as t
import tempfile
from types import TracebackType
from pathlib import Path
from binascii import crc32

from starwhale.utils import console
from starwhale.consts import D_ALIGNMENT_SIZE, D_FILE_VOLUME_SIZE
from starwhale.utils.fs import ensure_dir


class RotatedBinWriter:
    """
    format:
        header_magic    uint32  I
        crc             uint32  I
        _reserved       uint64  Q
        size            uint32  I
        padding_size    uint32  I
        header_version  uint32  I
        data_magic      uint32  I --> above 32 bytes
        data bytes...
        padding bytes...        --> default 4K padding
    """

    _header_magic = struct.unpack(">I", b"SWDS")[0]
    _data_magic = struct.unpack(">I", b"SDWS")[0]
    _header_struct = struct.Struct(">IIQIIII")
    _header_size = _header_struct.size
    _header_version = 0

    class _BinSection(t.NamedTuple):
        offset: int
        size: int
        raw_data_offset: int
        raw_data_size: int

    def __init__(
        self,
        workdir: Path,
        rotated_bin_notify_queue: t.Optional[queue.Queue[t.Optional[Path]]] = None,
        alignment_bytes_size: int = D_ALIGNMENT_SIZE,
        volume_bytes_size: int = D_FILE_VOLUME_SIZE,
    ) -> None:
        self.workdir = workdir

        if alignment_bytes_size <= 0:
            raise ValueError(
                f"alignment_bytes_size must be greater than zero: {alignment_bytes_size}"
            )

        self.alignment_bytes_size = alignment_bytes_size

        if volume_bytes_size < 0:
            raise ValueError(
                f"volume_bytes_size must be greater than zero: {volume_bytes_size}"
            )
        self.volume_bytes_size = volume_bytes_size

        self._rotated_bin_notify_queue = rotated_bin_notify_queue
        self._wrote_size = 0

        ensure_dir(self.workdir)
        _, bin_writer_path = tempfile.mkstemp(
            prefix="bin-writer-", dir=str(self.workdir.absolute())
        )
        self._current_writer_path = Path(bin_writer_path)
        self._current_writer = self._current_writer_path.open("wb")
        self._rotated_bin_paths: t.List[Path] = []

    def __enter__(self) -> RotatedBinWriter:
        return self

    def __exit__(
        self,
        type: t.Optional[t.Type[BaseException]],
        value: t.Optional[BaseException],
        trace: TracebackType,
    ) -> None:
        if value:  # pragma: no cover
            console.warning(f"type:{type}, exception:{value}, traceback:{trace}")

        self.close()

    @property
    def working_path(self) -> Path:
        return self._current_writer_path

    @property
    def rotated_paths(self) -> t.List[Path]:
        return self._rotated_bin_paths

    def close(self) -> None:
        self._rotate()

        empty = self._current_writer.tell() == 0
        self._current_writer.close()
        if empty and self._current_writer_path.exists():
            # last file is empty
            os.unlink(self._current_writer_path)

    def _rotate(self) -> None:
        if self._wrote_size == 0:
            return
        self._current_writer.close()
        self._wrote_size = 0

        self._rotated_bin_paths.append(self._current_writer_path)
        if self._rotated_bin_notify_queue is not None:
            self._rotated_bin_notify_queue.put(self._current_writer_path)

        _, bin_writer_path = tempfile.mkstemp(
            prefix="bin-writer-", dir=str(self.workdir.absolute())
        )
        self._current_writer_path = Path(bin_writer_path)
        self._current_writer = self._current_writer_path.open("wb")

    def write(self, data: bytes) -> t.Tuple[Path, RotatedBinWriter._BinSection]:
        _cls = RotatedBinWriter
        size = len(data)
        crc = crc32(data)  # TODO: crc is right?
        start = self._current_writer.tell()
        padding_size = self._get_padding_size(size)

        _header = _cls._header_struct.pack(
            _cls._header_magic,
            crc,
            0,
            size,
            padding_size,
            _cls._header_version,
            _cls._data_magic,
        )
        _padding = b"\0" * padding_size
        self._current_writer.write(_header + data + _padding)
        _bin_path = self._current_writer_path
        _bin_section = _cls._BinSection(
            offset=start,
            size=_cls._header_size + size + padding_size,
            raw_data_offset=start + _cls._header_size,
            raw_data_size=size,
        )
        self._wrote_size += _bin_section.size

        if self._wrote_size > self.volume_bytes_size:
            self._rotate()

        return _bin_path, _bin_section

    def _get_padding_size(self, size: int) -> int:
        remain = (size + RotatedBinWriter._header_size) % self.alignment_bytes_size
        return 0 if remain == 0 else (self.alignment_bytes_size - remain)
