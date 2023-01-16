---
title: Evaluate Hugging Face Transformer `textattack/albert-base-v2-ag-news` on AG News dataset
---

```bash
    git clone https://github.com/star-whale/starwhale.git
    cd starwhale/client
    python3 -m pip install -e .
    cd ../example/datasets/cifar100
    swcli dataset build . --name cifar100-test --handler dataset:iter_test_item
    cd ../../transformer/cifar100
    python3 -m pip install -r requirements.txt
    python3 download_model.py
    swcli -vvv model eval --dataset cifar100-test/version/latest .
```
