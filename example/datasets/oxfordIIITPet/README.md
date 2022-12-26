---
title: The `oxfordIIITPet` Dataset
---

## The Oxford-IIIT Pet Dataset Description

- [Homepage](https://www.robots.ox.ac.uk/~vgg/data/pets/)

## The `oxfordIIITPet` dataset Structure

### Data Fields

- `data`: `starwhale.Image` loaded as bytes array
- `annotations` of type dict:
    - `mask`: `starwhale.Link` the mask file for the image
    - `pets` `annotation.object` in original xml
        - `name`: `annotation.object.name` in original xml
        - `bbox`: `starwhale.BoundingBox` starwhale viewable representation for `annotation.object.bndbox` in original
          xml
        - `pose`: `annotation.object.pose` in original xml
        - `truncated`: `annotation.object.truncated` in original xml
        - `occluded`: `annotation.object.occluded` in original xml
        - `difficult`: `annotation.object.difficult` in original xml
    - `segmented`: `segmented` in `list.txt`
    - `class_id`: `class_id` in `list.txt`
    - `species`: `species` in `list.txt`
    - `breed_id`: `breed_id` in `list.txt`

## Build `oxfordIIITPet` Dataset locally

```shell
python3 dataset.py
```

## Example

Output the first 1 record of the `oxfordIIITPet` dataset.

```shell
python3 example.py
```
