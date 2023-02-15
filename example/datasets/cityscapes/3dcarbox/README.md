---
title: The `cityscapes_3dcarbox` Dataset
---

## The Cityscapes 3D Dataset Description

- [Homepage](https://arxiv.org/abs/2006.07864)

## The `cityscapes_3dcarbox` dataset Structure

### Data Fields

- `data` of type `dict`:
    - `left_image_8bit`: of type `starwhale.Image`
    - `right_image_8bit`: of type `starwhale.Image`
    - `imgWidth`: original `imgWidth` in `xxx_gtBbox3d.json`
    - `imgHeight`: original `imgHeight` in `xxx_gtBbox3d.json`
    - `sensor`: original `sensor` in `xxx_gtBbox3d.json`
    - `objects`: of type `list`, extension for `objects` in `xxx_gtBbox3d.json`
      - `2d`: original `sensor` in `2d`
      - `2d_sw`: `starwhale.BoundingBox3D`
      - `3d`: original `3d` in `objects`
    - `ignore`: original `ignore` in `xxx_gtBbox3d.json`


## Build `cityscapes_3dcarbox` Dataset locally

```shell
python3 dataset.py
```

## Example

Output the first 1 record of the `cityscapes_3dcarbox` dataset.

```shell
python3 example.py
```
