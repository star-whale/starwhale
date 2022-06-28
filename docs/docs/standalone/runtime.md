---
title: Starwhale Runtime
---

## What is Starwhale Runtime?

In the development of ML/DL, python is the first class language. A standard and easy-to-use python runtime environment is very important. Starwhale Runtime tries to provide an out-of-the-box runtime management tool that includes:

- a `runtime.yaml` file
- some commands to finish runtime workflow
- a bundle file named `.swrt`
- runtime store in standalone and cloud instance

When we use Starwhale Runtime, we can gain some devops abilities:

- versioning
- shareable
- reproducible and runnable
- system independent

## RECOMMENDED Workflow

### 1. Standalone Preparing Phase

- Step1: Create a runtime, command: `swcli runtime create --mode <venv|conda> --name <runtime name> --python <python version> WORKDIR`
- Step2: Activate this runtime, command: `swcli runtime activate WORKDIR`
- Step3: Install python requirements, use `pip install` or `conda install`.
- Step4: Test python environment: evaluate model, build dataset or run some python scripts.

### 2. Standalone Build and Share Phase

- Step1: Build a runtime, command: `swcli runtime build WORKDIR`
- Step2: Run with runtime, command: `swcli job create --model mnist/version/latest --runtime pytorch-mnist/version/latest --dataset mnist/version/latest`.
- Step3: Copy a runtime to cloud instance, command: `swcli runtime copy pytorch-mnist-env/version/latest http://<host>:<port>/project/self`.

### 3. Use Pre-defined Runtime Phase

- Step1: Copy a runtime to standalone instance, command: `swcli runtime copy http://<host>:<port>/project/self/runtime/pytorch-mnist-env/version/latest local/project/self`
- Step2: Restore runtime for develop: `swcli runtime restore mnist/version/latest`.
- Step3: Run with runtime, same as Phase2-3.

## runtime.yaml Definition

|Name|Description|Required|Default Value|Example|
|----|-----------|--------|-------------|-------|
|mode|environment mode, venv or conda|❌|`venv`|`venv`|
|name|runtime name|✅|`""`|`pytorch-mnist`|
|pip_req|the path of requirements.txt|❌|`requirements.txt`|`requirements.txt`|
|python_version|python version, format is major:minor|❌|`3.8`|`3.9`|
|starwhale_version|starwhale python package version|❌|`""`|`0.2.0b20`|

Example:

```yaml
mode: venv
name: pytorch-mnist
pip_req: requirements.txt
python_version: '3.8'
starwhale_version: '0.2.0b20'
```

- `swcli runtime create` command creates a `runtime.yaml` in the workdir which is a RECOMMENDED method.
- `swcli` uses `starwhale_version` version to render docker image.