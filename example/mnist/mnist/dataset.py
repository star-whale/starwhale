import struct
import typing as t
from pathlib import Path

from starwhale import Link, BuildExecutor, GrayscaleImage

_TItem = t.Generator[t.Dict[str, t.Any], None, None]


def iter_swds_bin_item() -> _TItem:
    root_dir = Path(__file__).parent.parent / "data"

    with (root_dir / "t10k-images-idx3-ubyte").open("rb") as data_file, (
        root_dir / "t10k-labels-idx1-ubyte"
    ).open("rb") as label_file:
        _, data_number, height, width = struct.unpack(">IIII", data_file.read(16))
        _, label_number = struct.unpack(">II", label_file.read(8))
        print(
            f">data({data_file.name}) split data:{data_number}, label:{label_number} group"
        )
        image_size = height * width

        for i in range(0, min(data_number, label_number)):
            _data = data_file.read(image_size)
            _label = struct.unpack(">B", label_file.read(1))[0]
            yield {
                "img": GrayscaleImage(
                    _data,
                    display_name=f"{i}",
                    shape=(height, width, 1),
                ),
                "label": _label,
            }


class DatasetProcessExecutor(BuildExecutor):
    def get_info(self) -> t.Optional[t.Dict[str, t.Any]]:
        return {"all_labels": list(range(0, 10))}

    def iter_item(self) -> _TItem:
        return iter_swds_bin_item()


def iter_user_raw_item() -> _TItem:
    root_dir = Path(__file__).parent.parent / "data"
    data_fpath = root_dir / "t10k-images-idx3-ubyte"
    label_fpath = root_dir / "t10k-labels-idx1-ubyte"

    with data_fpath.open("rb") as data_file, label_fpath.open("rb") as label_file:
        _, data_number, height, width = struct.unpack(">IIII", data_file.read(16))
        _, label_number = struct.unpack(">II", label_file.read(8))

        image_size = height * width
        offset = 16

        for i in range(0, min(data_number, label_number)):
            _data = GrayscaleImage(
                link=Link(
                    uri=str(data_fpath.absolute()),
                    offset=offset,
                    size=image_size,
                    with_local_fs_data=True,
                ),
                display_name=f"{i}",
                shape=(height, width, 1),
            )
            _label = struct.unpack(">B", label_file.read(1))[0]
            yield {"img": _data, "label": _label}
            offset += image_size


class RawDatasetProcessExecutor(BuildExecutor):
    def iter_item(self) -> _TItem:
        return iter_user_raw_item()


class LinkRawDatasetProcessExecutor(BuildExecutor):
    _endpoint = "10.131.0.1:9000"
    _bucket = "users"

    def iter_item(self) -> _TItem:
        root_dir = Path(__file__).parent.parent / "data"

        with (root_dir / "t10k-labels-idx1-ubyte").open("rb") as label_file:
            _, label_number = struct.unpack(">II", label_file.read(8))

            offset = 16
            image_size = 28 * 28

            uri = f"s3://{self._endpoint}/{self._bucket}/dataset/mnist/t10k-images-idx3-ubyte"
            for i in range(label_number):
                _data = GrayscaleImage(
                    link=Link(
                        f"{uri}",
                        offset=offset,
                        size=image_size,
                    ),
                    display_name=f"{i}",
                    shape=(28, 28, 1),
                )
                _label = struct.unpack(">B", label_file.read(1))[0]
                yield {"img": _data, "label": _label}
                offset += image_size
