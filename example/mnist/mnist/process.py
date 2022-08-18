import struct
from pathlib import Path

from starwhale.api.dataset import SWDSBinBuildExecutor, UserRawBuildExecutor


def _do_iter_label_slice(path: str):
    fpath = Path(path)

    with fpath.open("rb") as f:
        _, number = struct.unpack(">II", f.read(8))
        print(f">label({fpath.name}) split {number} group")

        while True:
            content = f.read(1)
            if not content:
                break
            yield content


class DataSetProcessExecutor(SWDSBinBuildExecutor):
    def iter_data_slice(self, path: str):
        fpath = Path(path)

        with fpath.open("rb") as f:
            _, number, height, width = struct.unpack(">IIII", f.read(16))
            print(f">data({fpath.name}) split {number} group")

            while True:
                content = f.read(height * width)
                if not content:
                    break
                yield content

    def iter_label_slice(self, path: str):
        return _do_iter_label_slice(path)


class RawDataSetProcessExecutor(UserRawBuildExecutor):
    def iter_data_slice(self, path: str):
        fpath = Path(path)

        with fpath.open("rb") as f:
            _, number, height, width = struct.unpack(">IIII", f.read(16))
            size = height * width
            offset = 16

            for _ in range(number):
                yield offset, size
                offset += size

    def iter_label_slice(self, path: str):
        return _do_iter_label_slice(path)
