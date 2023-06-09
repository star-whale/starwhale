---
title: Open Assistant Guanaco Dataset
---

## Description

- Homepage: https://huggingface.co/datasets/timdettmers/openassistant-guanaco

## Data Fields

- data:
  - text: string type

## Data Splits

- train: 9850 samples
- test: 518 samples

## Dataset Build

```shell
python3 dataset.py
```

## Dataset Head

```shell
swcli dataset head oasst-guanaco-train
swcli dataset head oasst-guanaco-eval
```
