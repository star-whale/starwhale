# 🦙 LLAMA meets Starwhale 🐋

Thanks to [LLAMA](https://github.com/facebookresearch/llama) and [QLoRA](https://github.com/artidoro/qlora) Project. This example demonstrates how to conduct a Starwhale Evaluation and Fine-tune(qlora) for LLAMA.

We use the base model from [huggyllama](https://huggingface.co/huggyllama). The example will support 7b, 13b, 30b and 65b llama models.

The code base can work for the following scenarios:

1. Build Starwhale Model for base llama model.
2. Evaluate base llama model.
3. Fine-tune base llama model with QLoRA and build a new Starwhale Model Package.
4. Evaluate fine-tuned llama model.
5. Fine-tune fine-tuned llama model and build a new Starwhale Model Package.
6. Evaluate and fine-tune the step 5 output.

Current supports datasets:

- For evaluation:
  - mkqa
  - z_ben_common
  - vicuna
  - webqsp

- For finetune:
  - openassistant


## Build Starwhale Runtime

```bash
swcli runtime build --yaml runtime.yaml
```

## Build Starwhale Model  for the original LLAMA

Download the original llama model files and build a Starwhale Model Package.

```bash
python build.py 7b
# build 13b/30b/65b
python build.py 13b
python build.py 30b
python build.py 65b
```

## Run Starwhale Model for evaluation in the Standalone instance

```bash
# at llama dir
swcli -vvvv model run -w . --dataset mkqa-mini -m evaluation
```

## Run Starwhale Model for finetune(qlora) in the Standalone instance

finetune llama base model with [ossat-guanaco](https://huggingface.co/datasets/timdettmers/openassistant-guanaco) dataset and QLoRA method.

```bash
# at llama-qlora dir
swcli -vvvv model run -w . --dataset oasst-guanaco-train --dataset oasst-guanaco-eval -m finetune
```
