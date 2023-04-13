---
title: The `z-bench` Dataset
---

## The z-bench Dataset Description

- [Homepage](https://github.com/zhenbench/z-bench)

## The `z-bench` dataset Structure

### Data Fields

- `data` of type dict:
    - `prompt`: `str`
    - `task_type`: `str`
    - `ref_answer`: `str`
    - `gpt3d5`: `str`
    - `gpt4`: `str`

## Build `z-bench` sample Dataset locally

```shell
python3 dataset.py common
```

## Example

Output the `10`th record of the `z-bench` dataset.

```shell
python3 example.py common
```
