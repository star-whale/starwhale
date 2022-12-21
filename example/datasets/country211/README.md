---
title: The `country211` Dataset
---

## The Country 211 Dataset Description

- [Homepage](https://github.com/openai/CLIP/blob/main/data/country211.md)

## The `country211` dataset Structure

### Data Fields

- `data`: `starwhale.Image` loaded as bytes array
- `annotations` of type dict:
    - `label`: the label for the image

## Build `country211` Dataset locally

```shell
python3 dataset.py
```

## Example

Output the first 1 record of the `country211` dataset.

```shell
python3 example.py
```
