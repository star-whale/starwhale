---
title: CIFAR100 Dataset
---

## CIFAR100 Dataset Description

- Homepage: [https://www.cs.toronto.edu/~kriz/cifar.html](https://www.cs.toronto.edu/~kriz/cifar.html)
- Citation: [Learning Multiple Layers of Features from Tiny Images](https://www.cs.toronto.edu/~kriz/learning-features-2009-TR.pdf), Alex Krizhevsky, 2009.

## CIFAR100 Dataset Structure

### Data Fields

- data: `starwhale.Image` type, 32*32 PNG Image, 3 channels
- annotations:
  - fine_label: int, 0-99
  - find_label_name: str, the class to which it belongs
  - coarse_label: int, 0-19
  - coarse_label_name: str, the superclass to which it belongs

### Data Splits

- test: 500 images for 100 classes each, total 50000 records.
- train: 100 images for 100 classes each, total 10000 records.

## Build CIFAR100 Dataset

- download raw data

```shell
make download
```

- build train dataset

```shell
swcli dataset build . --name cifar100-train --handler dataset:iter_train_item
```

- build test dataset

```shell
swcli dataset build . --name cifar100-test --handler dataset:iter_test_item
```

## Example

Output the first 10 records of the cifar100-test dataset.

```shell
python example.py
```
