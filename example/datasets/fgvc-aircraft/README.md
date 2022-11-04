---
title: The FGVC-Aircraft Dataset
---

## The FGVC-Aircraft Dataset Description

- [Homepage](https://www.robots.ox.ac.uk/~vgg/data/fgvc-aircraft/)
- Citation: [Fine-Grained Visual Classification of Aircraft](http://arxiv.org/abs/1306.5151), S. Maji, J. Kannala, E. Rahtu, M. Blaschko, A. Vedaldi, 2013

## The `fgvc-aircraft-family-test` dataset Structure

### Data Fields

- `data`: `starwhale.Image` type, JPEG Image:
  - `fp`: image bytes
  - `shape`: image shape (width, height)
- `annotations`:
  - `family`: String (such as 'Boeing 707')

### Web viewer

https://cloud.starwhale.cn/projects/14/datasets/6/versions/23/files

## Use the `fgvc-aircraft-family-test` Dataset

- Sign up with https://cloud.starwhale.cn

```shell
$ python3 -m pip install -r requirements.txt
$ swcli instance login https://cloud.starwhale.cn --token 'YOUR TOKEN' --alias saas
$ swcli dataset copy cloud://saas/project/datasets/fgvc-aircraft-family-test/version/mzsdizjxgnstiytdmu2tknbvomywq4a local
$ python3 example.py
```


## Build `fgvc-aircraft-family-test` Dataset locally

- download raw data

```shell
make download
```

- build `fgvc-aircraft-family-test` dataset

```shell
swcli dataset build . --name fgvc-aircraft-family-test --handler dataset:iter_family_test_item
```

- build `fgvc-aircraft-family-train` dataset

```shell
swcli dataset build . --name fgvc-aircraft-family-train --handler dataset:iter_family_train_item
```

## Example

Output the first 10 records of the `fgvc-aircraft-family-test` dataset.

```shell
python3 example.py
```
