---
title: Evaluate Hugging Face Transformer `textattack/albert-base-v2-ag-news` on AG News dataset
---

This example requires swcli version being above 0.4.0

```bash
$ python3 -m pip install starwhale>=0.4.0
$ git clone https://github.com/star-whale/starwhale.git
$ cd starwhale/example/text_cls_AG_NEWS
$ swcli dataset build .
$ cd ../transformer/ag_news/code
$ python3 download_model.py
$ cd ..
$ python3 -m pip install -r requirements-sw-lock.txt
$ swcli -vvv model eval  --dataset ag_news/version/latest .
```
