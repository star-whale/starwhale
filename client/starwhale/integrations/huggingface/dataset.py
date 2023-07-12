from __future__ import annotations

import io
import os
import typing as t

from starwhale.utils.error import NoSupportError

try:
    import datasets as hf_datasets
except ImportError:  # pragma: no cover
    raise ImportError("Please install huggingface/datasets with `pip install datasets`")

from starwhale.utils import console
from starwhale.utils.fs import DIGEST_SIZE
from starwhale.core.dataset.type import Text, Audio, Image, Binary, MIMEType


def _transform_to_starwhale(data: t.Any, feature: t.Any) -> t.Any:
    if isinstance(feature, hf_datasets.Value):
        if feature.dtype in ("large_string", "string"):
            if len(data) > DIGEST_SIZE:
                return Text(content=data)
            else:
                return data
        elif feature.dtype in ("large_binary", "binary"):
            if len(data) > DIGEST_SIZE:
                return Binary(fp=data)
            else:
                return data
        else:
            return data
    elif isinstance(feature, hf_datasets.Audio):
        if data.get("path") is not None:
            return Audio(fp=data["path"])
        elif data.get("array") is not None:
            import soundfile

            img_io = io.BytesIO()
            # TODO: support format by raw array
            soundfile.write(img_io, data["array"], data["sampling_rate"], format="wav")
            return Audio(fp=img_io.getvalue(), mime_type=MIMEType.WAV)
        else:
            raise ValueError(f"Unknown audio data fields: {data}")
    elif isinstance(feature, hf_datasets.Image):
        from PIL import Image as PILImage

        if isinstance(data, PILImage.Image):
            img_io = io.BytesIO()
            data.save(img_io, format=data.format or "PNG")
            img_fp = img_io.getvalue()

            try:
                data_mimetype = data.get_format_mimetype()
                mime_type = MIMEType(data_mimetype)
            except (ValueError, AttributeError):
                mime_type = MIMEType.PNG
            return Image(
                fp=img_fp,
                shape=(data.height, data.width, len(data.getbands())),
                mime_type=mime_type,
            )
        else:
            raise NoSupportError(
                f"Unknown image type: {type(data)}, current only support PIL.Image.Image"
            )
    elif isinstance(feature, hf_datasets.ClassLabel):
        # TODO: graceful handle classLabel, store it into Starwhale.ClassLabel type
        return data
    elif isinstance(feature, list):
        # list supports mixed type, but Starwhale only supports same type
        return [_transform_to_starwhale(d, feature[i]) for i, d in enumerate(data)]
    elif isinstance(feature, hf_datasets.Sequence):
        inner_feature = feature.feature
        if isinstance(inner_feature, dict):
            return {
                k: _transform_to_starwhale(v, inner_feature[k]) for k, v in data.items()
            }
        else:
            # TODO: need to handle complex Sequence type
            return [_transform_to_starwhale(d, inner_feature) for d in data]
    elif isinstance(feature, dict):
        return {k: _transform_to_starwhale(v, feature[k]) for k, v in data.items()}
    else:
        # Other unnecessary huggingface types:
        #   - Array2D, Array3D, Array4D and Array5D: multi-dimension array
        #   - Translation, TranslationVariableLanguages: dict and dict[str, str|list[str]] type
        console.debug(f"skip transform feature: {feature}")
        return data


def _iter_dataset(ds: hf_datasets.Dataset) -> t.Iterator[t.Tuple[int, t.Dict]]:
    for i in range(len(ds)):
        item = {}
        for k, v in ds[i].items():
            feature = ds.features[k]
            item[k] = _transform_to_starwhale(v, feature)
            # TODO: support inner ClassLabel
            if isinstance(feature, hf_datasets.ClassLabel):
                item[f"{k}__classlabel__"] = feature.names[v]
        yield i, item


def iter_dataset(
    repo: str,
    subset: str | None = None,
    split: str | None = None,
    revision: str = "main",
    cache: bool = True,
) -> t.Iterator[t.Tuple[str, t.Dict]]:
    download_mode = (
        hf_datasets.DownloadMode.REUSE_DATASET_IF_EXISTS
        if cache
        else hf_datasets.DownloadMode.FORCE_REDOWNLOAD
    )

    ds = hf_datasets.load_dataset(
        repo,
        subset,
        split=split,
        revision=revision,
        num_proc=min(8, os.cpu_count() or 8),
        download_mode=download_mode,
    )

    if isinstance(ds, hf_datasets.DatasetDict):
        for _split, _ds in ds.items():
            for _key, _data in _iter_dataset(_ds):
                yield f"{_split}/{_key}", _data
    elif isinstance(ds, hf_datasets.Dataset):
        for _key, _data in _iter_dataset(ds):
            if split:
                _s_key = f"{split}/{_key}"
            else:
                _s_key = str(_key)
            yield _s_key, _data
    else:
        raise RuntimeError(f"Unknown dataset type: {type(ds)}")
