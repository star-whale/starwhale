from __future__ import annotations

import io
import os
import typing as t

from starwhale.utils.error import NoSupportError

try:
    import datasets as hf_datasets
except ImportError:  # pragma: no cover
    raise ImportError(
        "Please install huggingface/datasets with `pip install datasets`"
    ) from None

from starwhale.utils import console
from starwhale.base.data_type import Audio, Image, MIMEType


def _transform_to_starwhale(data: t.Any, feature: t.Any) -> t.Any:
    if isinstance(feature, hf_datasets.Value):
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
        # Huggingface list feature should be provided with a single sub-feature as an example of the feature type hosted in this list.
        # ref: https://huggingface.co/docs/datasets/package_reference/main_classes#datasets.Features
        return [_transform_to_starwhale(d, feature[0]) for d in data]
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


def _iter_dataset(
    ds: hf_datasets.Dataset, subset: str, split: str | None, add_info: bool = True
) -> t.Iterator[t.Tuple[int, t.Dict]]:
    for i in range(len(ds)):
        item = {}
        for k, v in ds[i].items():
            feature = ds.features[k]
            item[k] = _transform_to_starwhale(v, feature)
            # TODO: support inner ClassLabel
            if isinstance(feature, hf_datasets.ClassLabel):
                item[f"{k}__classlabel__"] = feature.names[v]
        if add_info:
            if "_hf_subset" in item or "_hf_split" in item:
                raise RuntimeError(
                    f"Dataset {subset} has already contains _hf_subset or _hf_split field, {item.keys()}"
                )
            item["_hf_subset"] = subset
            item["_hf_split"] = split

        yield i, item


def iter_dataset(
    repo: str,
    subsets: t.List[str] | None = None,
    split: str | None = None,
    revision: str = "main",
    cache: bool = True,
    add_info: bool = True,
) -> t.Iterator[t.Tuple[str, t.Dict]]:
    download_mode = (
        hf_datasets.DownloadMode.REUSE_DATASET_IF_EXISTS
        if cache
        else hf_datasets.DownloadMode.FORCE_REDOWNLOAD
    )
    download_config = hf_datasets.DownloadConfig(
        max_retries=10,
        num_proc=min(8, os.cpu_count() or 8),
    )

    if not subsets:
        subsets = hf_datasets.get_dataset_config_names(
            repo,
            revision=revision,
            download_mode=download_mode,
            download_config=download_config,
        )

    if not subsets:
        raise RuntimeError(f"Dataset {repo} has no any valid config names")

    for subset in subsets:
        ds = hf_datasets.load_dataset(
            repo,
            subset,
            split=split,
            revision=revision,
            download_mode=download_mode,
            download_config=download_config,
        )

        if isinstance(ds, hf_datasets.DatasetDict):
            for _ds_split, _ds in ds.items():
                for _key, _data in _iter_dataset(
                    _ds, subset, _ds_split, add_info=add_info
                ):
                    yield f"{subset}/{_ds_split}/{_key}", _data
        elif isinstance(ds, hf_datasets.Dataset):
            for _key, _data in _iter_dataset(ds, subset, split, add_info=add_info):
                if split:
                    _s_key = f"{subset}/{split}/{_key}"
                else:
                    _s_key = f"{subset}/{_key}"
                yield _s_key, _data
        else:
            raise RuntimeError(f"Unknown dataset type: {type(ds)}")
