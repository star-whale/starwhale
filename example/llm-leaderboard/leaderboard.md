# LLM LeaderBoard on Starwhale 🐋

Starwhale provides an open and easy-to-reproduce leaderboard for LLMs.

Current supported LLMs:

- Baichuan 7b/13b
- Qwen 7b/7b-chat
- llama2
  - llama2 7b/13b
  - llama2 7b-chat/13b-chat
  - llama2-7b-chinese/llama2-13b-chinese
  - llama2-7b-chinese-alpaca/llama2-13b-chinese-alpaca
- llama 7b/13b
- chatglm 6b
- chatglm2 6b
- aquila 7b/7b-chat
- mistral-7b-instruct
- mistral-8*7b-instruct

## Build Starwhale Runtime

```bash
swcli runtime build --yaml runtime.yaml

# Activate the runtime in the current shell
swcli runtime activate llm-leaderboard
```

## Commands for llm-leaderboard

### Build the supported LLMs to Starwhale Model

```bash
python src/main.py -vvv build --model baichuan-13b
python src/main.py -vvv build --model chatglm-6b --push https://cloud.starwhale.cn/project/starwhale:llm-leaderboard
python src/main.py -vvv build --model all --push https://cloud.starwhale.cn/project/starwhale:llm-leaderboard
```

### Submit evaluation tasks

For batch submit for server or cloud instance.

```bash
python src/main.py submit --model llama2-13b --model baichuan-13b --benchmark cmmlu --benchmark mmlu
```

### Run in the Standalone Instance

```bash
swcli model run --model llama2-13b --dataset cmmlu
swcli -vvvv model run --dataset cmmlu -w . -m src.evaluation --handler src.evaluation:predict_question
```
