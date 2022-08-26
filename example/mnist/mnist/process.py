import struct
from pathlib import Path

from starwhale.api.dataset import (
    Link,
    MIMEType,
    S3LinkAuth,
    SWDSBinBuildExecutor,
    UserRawBuildExecutor,
)


def _do_iter_label_slice(path: str):
    fpath = Path(path)

    with fpath.open("rb") as f:
        _, number = struct.unpack(">II", f.read(8))
        print(f">label({fpath.name}) split {number} group")

        while True:
            content = f.read(1)
            if not content:
                break
            yield struct.unpack(">B", content)[0]


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


class LinkRawDataSetProcessExecutor(RawDataSetProcessExecutor):
    _auth = S3LinkAuth(name="mnist", access_key="minioadmin", secret="minioadmin")
    _endpoint = "10.131.0.1:9000"
    _bucket = "users"

    def iter_all_dataset_slice(self):
        offset = 16
        size = 28 * 28
        uri = (
            f"s3://{self._endpoint}@{self._bucket}/dataset/mnist/t10k-images-idx3-ubyte"
        )
        for _ in range(10000):
            link = Link(
                f"{uri}",
                self._auth,
                offset=offset,
                size=size,
                mime_type=MIMEType.GRAYSCALE,
            )
            yield link
            offset += size
