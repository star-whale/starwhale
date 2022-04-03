import struct
from abc import ABCMeta, abstractmethod
import typing as t
from pathlib import Path
import math
from binascii import crc32

import jsonlines

from starwhale.swds.dataset import (
    D_ALIGNMENT_SIZE, D_USER_BATCH_SIZE, D_FILE_VOLUME_SIZE
)

#TODO: tune header size
_header_magic = struct.unpack(">I", b"SWDS")[0]
_data_magic = struct.unpack(">I", b"SDWS")[0]
_header_struct = struct.Struct(">IIQIIII")
_header_size = _header_struct.size


class BuildExecutor(object):
    """
    BuildExecutor can build swds.

    swds_bin format:
        header_magic  uint32  I
        crc           uint32  I
        idx           uint64  Q
        size          uint32  I
        padding_size  uint32  I
        batch_size    uint32  I
        data_magic    uint32  I --> above 32 bytes
        data bytes...
        padding bytes...        --> default 4K padding
    """
    #TODO: add more docstring for class

    __metaclass__ = ABCMeta

    INDEX_NAME = "index.jsonl"
    _DATA_TMP_IDX = "_tmp_index_data.jsonl"
    _LABEL_TMP_IDX = "_tmp_index_label.jsonl"
    _DATA_FMT = "data_ubyte_{index}.swds_bin"
    _LABEL_FMT = "label_ubyte_{index}.swds_bin"

    def __init__(self,
                 data_dir:Path = Path("."),
                 output_dir: Path = Path("./sw_output"),
                 data_filter:str ="*", label_filter:str ="*",
                 batch:int =D_USER_BATCH_SIZE,
                 alignment_bytes_size:int = D_ALIGNMENT_SIZE,
                 volume_bytes_size:int = D_FILE_VOLUME_SIZE,
                 ) -> None:
        #TODO: add more docstring for args
        #TODO: validate group upper and lower?
        self._batch = max(batch, 1)
        self.data_dir = data_dir
        self.data_filter = data_filter
        self.label_filter = label_filter
        self.output_dir = output_dir
        self.alignment_bytes_size = alignment_bytes_size
        self.volume_bytes_size = volume_bytes_size

        self._index_writer = None
        self._prepare()

    def _prepare(self):
        self.output_dir.mkdir(parents=True, exist_ok=True)
        self._index_writer = jsonlines.open(str((self.output_dir / self.INDEX_NAME).resolve()), mode="w")

    def __exit__(self):
        try:
            self._index_writer.close() # type: ignore
        except Exception as e:
            print(f"index writer close exception: {e}")

        print("cleanup done.")

    def _write(self, writer, idx: int, data: bytes) -> t.Tuple[int, int]:
        size = len(data)
        crc = crc32(data) #TODO: crc is right?
        start = writer.tell()
        padding_size = self._get_padding_size(size + _header_size)

        _header = _header_struct.pack(
            _header_magic, crc, idx, size, padding_size, self._batch, _data_magic
        )
        _padding = b'\0' * padding_size
        writer.write(_header + data + _padding)
        return start, _header_size + size + padding_size

    def _get_padding_size(self, size):
        remain = (size + _header_size) % self.alignment_bytes_size
        return 0 if remain == 0 else (self.alignment_bytes_size - remain)

    def _write_index(self, idx, fno, data_pos, data_size, label_pos, label_size):
        self._index_writer.write(  # type: ignore
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

        for idx, (data, label) in enumerate(zip(self.iter_all_dataset_slice(), self.iter_all_label_slice())):
            data_pos, data_size = self._write(dwriter, idx, data)
            label_pos, label_size = self._write(lwriter, idx, label)
            self._write_index(idx, fno, data_pos, data_size, label_pos, label_size)

            wrote_size += data_size
            if wrote_size > self.volume_bytes_size:
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

    def _iter_files(self, filter, sort_key=None):
        _key = sort_key
        if _key is not None and not callable(_key):
            raise Exception(f"data_sort_func({_key}) is not callable.")

        _files = sorted(self.data_dir.rglob(filter), key=_key)
        for p in _files:
            if not p.is_file():
                continue
            yield p

    def iter_data_files(self):
        return self._iter_files(self.data_filter, self.data_sort_func())

    def iter_label_files(self):
        return self._iter_files(self.label_filter, self.label_sort_func())

    def iter_all_dataset_slice(self):
        for p in self.iter_data_files():
            for d in self.iter_data_slice(str(p.absolute())):
                yield d

    def iter_all_label_slice(self):
        for p in self.iter_label_files():
            for d in self.iter_label_slice(str(p.absolute())):
                yield d

    @abstractmethod
    def iter_data_slice(self, path: str):
        raise NotImplementedError

    @abstractmethod
    def iter_label_slice(self, path: str):
        raise NotImplementedError

    def data_sort_func(self):
        return None

    def label_sort_func(self):
        return None


class MNISTBuildExecutor(BuildExecutor):

    def iter_data_slice(self, path: str):
        fpath = Path(path)

        with fpath.open("rb") as f:
            _, number, hight, width = struct.unpack(">IIII", f.read(16))
            print(f">data({fpath.name}) split {math.ceil(number / self._batch)} group")

            while True:
                content = f.read(self._batch * hight * width)
                if not content:
                    break
                yield content

    def iter_label_slice(self, path: str):
        fpath = Path(path)

        with fpath.open("rb") as f:
            _, number = struct.unpack(">II", f.read(8))
            print(f">label({fpath.name}) split {math.ceil(number / self._batch)} group")

            while True:
                content = f.read(self._batch)
                if not content:
                    break
                yield content


#TODO: define some open dataset class, like ImageNet, COCO