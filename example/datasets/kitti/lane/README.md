---
title: The `kitti-object-birds` Dataset
---

## The KITTI Dataset Bird's Eye View Evaluation 2017 Description

- [Homepage](https://www.cvlibs.net/datasets/kitti/eval_object.php?obj_benchmark=bev)

## The `kitti-object-birds` dataset Structure


### Data Fields
- `image_2`: `starwhale.Image`
- `image_3`: `starwhale.Image`
- `label_2`: `starwhale.Text`
- `velodyne`: `starwhale.Binary`

## Build `kitti-object-birds` Dataset locally

```shell
python3 dataset.py
```

## Example

Output the first 1 record of the `kitti-object-birds` dataset.

```shell
python3 example.py
```
