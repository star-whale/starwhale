---
title: The `gtsrb` Dataset
---

## The German Traffic Sign Recognition Benchmark Dataset Description

- [Homepage](https://benchmark.ini.rub.de/gtsrb_news.html)

## The `gtsrb` dataset Structure

### Data Fields

- `data` of type dict:
    - `image`: `starwhale.Image`
    - `bbox`: `starwhale.BoundingBox` the bbox for the sign
    - `class`: the class for the sign

## Build `gtsrb` Dataset locally

```shell
python3 dataset.py
```

## Example

Output the first 1 record of the `gtsrb` dataset.

```shell
python3 example.py
```
