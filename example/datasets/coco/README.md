---
title: The `coco-raw` Dataset
---

## The COCO Dataset Description

- [Homepage](https://cocodataset.org/#home)

## The `coco-raw` dataset Structure

### Data Fields

- `data` of type dict:
  - `mask`: `starwhale.Image`
  - `image`: `starwhale.Image`
  - `segments_info`:  array of `segment_info`
    - `bbox_view`: `starwhale.BoundingBox` used by viewer
    - other original fields


## Build `coco` Dataset locally

You can build `coco` dataset either using local data or remote data by `swcli` or Starwhale SDK

### Using `swcli` to build `coco` Dataset from local data

- download raw data

```shell
make download
```

- build `coco-raw` dataset

```shell
swcli dataset build . --name coco-raw --handler dataset:do_iter_item
```

### Using StarWhale SDK to build `coco` Dataset from remote data

```shell
python coco_builder.py
```

## Example

Output the first 1 record of the `coco-raw` dataset.

```shell
python3 example.py
```
