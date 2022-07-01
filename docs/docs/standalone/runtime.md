---
title: Starwhale Runtime
---

## What is Starwhale Runtime?

Python is the first-class language in ML/DL. So that a standard and easy-to-use python runtime environment is critical. Starwhale Runtime tries to provide an out-of-the-box runtime management tool that includes:

- a `runtime.yaml` file
- some commands to finish the runtime workflow
- a bundle file with the `.swrt` extension
- runtime stored in the standalone and cloud instance

When we use Starwhale Runtime, we can gain some DevOps abilities:

- versioning
- shareable
- reproducible
- system-independent

## RECOMMENDED Workflow

### 1. Preparing

- Step1: Create a runtime: `swcli runtime create --mode <venv|conda> --name <runtime name> --python <python version> WORKDIR`
- Step2: Activate this runtime: `swcli runtime activate WORKDIR`
- Step3: Install python requirements by `pip install` or `conda install`.
- Step4: Test python environment: evaluate models, build datasets, or run some python scripts.

### 2. Building and Sharing

- Step1: Build a runtime: `swcli runtime build WORKDIR`
- Step2: Run with runtime: `swcli job create --model mnist/version/latest --runtime pytorch-mnist/version/latest --dataset mnist/version/latest`.
- Step3: Copy a runtime to the cloud instance: `swcli runtime copy pytorch-mnist-env/version/latest http://<host>:<port>/project/self`.

### 3. Using Pre-defined Runtime

- Step1: Copy a runtime to the standalone instance: `swcli runtime copy http://<host>:<port>/project/self/runtime/pytorch-mnist-env/version/latest local/project/self`
- Step2: Restore runtime for development: `swcli runtime restore mnist/version/latest`.
- Step3: Run with runtime, same as Phase2-3.

## runtime.yaml Definition

|Field|Description|Required|Default Value|Example|
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

- `swcli runtime create` command creates a `runtime.yaml` in the working dir, which is a RECOMMENDED method.
- `swcli` uses `starwhale_version` version to render the docker image.
