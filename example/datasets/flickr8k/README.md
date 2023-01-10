---
title: The `flickr8k` Dataset
---

## The Flicker 8K Dataset Description

- [Homepage](http://hockenmaier.cs.illinois.edu/8k-pictures.html)

## The `flickr8k` dataset Structure

### Data Fields

- `annotations` of type dict:
    - `image`: `starwhale.Image`
    - `labels`: the labels for the image

## Build `flickr8k` Dataset locally

```shell
python3 dataset.py
```

## Example

Output the first 1 record of the `flickr8k` dataset.

```shell
python3 example.py
```
