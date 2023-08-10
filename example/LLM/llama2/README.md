# 🦙 LLAMA2 meets Starwhale 🐋

Thanks to [llama2](https://github.com/facebookresearch/llama) Project. This example demonstrates how to conduct a Starwhale Evaluation LLAMA2.

We use the llama2 models for the example. The code base can work for the following scenarios:

1. Build Starwhale Model for base llama2 models.
2. Evaluate llama2 models.
3. Chat for llama2 models.

Current supported datasets:

- For evaluation:
  - mkqa
  - z_ben_common
  - vicuna
  - webqsp

Current supported models:

- [llama2-7b](https://cloud.starwhale.cn/projects/12/models/135)
- [llama2-7b-chat](https://cloud.starwhale.cn/projects/12/models/134)
- [llama2-13b](https://cloud.starwhale.cn/projects/12/models/136)
- [llama2-13b-chat](https://cloud.starwhale.cn/projects/12/models/137)
- [llama2-70b](https://cloud.starwhale.cn/projects/12/models/139)
- [llama2-70b-chat](https://cloud.starwhale.cn/projects/12/models/138)

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

### Use swcli command-line

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

### Use Python Script

Recommended to run the Python script in the activated llama1 runtime.


```bash
python build.py 7b
#python build.py 7b-chat
#python build.py 13b
#python build.py 13b-chat
#python build.py 70b
#python build.py 70b-chat
```

example output:

```bash
(starwhale) $ python build.py 13b
[2023-07-19 20:23:20.001987] 👾 verbosity: 3, log level: DEBUG
[2023-07-19 20:23:20.018352] 🚧 start to build model bundle...
[2023-07-19 20:23:20.019668] ❓ |WARN| refine resource[ResourceType.model] llama2-13b/version/inplhizg3sotnwhm35tnmx6etsbs7qal5beukirb failed: Can not find the exact match item inplhizg3sotnwhm35tnmx6etsbs7qal5beukirb, found: []
[2023-07-19 20:23:20.020764] 👷 uri local/project/self/model/llama2-13b/version/inplhizg3sotnwhm35tnmx6etsbs7qal5beukirb
[2023-07-19 20:23:20.021935] 🔈 |DEBUG| 🆕 version inplhizg3sot
[2023-07-19 20:23:20.022732] 📁 workdir: /home/liutianwei/.starwhale/.tmp/tmpdtn9g74k
[2023-07-19 20:23:20.023316] 🦚 copy source code files: /mnt/data/tianwei/code/starwhale/example/LLM/llama2 -> /home/liutianwei/.starwhale/.tmp/tmpdtn9g74k/src
[2023-07-19 20:23:20.023955] 🔈 |DEBUG| copy dir: /mnt/data/tianwei/code/starwhale/example/LLM/llama2 -> /home/liutianwei/.starwhale/.tmp/tmpdtn9g74k/src, excludes: ['*/.git/*', '', '*/adapter-7b/checkpoint*', '*/adapter-7b/runs', '*/base-13b/*', '*/adapter-13b/*', '*/base-
30b/*', '*/adapter-30b/*', '*/base-65b/*', '*/adapter-65b/*', '*/llama-2-7b/*', '*/llama-2-7b-chat/*', '*/llama-2-13b-chat/*', '*/llama-2-70b/*', '*/llama-2-70b-chat/*', '__pycache__/', '*.py', '*$py.class']
[2023-07-19 20:29:02.844505] 📁 source code files size: 24.25GB
[2023-07-19 20:29:02.846831] 🔈 |DEBUG| generating model serving config for local/project/self/model/llama2-13b/version/inplhizg3sotnwhm35tnmx6etsbs7qal5beukirb ...
[2023-07-19 20:29:03.071744] 🚀 generate jobs yaml from modules: ['evaluation'] , package rootdir: /mnt/data/tianwei/code/starwhale/example/LLM/llama2
[2023-07-19 20:29:58.218060] 💡 |INFO| 🧺 resource files size: 24.25GB
[2023-07-19 20:29:58.273417] 💯 finish gen resource @ /home/liutianwei/.starwhale/self/model/llama2-13b/in/inplhizg3sotnwhm35tnmx6etsbs7qal5beukirb.swmp
(starwhale) liutianwei@host005-bj01 ~/workdir/code/starwhale/example/LLM/llama2$ python build.py 13b-chat                                                                                                                                                      ✹ ✭example/llama2
[2023-07-19 20:32:58.733390] 👾 verbosity: 3, log level: DEBUG
[2023-07-19 20:32:58.749449] 🚧 start to build model bundle...
[2023-07-19 20:32:58.750717] ❓ |WARN| refine resource[ResourceType.model] llama2-13b-chat/version/tvypzoflrgbcly5qqnbtfxel4ethwzetgdqeiebs failed: Can not find the exact match item tvypzoflrgbcly5qqnbtfxel4ethwzetgdqeiebs, found: []
[2023-07-19 20:32:58.751809] 👷 uri local/project/self/model/llama2-13b-chat/version/tvypzoflrgbcly5qqnbtfxel4ethwzetgdqeiebs
[2023-07-19 20:32:58.753017] 🔈 |DEBUG| 🆕 version tvypzoflrgbc
[2023-07-19 20:32:58.753823] 📁 workdir: /home/liutianwei/.starwhale/.tmp/tmpfha_7hj0
[2023-07-19 20:32:58.754393] 🦚 copy source code files: /mnt/data/tianwei/code/starwhale/example/LLM/llama2 -> /home/liutianwei/.starwhale/.tmp/tmpfha_7hj0/src
[2023-07-19 20:32:58.755066] 🔈 |DEBUG| copy dir: /mnt/data/tianwei/code/starwhale/example/LLM/llama2 -> /home/liutianwei/.starwhale/.tmp/tmpfha_7hj0/src, excludes: ['*/.git/*', '', '*/adapter-7b/checkpoint*', '*/adapter-7b/runs', '*/base-13b/*', '*/adapter-13b/*', '*/base-
30b/*', '*/adapter-30b/*', '*/base-65b/*', '*/adapter-65b/*', '*/llama-2-7b/*', '*/llama-2-7b-chat/*', '*/llama-2-13b/*', '*/llama-2-70b/*', '*/llama-2-70b-chat/*', '__pycache__/', '*.py', '*$py.class']
[2023-07-19 20:38:37.510521] 📁 source code files size: 24.25GB
[2023-07-19 20:38:37.512795] 🔈 |DEBUG| generating model serving config for local/project/self/model/llama2-13b-chat/version/tvypzoflrgbcly5qqnbtfxel4ethwzetgdqeiebs ...
[2023-07-19 20:38:37.749889] 🚀 generate jobs yaml from modules: ['evaluation'] , package rootdir: /mnt/data/tianwei/code/starwhale/example/LLM/llama2
[2023-07-19 20:39:34.547602] 💡 |INFO| 🧺 resource files size: 24.25GB
[2023-07-19 20:39:34.642076] 💯 finish gen resource @ /home/liutianwei/.starwhale/self/model/llama2-13b-chat/tv/tvypzoflrgbcly5qqnbtfxel4ethwzetgdqeiebs.swmp
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