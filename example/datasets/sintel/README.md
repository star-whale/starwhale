---
title: The `sintel` Dataset
---

## The MPI Sintel FLow Dataset Description

- [Homepage](http://sintel.is.tue.mpg.de/)

## The `sintel` dataset Structure

### Data Fields

- `data` of type dict:
    - `frame0/albedo`: `starwhale.Image`
    - `frame0/clean`: `starwhale.Image`
    - `frame0/final`: `starwhale.Image`
    - `frame1/albedo`: `starwhale.Image`
    - `frame1/clean`: `starwhale.Image`
    - `frame1/final`: `starwhale.Image`
    - `flow_viz`: `starwhale.Image`
    - `flow_bin`: `starwhale.NumpyBinary`
    - `pix_invalid`: `starwhale.Image`
    - `pix_occlusions`: `starwhale.Image`

## Build `sintel` Dataset locally

```shell
python3 dataset.py
```

## Example

Output the `1024`th record of the `sintel` dataset.

```shell
python3 example.py
```
