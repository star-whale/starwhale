import struct
import os
import typing as t
from pathlib import Path
from collections import namedtuple
import math
from binascii import crc32

import jsonlines

#TODO: add file header magic?
"""
bin format:
    header_magic  uint32  I
    crc           uint32  I
    idx           uint64  Q
    size          uint32  I
    data_magic    uint32  I --> above 24 bytes
    data bytes...
    padding bytes...
"""
header_magic = struct.unpack(">I", b"SWDS")[0]
data_magic = struct.unpack(">I", b"SDWS")[0]
header_struct = struct.Struct(">IIQII")
header_size = header_struct.size


class DataSetProcessExecutor(object):
    INDEX_NAME = "index.jsonl"
    _DATA_TMP_IDX = "_tmp_index_data.jsonl"
    _LABEL_TMP_IDX = "_tmp_index_label.jsonl"
    _DATA_FMT = "data_ubyte_{index}.swds_bin"
    _LABEL_FMT = "label_ubyte_{index}.swds_bin"
    _FILE_VOLUME_SIZE = 4 * 1024 * 1024  # 4MB
    _DATA_ALIGNMENT_SIZE = 4 * 1024      # 4K for pagecache?

    def __init__(self, workdir=".", data_filter="*", label_filter="*",
                 data_sort_key=None, label_sort_key=None, batch=50) -> None:

        #TODO: validate group upper and lower?
        self._batch = max(batch, 1)
        self.workdir = Path(workdir)
        self.data_filter = data_filter
        self.label_filter = label_filter
        self.data_sort_key = data_sort_key or None
        self.label_sort_key = label_sort_key or None
        self.output_dir = self.workdir / "sw_output"

        self._index_writer = None

        self._prepare()

    def _prepare(self):
        self.output_dir.mkdir(parents=True, exist_ok=True)
        self._index_writer = jsonlines.open(str((self.output_dir / self.INDEX_NAME).resolve()), mode="w")

    def __exit__(self):
        try:
            self._index_writer.close()
        except Exception as e:
            print(f"index writer close exception: {e}")

        print("cleanup done.")

    def _write(self, writer, idx: int, data: bytes) -> t.Tuple[int, int]:
        size = len(data)
        crc = crc32(data) #TODO: crc is right?

        _header = header_struct.pack(header_magic, crc, idx, size, data_magic)
        _padding = b'\0' * self._get_padding_size(size + header_size)
        start = writer.tell()
        writer.write(_header + data + _padding)
        return start, size

    def _get_padding_size(self, size):
        remain = (size + header_size) % self._DATA_ALIGNMENT_SIZE
        return 0 if remain == 0 else (self._DATA_ALIGNMENT_SIZE - remain)

    def _write_index(self, idx, fno, data_pos, data_size, label_pos, label_size):
        self._index_writer.write(
            dict(
                id=idx,
                batch=self._batch,
                data=dict(
                    file=self._DATA_FMT.format(index=fno),
                    offset=data_pos,
                    size=data_size,
                ),
                label=dict(
                    file=self._LABEL_FMT.format(index=fno),
                    offset=label_pos,
                    size=label_size,
                )
            )
        )

    def make_swds(self):
        #TODO: add lock
        fno, wrote_size = 0, 0
        dwriter = (self.output_dir / self._DATA_FMT.format(index=fno)).open("wb")
        lwriter = (self.output_dir / self._LABEL_FMT.format(index=fno)).open("wb")

        for idx, (data, label) in enumerate(zip(self.iter_dataset_slice(), self.iter_label_slice())):
            data_pos, data_size = self._write(dwriter, idx, data)
            label_pos, label_size = self._write(lwriter, idx, label)
            self._write_index(idx, fno, data_pos, data_size, label_pos, label_size)

            wrote_size += data_size
            if wrote_size > self._FILE_VOLUME_SIZE:
                wrote_size = 0
                fno += 1

                dwriter.close()
                lwriter.close()

                dwriter = (self.output_dir / self._DATA_FMT.format(index=fno)).open("wb")
                lwriter = (self.output_dir / self._LABEL_FMT.format(index=fno)).open("wb")

        try:
            dwriter.close()
            lwriter.close()
        except Exception as e:
            print(f"data/label write close exception: {e}")

    #TODO: abstract decorator
    def iter_data_files(self):
        _files = sorted(self.workdir.rglob(self.data_filter), key=self.data_sort_key)
        for p in _files:
            if not p.is_file():
                continue
            yield p

    def iter_label_files(self):
        _files = sorted(self.workdir.rglob(self.label_filter), key=self.label_sort_key)
        for p in _files:
            if not p.is_file():
                continue
            yield p

    def iter_dataset_slice(self):
        for p in self.iter_data_files():
            with p.open("rb") as f:
                _, number, hight, width = struct.unpack(">IIII", f.read(16))
                print(f">data({p.name}) split {math.ceil(number / self._batch)} group")

                while True:
                    content = f.read(self._batch * hight * width)
                    if not content:
                        break
                    yield content

    def iter_label_slice(self):
        for p in self.iter_label_files():
            with p.open("rb") as f:
                _, number = struct.unpack(">II", f.read(8))
                print(f">label({p.name}) split {math.ceil(number / self._batch)} group")

                while True:
                    content = f.read(self._batch)
                    if not content:
                        break
                    yield content


if __name__ == "__main__":
    executor = DataSetProcessExecutor(
        workdir=os.path.dirname(__file__),
        data_filter="*images*", label_filter="*labels*"
    )
    executor.make_swds()