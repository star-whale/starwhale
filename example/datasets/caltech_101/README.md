---
title: The `caltech-101` Dataset
---

## The Caltech 101 Dataset Description

- [Homepage](https://data.caltech.edu/records/mzrjq-6wc02)

## The `caltech-101` dataset Structure

### Data Fields

- `row.data`: `dict`
  - `image`: `starwhale.Image`
  - `label`: the label for the image
- `row.index` of type `str`:


## Build `caltech-101` Dataset locally

```shell
python3 dataset.py
```

## Example

Output the first 1 record of the `caltech-101` dataset.

```shell
python3 example.py
```
