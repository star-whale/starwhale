---
title: The `lfw` Dataset
---

## The Labeled Faces in the Wild Dataset Description

- [Homepage](http://vis-www.cs.umass.edu/lfw/)

## The `lfw` dataset Structure

### Data Fields

- `data` of type dict:
    - `image`: `starwhale.Image`
    - `identity`: the identity for the person

## Build `lfw` Dataset locally

```shell
python3 dataset.py
```

## Example

Output the first 1 record of the `lfw` dataset.

```shell
python3 example.py
```
