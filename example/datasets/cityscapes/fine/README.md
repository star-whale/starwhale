---
title: The `cityscapes_fine` Dataset
---

## The Cityscapes Dataset (fine) Description

- [Homepage](https://www.cityscapes-dataset.com/examples/#fine-annotations)

## The `cityscapes_fine` dataset Structure

### Data Fields

- `data`: `starwhale.Image` loaded as bytes array
- `annotations` of type `dict`:
    - `color_mask`: of type `starwhale.Link`
    - `instance_mask`: of type `starwhale.Link`
    - `label_mask`: of type `starwhale.Link`
    - `polygons`: of type `starwhale.Link`
      - `objects`: `starwhale.Polygon` starwhale viewable representation for `polygon` in annotation file
      - `imgHeight`: `imgHeight` in annotation file
      - `imgWidth`: `imgWidth` in annotation file

## Build `cityscapes_fine` Dataset locally

```shell
python3 dataset.py
```

## Example

Output the first 1 record of the `cityscapes_fine` dataset.

```shell
python3 example.py
```
