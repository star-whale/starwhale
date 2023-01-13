---
title: Evaluate Hugging Face Transformer `textattack/albert-base-v2-ag-news` on AG News dataset
---

```bash
    git clone https://github.com/star-whale/starwhale.git
    cd starwhale/client
    python3 -m pip install -e .
    cd ../example/text_cls_AG_NEWS
    swcli dataset build .
    cd ../transformer/ag_news/code
    python3 download_model.py
    cd ..
    python3 -m pip install -r requirements-sw-lock.txt
    swcli -vvv model eval  --dataset ag_news/version/latest .
```
