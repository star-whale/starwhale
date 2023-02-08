---
title: The `dtd` Dataset
---

## The Describable Textures Dataset Description

- [Homepage](https://www.robots.ox.ac.uk/~vgg/data/dtd/)

## The `dtd` dataset Structure

### Data Fields

- `data` of type dict:
    - `image`: `starwhale.Image`
    - `labels`: the labels for the image

## Build `dtd` Dataset locally

```shell
python3 dataset.py
```

## Example

Output the `banded/banded_0063.jpg` record of the `dtd` dataset.

```shell
python3 example.py
```
