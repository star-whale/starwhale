---
title: The `culane` Dataset
---

## The CULane Dataset Description

- [Homepage](https://xingangpan.github.io/projects/CULane.html)

## The `culane` dataset Structure

### Data Fields

- `data`: `starwhale.Image` loaded as bytes array
- `annotations` of type dict:
    - `mask`: `starwhale.Link` the mask file of the lines
    - `lines`:  array of lines which contain line points

## Build `culane` Dataset locally

```shell
python3 dataset.py
```

## Example

Output the first 1 record of the `culane` dataset.

```shell
python3 example.py
```
