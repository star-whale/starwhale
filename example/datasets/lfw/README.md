---
title: The `lfw` Dataset
---

## The Labeled Faces in the Wild Dataset Description

- [Homepage](http://vis-www.cs.umass.edu/lfw/)

## The `lfw` dataset Structure

### Data Fields

- `data`: `starwhale.Image` loaded as bytes array
- `annotations` of type dict:
    - `identity`: the identity for the persion

## Build `lfw` Dataset locally

```shell
python3 dataset.py
```

## Example

Output the first 1 record of the `lfw` dataset.

```shell
python3 example.py
```
