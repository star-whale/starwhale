import typing as t

import numpy
import torch

__all__ = ["convert_list_to_tensor", "convert_numpy_to_tensor"]


def convert_list_to_tensor(data: t.List) -> torch.Tensor:
    if not isinstance(data, list):
        raise TypeError(f"data is not list type: {data}")

    return torch.Tensor(data)


def convert_numpy_to_tensor(data: numpy.ndarray) -> torch.Tensor:
    if not isinstance(data, numpy.ndarray):
        raise TypeError(f"data is not numpy.ndarray type: {data}")

    return torch.as_tensor(data)
