---
title: The `svhn` Dataset
---

## The Street View House Numbers Dataset Description

- [Homepage](http://ufldl.stanford.edu/housenumbers/)

## The `svhn` dataset Structure

### Data Fields

- `data`: `starwhale.Image` loaded as bytes array
- `annotations` of type dict:
    - `numbers`: list
      - `bbox`: `starwhale.BoundingBox`
      - `label`: `np.float` the label for the number in the bbox

## Build `svhn` Dataset locally

```shell
python3 dataset.py
```

## Example

Output the first 1 record of the `svhn` dataset.

```shell
python3 example.py
```
