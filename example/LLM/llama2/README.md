# 🦙 LLAMA2 meets Starwhale 🐋

Thanks to [llama2](https://github.com/facebookresearch/llama) Project. This example demonstrates how to conduct a Starwhale Evaluation LLAMA2.

We use the llama2-7b-chat model for the example. The code base can work for the following scenarios:

1. Build Starwhale Model for base llama2 model.
2. Evaluate llama2 model.
3. Chat for llama2 model.

Current supports datasets:

- For evaluation:
  - mkqa
  - z_ben_common
  - vicuna
  - webqsp

## Build Starwhale Runtime

```bash
swcli runtime build --yaml runtime.yaml
```

example output:

```bash
$ swcli runtime build --yaml runtime.yaml

🚧 start to build runtime bundle...
👷 uri local/project/self/runtime/llama2/version/s4kudnma5ce7lbxudagr43rggrzigz3cfdrbmex4
🐦 runtime will ignore pypi editable package
👽 try to lock environment dependencies to /mnt/data/tianwei/code/starwhale/example/LLM/llama2 ...
🦋 lock dependencies at mode venv
🍼 install runtime.yaml dependencies @ /mnt/data/tianwei/code/starwhale/example/LLM/llama2/.starwhale/venv for lock...
🐱 use /mnt/data/tianwei/code/starwhale/example/LLM/llama2/.starwhale/venv/bin/python3 to freeze requirements...
🐭 dump lock file: /mnt/data/tianwei/code/starwhale/example/LLM/llama2/.starwhale/lock/requirements-sw-lock.txt
📁 workdir: /home/liutianwei/.starwhale/self/workdir/runtime/llama2/s4/s4kudnma5ce7lbxudagr43rggrzigz3cfdrbmex4
🐝 dump environment info...
🥯 dump dependencies info...
🌈 runtime uses builtin docker image: docker-registry.starwhale.cn/star-whale/starwhale:0.5.6-cuda11.7  🌈
🦋 .swrt bundle:/home/liutianwei/.starwhale/self/runtime/llama2/s4/s4kudnma5ce7lbxudagr43rggrzigz3cfdrbmex4.swrt
  10 out of 10 steps finished ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━ 100% 0:00:00 0:00:00
```

## Build Starwhale Model for the llama2

```bash
swcli -vvv model build -m evaluation . --name llama2-7b-chat
swcli -vvv model build -m evaluation . --name llama2-7b-chat --runtime llama2
```

example output:

```bash
$ swcli -vvv model build -m evaluation . --name llama2-7b-chat
[2023-07-19 16:11:41.616914] 👾 verbosity: 3, log level: DEBUG
[2023-07-19 16:11:41.621373] 🚧 start to build model bundle...
[2023-07-19 16:11:41.623940] ❓ |WARN| refine resource[ResourceType.model] llama2-7b-chat/version/f7sibscdttmw7wbeqnogakqljqtfjbyq65fmheav failed: Can not find the exact match item f7sibscdttmw7wbeqnogakqljqtfjbyq65fmheav, found: []
[2023-07-19 16:11:41.625017] 👷 uri local/project/self/model/llama2-7b-chat/version/f7sibscdttmw7wbeqnogakqljqtfjbyq65fmheav
[2023-07-19 16:11:41.627441] 🔈 |DEBUG| 🆕 version f7sibscdttmw
[2023-07-19 16:11:41.628249] 📁 workdir: /home/liutianwei/.starwhale/.tmp/tmpik4d1pp4
[2023-07-19 16:11:41.628835] 🦚 copy source code files: . -> /home/liutianwei/.starwhale/.tmp/tmpik4d1pp4/src
[2023-07-19 16:11:41.629737] 🔈 |DEBUG| copy dir: . -> /home/liutianwei/.starwhale/.tmp/tmpik4d1pp4/src, excludes: ['*/.git/*', '', '*/adapter-7b/checkpoint*', '*/adapter-7b/runs', '*/base-13b/*', '*/adapter-13b/*', '*/base-30b/*', '*/adapter-30b/*', '*/base-65b/*', '*/adapter-65b/*', '__pycache__/', '*.py', '*$py.class']
[2023-07-19 16:14:04.932350] 📁 source code files size: 12.55GB
[2023-07-19 16:14:04.934443] 🔈 |DEBUG| generating model serving config for local/project/self/model/llama2-7b-chat/version/f7sibscdttmw7wbeqnogakqljqtfjbyq65fmheav ...
[2023-07-19 16:14:20.768270] 🚀 generate jobs yaml from modules: ('evaluation',) , package rootdir: .
[2023-07-19 16:16:22.525950] 💡 |INFO| 🧺 resource files size: 12.55GB
[2023-07-19 16:16:22.555182] 💯 finish gen resource @ /home/liutianwei/.starwhale/self/model/llama2-7b-chat/f7/f7sibscdttmw7wbeqnogakqljqtfjbyq65fmheav.swmp
```

## Run Starwhale Model for evaluation in the Standalone instance

```bash
swcli -vvv model run -w . -m evaluation --handler evaluation:copilot_predict --dataset vicuna-mini
```

## Run Starwhale Model for chatting in the Standalone instance

```bash
swcli -vvv model run -w . -m evaluation --handler evaluation:chatbot
```

visit http://localhost:7860 for chatting.


