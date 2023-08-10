# LLM LeaderBoard on Starwhale 🐋

Starwhale provides an open and easy-to-reproduce leaderboard for LLMs.

## Build Starwhale Runtime

```bash
swcli runtime build --yaml runtime.yaml

# Activate the runtime in the current shell
swcli runtime activate llm-leaderboard
```

## Commands for llm-leaderboard

### Build the supported LLMs to Starwhale Model

```bash
python src/main.py build --model baichuan-13b
```

### Get Starwhale LLM LeaderBoard

Summarize evaluation results from [Starwhale Cloud](https://cloud.starwhale.cn) and generate a LLMs LeaderBoard in the terminal.

```bash
python src/main.py leaderboard
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
