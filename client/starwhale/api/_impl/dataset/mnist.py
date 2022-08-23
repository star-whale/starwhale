import struct
import typing as t
from pathlib import Path

from .builder import SWDSBinBuildExecutor


class MNISTBuildExecutor(SWDSBinBuildExecutor):
    def iter_data_slice(self, path: str) -> t.Generator[bytes, None, None]:
        fpath = Path(path)

        with fpath.open("rb") as f:
            _, number, height, width = struct.unpack(">IIII", f.read(16))
            print(f">data({fpath.name}) split {number} group")

            while True:
                content = f.read(height * width)
                if not content:
                    break
                yield content

    def iter_label_slice(self, path: str) -> t.Generator[bytes, None, None]:
        fpath = Path(path)

        with fpath.open("rb") as f:
            _, number = struct.unpack(">II", f.read(8))
            print(f">label({fpath.name}) split {number} group")

            while True:
                content = f.read(1)
                if not content:
                    break
                yield content
