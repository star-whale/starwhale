Baichuan2 Finetune with Starwhale
======

- 🍬 Parameters: 7b
- 🔆 Github: https://github.com/baichuan-inc/Baichuan2
- 🥦 Author: Baichuan Inc.
- 📝 License: baichuan
- 🐱 Starwhale Example: https://github.com/star-whale/starwhale/tree/main/example/llm-finetune/models/baichuan2
- 🌽 Introduction: Baichuan 2 is the new generation of large-scale open-source language models launched by Baichuan Intelligence inc..It is trained on a high-quality corpus with 2.6 trillion tokens and has achieved the best performance in authoritative Chinese and English benchmarks of the same size.Baichuan2-7b-chat is chat model of Baichuan 2, which contains 7 billion parameters.

In this example, we will use Baichuan2-7b-chat as the base model to finetune and evaluate.

- Evaluate baichuan2-7b-chat model.
- Provide baichuan2-7b-chat multi-turn chat online evaluation.
- Fine-tune baichuan2-7b-chat model with belle-multiturn-chat dataset.
- Evaluate the fine-tuned model.
- Provide the fine-tuned model multi-turn chat online evaluation.
- Fine-tune fine-tuned baichuan2-7n-chat model.

Because of 4bit quantization technical, the single T4/A10/A100 gpu card is ok for evaluation and finetune.

Build Starwhale Model
------

```bash
python3 build.py
```

Run Online Evaluation in the Standalone instance
------

```bash
# for source code
swcli model run -w . -m evaluation --handler evaluation:chatbot

# for model package with runtime
swcli model run --uri baichuan2-7b-chat --handler evaluation:chatbot --runtime llm-finetune
```

Run Starwhale Model for evaluation in the Standalone instance
------

```bash
swcli dataset cp https://cloud.starwhale.cn/projects/401/datasets/161/versions/223/ .
swcli -vvv model run -w . -m evaluation --handler evaluation:copilot_predict --dataset z-bench-common --dataset-head 3
```

Finetune base model
------

```bash
# build finetune dataset from baichuan2
swcli dataset build --json https://raw.githubusercontent.com/baichuan-inc/Baichuan2/main/fine-tune/data/belle_chat_ramdon_10k.json --name belle_chat_random_10k

swcli -vvv model run -w . -m finetune --dataset belle_chat_random_10k --handler finetune:lora_finetune
```

