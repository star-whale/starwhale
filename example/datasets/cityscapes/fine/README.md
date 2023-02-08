---
title: The `cityscapes_fine` Dataset
---

## The Cityscapes Dataset (fine) Description

- [Homepage](https://www.cityscapes-dataset.com/examples/#fine-annotations)

## The `cityscapes_fine` dataset Structure

### Data Fields

- `data` of type `dict`:
    - `image`: of type `starwhale.Image`
    - `color_mask`: of type `starwhale.Image`
    - `instance_mask`: of type `starwhale.Image`
    - `label_mask`: of type `starwhale.Image`
    - `polygons`: of type `dict`
      - `objects`: `list`
        - `label`: the `label` in annotation file
        - `polygon` `starwhale.Polygon` starwhale viewable representation for `polygon` in annotation file
      - `imgHeight`: the `imgHeight` in annotation file
      - `imgWidth`: the `imgWidth` in annotation file

## Build `cityscapes_fine` Dataset locally

```shell
python3 dataset.py
```

## Example

Output the first 1 record of the `cityscapes_fine` dataset.

```shell
python3 example.py
```
