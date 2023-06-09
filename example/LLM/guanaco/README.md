# 🦙 BELLE meets Starwhale 🐋

Thanks to [QLoRA](https://github.com/artidoro/qlora) Project. This example demonstrates how to conduct a Starwhale Evaluation and Fine-tune for Guanaco.

## Build Starwhale Runtime

```bash
swcli runtime build
```

## Build Starwhale Model

```bash
python sw.py build 7b
# build 13b/33b/65b
# python sw.py build 13b
# python sw.py build 33b
# python sw.py build 65b
```

## Run Starwhale Model in the Standalone instance

```bash
# at the guanaco dir
swcli -vvvv model run -w . --dataset mkqa-mini -m sw

swcli model run --uri guanaco-7b --dataset mkqa-mini --runtime guanaco
```
