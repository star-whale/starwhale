---
title: The `sbd` Dataset
---

## The Semantic Boundaries Dataset Description

- [Homepage](http://home.bharathh.info/pubs/codes/SBD/download.html)

## The `sbd` dataset Structure

### Data Fields

- `data`: `starwhale.Image` loaded as bytes array
- `annotations` of type dict:
    - `boundaries`: the boundaries for the objects of type `bytes`
    - `shape`: the shape for the image of type `tuple`

## Build `sbd` Dataset locally

```shell
python3 dataset.py
```

## Example

Output the `2008_000202` record of the `sbd` dataset.

```shell
python3 example.py
```
