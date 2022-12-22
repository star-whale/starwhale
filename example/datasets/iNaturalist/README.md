---
title: The `iNaturalist-val` Dataset
---

## The iNaturalist Dataset Description

- [Homepage](https://github.com/visipedia/inat_comp/tree/master/2021)

## The `iNaturalist-val` dataset `str`ucture

### Data Fields

- `data`: `starwhale.Image` loaded as bytes array
- `annotations` of type dict:
    - `latitude`: `float`,
    - `longitude`: `float`,
    - `name` : `str` category name,
    - `common_name` : `str`,
    - `supercategory` : `str`,
    - `kingdom` : `str`,
    - `phylum` : `str`,
    - `class` : `str`,
    - `order` : `str`,
    - `family` : `str`,
    - `genus` : `str`,
    - `specific_epithet` : `str`,
    - `image_dir_name` : `str`,

## Build `iNaturalist-val` Dataset locally

```shell
python3 dataset.py
```

## Example

Output the first 1 record of the `iNaturalist-val` dataset.

```shell
python3 example.py
```
