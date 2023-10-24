MusicGen Example Guides
======

[MusicGen](https://ai.honu.io/papers/musicgen/) a single Language Model (LM) that operates over several streams of compressed discrete music representation.

- 🏔️ Homepage:  ️<https://ai.honu.io/papers/musicgen/>
- 🌋 Github: <https://github.com/facebookresearch/audiocraft>️
- 🏕️ Size: small(300M), melody(1.5B), medium(1.5B), large(3.3B)

Login Starwhale Cloud
------

```bash
swcli instance login --token "${TOKEN}" --alias cloud-cn https://cloud.starwhale.cn/
```

Build Starwhale Runtime
------

```bash
swcli -vvv runtime build
swcli runtime cp musicgen https://cloud.starwhale.cn/project/starwhale:llm_text_to_audio
```

Build Starwhale Model
------

Model name choices: `melody`, `medium`, `small` and `large`.

```bash
python3 build.py ${model_name}

swcli runtime activate musicgen
python3 build.py small
swcli model cp musicgen-small https://cloud.starwhale.cn/project/starwhale:llm_text_to_audio
```

Run Starwhale Model
------

```bash
# use model src dir
swcli model run --workdir . --runtime musicgen --dataset musicgen-mini -m evaluation

# use model package
swcli model run --uri musicgen-small --runtime musicgen --dataset musicgen-mini
```
