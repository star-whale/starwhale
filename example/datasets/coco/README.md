---
title: The `coco-raw` Dataset
---

## The COCO Dataset Description

- [Homepage](https://cocodataset.org/#home)

## The `coco-raw` dataset Structure

### Data Fields

- `data`: `starwhale.Image` loaded as bytes array
- `annotations` of type dict:
  - `mask`: `starwhale.Link` loaded as dict
    - `uri`: the path where the `mask` file sits
  - `segments_info`:  array of `segment_info`
    - `bbox_view`: `starwhale.BoundingBox` used by viewer
    - other original fields


## Build `coco-raw` Dataset locally

- download raw data

```shell
make download
```

- build `coco-raw` dataset

```shell
swcli dataset build . --name coco-raw --handler dataset:do_iter_item
```

## Example

Output the first 1 record of the `coco-raw` dataset.

```shell
python3 example.py
```
