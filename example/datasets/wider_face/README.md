---
title: The `wider_face` Dataset
---

## The WIDER FACE Dataset Description

- [Homepage](http://shuoyang1213.me/WIDERFACE/)

## The `wider_face` dataset Structure

### Data Fields

- `data`: `starwhale.Image` loaded as bytes array
- `annotations` of type dict:
    - `faces`: list
      - `bbox`: `starwhale.BoundingBox` starwhale viewable representation for bounding box in `wider_face_train_bbx_gt.txt`
      - `blur`: blur value in `wider_face_train_bbx_gt.txt`
      - `expression`: expression value in `wider_face_train_bbx_gt.txt`
      - `illumination`: illumination value in `wider_face_train_bbx_gt.txt`
      - `occlusion`: occlusion value in `wider_face_train_bbx_gt.txt`
      - `pose`: pose value in `wider_face_train_bbx_gt.txt`
      - `invalid`: invalid value in `wider_face_train_bbx_gt.txt`

## Build `wider_face` Dataset locally

```shell
python3 dataset.py
```

## Example

Output the `0--Parade/0_Parade_marchingband_1_205.jpg` record of the `wider_face` dataset.

```shell
python3 example.py
```
