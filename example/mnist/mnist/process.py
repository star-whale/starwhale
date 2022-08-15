import math
import struct
from pathlib import Path

from starwhale.api.dataset import BuildExecutor


class DataSetProcessExecutor(BuildExecutor):
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
        fpath = Path(path)

        with fpath.open("rb") as f:
            _, number = struct.unpack(">II", f.read(8))
            print(f">label({fpath.name}) split {number} group")

            while True:
                content = f.read(1)
                if not content:
                    break
                yield content


if __name__ == "__main__":
    with DataSetProcessExecutor(
        dataset_name="mnist",
        dataset_version="11223344",
        project_name="self",
        data_dir=Path(__file__) / "data",
        data_filter="*images*",
        label_filter="*labels*",
        alignment_bytes_size=4 * 1024,
        volume_bytes_size=4 * 1024 * 1024,
    ) as executor:
        executor.make_swds()
