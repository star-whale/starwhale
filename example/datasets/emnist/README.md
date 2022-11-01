---
title: The `emnist-digits-test` Dataset
---

## The EMNIST Dataset Description

- [Homepage](https://www.westernsydney.edu.au/icns/reproducible_research/publication_support_materials/emnist)
- Citation: [EMNIST: an extension of MNIST to handwritten letters](http://arxiv.org/abs/1702.05373), Cohen, G., Afshar, S., Tapson, J., & van Schaik, A. (2017).

## The `emnist-digits-test` dataset Structure

### Data Fields

- `data`: `starwhale.GrayscaleImage` type, 28*28 Grayscale Image
- `annotations`:
  - label: int, 0-9

### Web viewer

https://cloud.starwhale.cn/projects/14/datasets/5/versions/22/files

## Use the `emnist-digits-test` Dataset

- Sign up with https://cloud.starwhale.cn
```shell
$ python3 -m pip install starwhale
$ swcli instance login https://cloud.starwhale.cn --token 'YOUR TOKEN' --alias saas
$ swcli dataset copy cloud://saas/project/datasets/emnist-digits-test/version/gvsggndcgfrdimjugjqwkzlgo4zwo4a local
$ python3 example.py
```


## Build `emnist-digits-test` Dataset locally

- download raw data

```shell
make download
```

- build `emnist-digits-test` dataset

```shell
swcli dataset build . --name emnist-digits-test --handler dataset:iter_digits_test_item
```

- build `emnist-digits-train` dataset

```shell
swcli dataset build . --name emnist-digits-train --handler dataset:iter_digits_train_item
```

## Example

Output the first 10 records of the `emnist-digits-test` dataset.

```shell
python3 example.py
```
