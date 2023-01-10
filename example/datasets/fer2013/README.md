---
title: The `fer2013` Dataset
---

## The Facial Expression Recognition Dataset Description

- [Homepage](https://www.kaggle.com/c/challenges-in-representation-learning-facial-expression-recognition-challenge)

## The `fer2013` dataset Structure

### Data Fields

- `data` of type dict:
    - `image`: `starwhale.GrayscaleImage` with shape (48, 48)
    - `label`: the label for the image

## Build `fer2013` Dataset locally

```shell
python3 dataset.py
```

## Example

Output the first 1 record of the `fer2013` dataset.

```shell
python3 example.py
```
