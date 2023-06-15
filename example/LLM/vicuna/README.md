# 🦙 Vicuna meets Starwhale 🐋

Thanks to [Vicuna](https://github.com/lm-sys/FastChat) Project. This example demonstrates how to conduct a Starwhale Evaluation for Vicuna 7B/13B models.

The codebase achieves the following goals:

1. Building Starwhale Model Package for Vicuna by one-line command
2. Evaluating Vicuna model package from some Starwhale datasets.
3. Online-Evaluating Vicuna model package by user web input interface.

Current supports datasets:

- For evaluation:
  - mkqa
  - z_ben_common
  - vicuna

## Build Starwhale Runtime

```bash
swcli runtime build --yaml runtime.yaml
```

## Build Starwhale Model

Download the original llama model files, merge vicuna delta patch and build a Starwhale Model Package.

```bash
python build.py 7b
python build.py 13b
```

## Run Starwhale Model for evaluation in the Standalone instance

```bash
# at llama dir
swcli -vvvv model run -w . --dataset mkqa-mini -m evaluation
```

## Run Starwhale Model for online evaluation in the Standalone instance

```bash
# at llama dir
swcli -vvvv model serve -w . -m evaluation
```