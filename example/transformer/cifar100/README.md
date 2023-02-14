---
title: Evaluate Hugging Face Transformer `textattack/albert-base-v2-ag-news` on AG News dataset
---

This example requires swcli version being above 0.4.0

```bash
$ python3 -m pip install starwhale>=0.4.0
$ cd starwhale/example/datasets/cifar100
$ swcli dataset build . --name cifar100-test --handler dataset:iter_test_item
$ cd ../../transformer/cifar100
$ python3 -m pip install -r requirements.txt
$ python3 download_model.py
$ swcli -vvv model eval --dataset cifar100-test/version/latest .
```
