---
title: The `culane` Dataset
---

## The CULane Dataset Description

- [Homepage](https://xingangpan.github.io/projects/CULane.html)

## The `culane` dataset Structure

### Data Fields

- `data` of type dict:
    - `image`: `starwhale.Image`
    - `mask`: `starwhale.Image` the mask file of the lines
    - `lines`:  `list` of `starwhale.Line`

## Build `culane` Dataset locally

```shell
python3 dataset.py
```

## Example

Output the first 1 record of the `culane` dataset.

```shell
python3 example.py
```
