---
title: Image Classification Datasets
---

We use CIFAR-10 and CIFAR-100 datasets to evaluate image classification models.

## CIFAR Dataset Description

- Homepage: [https://www.cs.toronto.edu/~kriz/cifar.html](https://www.cs.toronto.edu/~kriz/cifar.html)
- Citation: [Learning Multiple Layers of Features from Tiny Images](https://www.cs.toronto.edu/~kriz/learning-features-2009-TR.pdf), Alex Krizhevsky, 2009.

## CIFAR Dataset Structure

### Data Fields

#### CIFAR-100

- features:
  - image: `starwhale.Image` type, 32*32 PNG Image, 3 channels
  - fine_label: int, 0-99
  - find_label_name: str, the class to which it belongs
  - coarse_label: int, 0-19
  - coarse_label_name: str, the superclass to which it belongs

#### CIFAR-10

- features:
  - image: `starwhale.Image` type, 32*32 PNG Image, 3 channels
  - label: int, 0-10

### Data Splits

- test: 500 images for 100 classes each, total 50000 records.
- train: 100 images for 100 classes each, total 10000 records.

## Build CIFAR datasets

### Build CIFAR-10 by swcli command

Only one-line command to build Starwhale Dataset from the Huggingface:

```bash
swcli dataset build -hf cifar10
```

### Build CIFAR-100 by Starwhale SDK

```bash
make download
python3 cifar100.py
```
