---
title: The `stanford_cars` Dataset
---

## The Stanford Cars Dataset Description

- [Homepage](https://ai.stanford.edu/~jkrause/cars/car_dataset.html)

## The `stanford_cars` dataset Structure

### Data Fields

- `data`: `starwhale.Image` loaded as bytes array
- `annotations` of type dict:
    - `label`: the class for the image
    - `bbox`: `starwhale.BoundingBox` the bbox for the car
    - `test`: if it belongs to the test set

## Build `stanford_cars` Dataset locally

```shell
python3 dataset.py
```

## Example

Output the first 1 record of the `stanford_cars` dataset.

```shell
python3 example.py
```
