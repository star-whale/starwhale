import pickle
from pathlib import Path

from starwhale.api.dataset import BuildExecutor


def unpickle(file):
    with open(file, 'rb') as fo:
        content_dict = pickle.load(fo, encoding='bytes')
    return content_dict


class CIFAR10Slicer(BuildExecutor):

    def iter_data_slice(self, path: str):
        print(f"iter_data_slice for {path}")
        content_dict = unpickle(path)
        data_numpy = content_dict.get(b'data')
        idx = 0
        data_size = len(data_numpy)
        while True:
            last_idx = idx
            idx = idx + self._batch
            if idx > data_size:
                break
            yield data_numpy[last_idx:idx].tobytes()

    def iter_label_slice(self, path: str):
        print(f"iter_data_slice for {path}")
        content_dict = unpickle(path)
        labels_list = content_dict.get(b'labels')
        idx = 0
        data_size = len(labels_list)
        while True:
            last_idx = idx
            idx = idx + self._batch
            if idx > data_size:
                break
            yield bytes(labels_list[last_idx:idx])


if __name__ == "__main__":
    executor = CIFAR10Slicer(
        data_dir=Path("../data"),
        data_filter="test_batch", label_filter="test_batch",
        batch=50,
        alignment_bytes_size=4 * 1024,
        volume_bytes_size=4 * 1024 * 1024,
    )
    executor.make_swds()
