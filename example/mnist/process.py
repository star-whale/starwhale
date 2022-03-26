import math
import struct
from pathlib import Path

from starwhale.api.dataset import BuildExecutor

class DataSetProcessExecutor(BuildExecutor):

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


if __name__ == "__main__":
    executor = DataSetProcessExecutor(
        data_dir=Path(__file__) / "data",
        data_filter="*images*", label_filter="*labels*",
        batch=50,
        alignment_bytes_size=4 * 1024,
        volume_bytes_size=4 * 1024 * 1024,
    )
    executor.make_swds()