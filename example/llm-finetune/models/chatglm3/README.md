ChatGLM3 Finetune with  Starwhale
======

- 🍬 Parameters: 6b
- 🔆 Github: https://github.com/THUDM/ChatGLM3
- 🥦 Author: THUDM.
- 📝 License: Unknown
- 🐱 Starwhale Example: https://github.com/star-whale/starwhale/tree/main/example/llm-finetune/models/chatglm3
- 🌽 Introduction: ChatGLM3 is a new generation of pre-trained dialogue models jointly released by Zhipu AI and Tsinghua KEG. ChatGLM3-6B is the open-source model in the ChatGLM3 series, maintaining many excellent features of the first two generations such as smooth dialogue and low deployment threshold.

In this example, we use 4bit quantization to reduce gpu memory usage, the single T4/A10/A100 gpu card is ok for evaluation and finetune.

Build Starwhale Model
------

```bash
python3 download.py
swcli model build .
```

Run Online Evaluation in the Standalone instance
------

```bash
# for source code
swcli -vvv model serve --workdir . --host 0.0.0.0 --port 10878

# for model package with runtime
swcli -vvv model serve --uri chatglm3-6b --host 0.0.0.0 --port 10878 --runtime llm-finetune
```

Run Starwhale Model for evaluation in the Standalone instance
------

```bash
# download evaluation dataset
swcli dataset cp https://cloud.starwhale.cn/projects/401/datasets/161/versions/223/ .

# for source code
swcli -vvv model run -w . -m evaluation --handler evaluation:copilot_predict --dataset z-bench-common --dataset-head 3

# for model package
swcli -vvv model run --uri chatglm3-6b --handler evaluation:copilot_predict --dataset z-bench-common --dataset-head 3 --runtime llm-finetune
```


Finetune base model
------

```bash
# build finetune dataset from baichuan2
swcli dataset build --json https://raw.githubusercontent.com/baichuan-inc/Baichuan2/main/fine-tune/data/belle_chat_ramdon_10k.json --name belle_chat_random_10k

# for source code
swcli -vvv model run -w . -m finetune --dataset belle_chat_random_10k --handler finetune:p_tuning_v2_finetune

# for model package
swcli -vvv model run -u chatglm3-6b --dataset belle_chat_random_10k --handler finetune:p_tuning_v2_finetune --runtime llm-finetune
```
