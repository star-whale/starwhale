---
title: The `eurosat` Dataset
---

## The EuroSAT Dataset Description

- [Homepage](https://github.com/phelber/eurosat)

## The `eurosat` dataset Structure

### Data Fields

- `data` of type dict:
    - `image`: `starwhale.Image`
    - `label`: the label for the image

## Build `eurosat` Dataset locally

```shell
python3 dataset.py
```

## Example

Output the first 1 record of the `eurosat` dataset.

```shell
python3 example.py
```
