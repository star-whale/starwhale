import typing as t
import collections

from torch.utils.data import IterableDataset

import starwhale.base.data_type as sw_type
from starwhale.api._impl.dataset import Dataset

__all__ = ["TorchIterableDataset", "default_transform"]


def _dummy_transform(data: t.Any) -> t.Any:
    return data


def default_transform(data: t.Any) -> t.Any:
    data_type = type(data)
    if isinstance(
        data,
        (sw_type.Audio, sw_type.Image, sw_type.GrayscaleImage, sw_type.BoundingBox),
    ):
        return data.to_tensor()
    elif isinstance(data, (sw_type.Binary, sw_type.Video)):
        return data.to_bytes()
    elif isinstance(data, sw_type.Text):
        return data.to_str()
    elif isinstance(data, collections.abc.Mapping):  # type: ignore
        try:
            return data_type({k: default_transform(v) for k, v in data.items()})
        except TypeError:
            # The mapping type may not support __init__(iterable)
            return {k: default_transform(v) for k, v in data.items()}
    elif isinstance(data, collections.abc.Sequence):  # type: ignore
        if isinstance(data, (str, bytes)):
            return data
        else:
            try:
                return data_type([default_transform(d) for d in data])
            except TypeError:
                # The sequence type may not support __init__(iterable), (e.g.: range)
                return [default_transform(d) for d in data]
    else:
        return data


class TorchIterableDataset(IterableDataset):
    def __init__(
        self,
        dataset: Dataset,
        transform: t.Optional[t.Callable] = None,
        drop_index: bool = True,
        skip_default_transform: bool = False,
    ) -> None:
        super().__init__()
        self.dataset = dataset
        if transform is not None:
            self.transform = transform
        else:
            self.transform = (
                _dummy_transform if skip_default_transform else default_transform
            )

        self.drop_index = drop_index

    def __iter__(self) -> t.Iterator:
        _t = self.transform
        for row in self.dataset:
            if self.drop_index:
                yield _t(row.features)
            else:
                yield _t(row.index), _t(row.features)

    def __len__(self) -> int:
        return len(self.dataset)

    def __str__(self) -> str:
        return f"TorchIterableDataset from Starwhale Dataset: {self.dataset}, drop_index:{self.drop_index}, transform: {self.transform}"

    __repr__ = __str__
