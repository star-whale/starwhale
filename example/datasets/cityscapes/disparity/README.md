---
title: The `cityscapes_disparity` Dataset
---

## The Cityscapes Dataset (fine) Description

- [Homepage](https://www.cityscapes-dataset.com/)

## The `cityscapes_disparity` dataset Structure

### Info Fields

- `baseline` : the baseline for stereo cameras in cm.
- `homepage`: the homepage for the dataset

### Data Fields

- `data` of type `dict`:
    - `left_image_8bit`: of type `starwhale.Image`
    - `right_image_8bit`: of type `starwhale.Image`
    - `disparity_mask`: of type `starwhale.Image`

## Build `cityscapes_disparity` Dataset locally

```shell
python3 dataset.py
```

## Example

Output the first 1 record of the `cityscapes_disparity` dataset.

```shell
python3 example.py
```
