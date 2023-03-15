import typing as t
import collections

import numpy
import tensorflow as tf

import starwhale.core.dataset.type as sw_type
from starwhale.utils.error import NoSupportError
from starwhale.api._impl.dataset import Dataset

__all__ = ["to_tf_dataset"]

# https://www.tensorflow.org/api_docs/python/tf/dtypes
_TYPE_DICT: t.Dict[t.Any, tf.DType] = {
    int: tf.dtypes.int64,
    float: tf.dtypes.float64,
    complex: tf.dtypes.complex128,
    bool: tf.dtypes.bool,
    str: tf.dtypes.string,
    bytes: tf.dtypes.string,
}

# TODO: sw_type supports Sequence Type to store list[dict] type: [{"a": 1}, {"a":2}]


def _transform(data: t.Any) -> t.Any:
    dtype = type(data)
    if dtype in _TYPE_DICT:
        return data

    if isinstance(data, (sw_type.BaseArtifact, sw_type.BoundingBox)):
        return data.to_numpy()
    elif isinstance(data, sw_type.Text):
        return data.to_str()
    elif isinstance(data, collections.abc.Mapping):  # type: ignore
        try:
            return dtype({k: _transform(v) for k, v in data.items()})
        except TypeError:
            # The mapping type may not support __init__(iterable)
            return {k: _transform(v) for k, v in data.items()}
    elif isinstance(data, collections.abc.Sequence):  # type: ignore
        try:
            return dtype([_transform(d) for d in data])
        except TypeError:
            # The sequence type may not support __init__(iterable), (e.g.: range)
            return [_transform(d) for d in data]
    else:
        return data


def _inspect_spec(data: t.Any) -> t.Union[tf.TensorSpec, t.Dict]:
    dtype = type(data)
    if dtype in _TYPE_DICT:
        return tf.TensorSpec(shape=(), dtype=_TYPE_DICT[dtype])

    if isinstance(data, (sw_type.BaseArtifact, sw_type.BoundingBox)):
        return tf.TensorSpec(shape=data.shape, dtype=data.dtype)
    elif isinstance(data, (list, tuple)):
        narray = numpy.array(data)
        narray_shape: t.List[t.Optional[int]] = list(narray.shape)
        narray_dtype: numpy.dtype = narray.dtype

        if narray_dtype == numpy.object_:
            _ravel_list = narray.ravel().tolist()
            _type_cnt = len(set([type(n) for n in _ravel_list]))

            if isinstance(_ravel_list[0], (list, tuple)):
                raise ValueError(f"Can't ravel to one dimension array: {_ravel_list}")

            if _type_cnt > 1:
                raise NoSupportError(
                    f"Can't convert different types in one array to tensor: {data}"
                )
            elif _type_cnt == 0:
                raise ValueError(
                    f"Can't find any types, but numpy array type is numpy.object_: {data}"
                )
            else:
                _spec = _inspect_spec(_ravel_list[0])
                if isinstance(_spec, tf.TensorSpec):
                    narray_dtype = _spec.dtype
                    _shape = _spec.shape.as_list()
                    narray_shape.extend(_shape)
                else:
                    raise NoSupportError(
                        f"Can't handle the compound type: {_ravel_list[0]}"
                    )
        return tf.TensorSpec(shape=narray_shape, dtype=narray_dtype)
    elif isinstance(data, dict):
        return {k: _inspect_spec(v) for k, v in data.items()}
    else:
        raise NoSupportError(
            f"inspect tensor spec does not support {dtype} type: {data}"
        )


def to_tf_dataset(dataset: Dataset, drop_index: bool = True) -> tf.data.Dataset:
    def _generator() -> t.Generator:
        for row in dataset:
            if drop_index:
                yield _transform(row.features)
            else:
                yield _transform(row.index), _transform(row.features)

    def _make_signature() -> t.Any:
        row = dataset.fetch_one()
        signature = []
        if not drop_index:
            signature.append(_inspect_spec(row.index))
            return _inspect_spec(row.index), _inspect_spec(row.features)
        return _inspect_spec(row.features)

    return tf.data.Dataset.from_generator(
        _generator, output_signature=_make_signature()
    )
