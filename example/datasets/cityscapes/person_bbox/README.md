---
title: The `city_person` Dataset
---

## The CityPersons Dataset Description

- [Homepage](https://openaccess.thecvf.com/content_cvpr_2017/papers/Zhang_CityPersons_A_Diverse_CVPR_2017_paper.pdf)

## The `city_person` dataset Structure

### Data Fields

- `data` of type `dict`:
    - `image`: `starwhale.Image`
    - `objects`: of type `list`, `objects` in annotation file
      - `bbox`: `starwhale.BoundingBox` starwhale viewable representation for `bbox` in annotation file
      - `bboxVis`: `starwhale.BoundingBox` starwhale viewable representation for `bboxVis` in annotation file
      - `instanceId`: `instanceId` in annotation file
      - `label`: `label` in annotation file

## Build `city_person` Dataset locally

```shell
python3 dataset.py
```

## Example

Output the first 1 record of the `city_person` dataset.

```shell
python3 example.py
```
