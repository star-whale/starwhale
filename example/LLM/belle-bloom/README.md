# 🦙 BELLE meets Starwhale 🐋

Thanks to [BELLE](https://github.com/LianjiaTech/BELLE) Project. This example demonstrates how to conduct a Starwhale Evaluation on BELLE without modifying any source code.

## Build Starwhale Runtime

```bash
swcli runtime build --yaml runtime.yaml
```

## Build Starwhale Model

```bash
python sw.py build bloom-4bit
```

![model build](https://github.com/star-whale/starwhale/assets/590748/63249227-34eb-4331-9029-8789cb92e7c8)

## Run Starwhale Evaluation

In advance, please copy mkqa dataset from the cloud instance.

```bash
swcli model run --workdir . --dataset mkqa --runtime belle --forbid-snapshot
swcli model run --uri belle-bloom-4bit --dataset mkqa --runtime belle
```

![model run](https://github.com/star-whale/starwhale/assets/590748/a59fe0af-abd4-4f10-be6f-dc6cd2aa91a8)
![model run with runtime](https://github.com/star-whale/starwhale/assets/590748/a223c1ac-3818-4bf8-b832-b8f11120cb3f)

## Web Server with Gradio

```bash
swcli model serve -w . -m sw --host 127.0.0.1 --port 8000 --runtime belle
swcli model serve -u belle-bloom-4bit --runtime belle
```

![model serve](https://github.com/star-whale/starwhale/assets/590748/00ab5a1c-a253-4470-a438-0caac314ba5b)

## Dive into the code

- `sw.py`: build, model evaluation, model serving, model fine-tune python script with Starwhale SDK.
- `runtime.yaml`: Starwhale Runtime spec.
- `.swignore`: A file defines ignore pattern, same as .gitignore.
