---
title: standalone installing
---

We can use `swcli` to complete all tasks for Starwhale Standalone mode. `swcli` is written by pure python3 which can be easy-to-install by pip command.
There are some tips of installation which can help you get a cleaner, unambiguous, no dependency conflicts `swcli` python environment.

:::caution Installing Advice
Don't install starwhale in your system global python environment. It will cause python dependency conflict problem.
:::

We recommend you build an independent virutalenv or conda environment to install starwhale.

## Prerequisites

- Python 3.7+
- Linux or MacOSX
- [Conda](https://conda.io/) (optional)

At ubuntu system, you can run the following commands:

```bash
sudo apt-get install python3 python3-venv python3-pip

#If you want to install multi python versions
sudo add-apt-repository -y ppa:deadsnakes/ppa
sudo apt-get update
sudo apt-get install -y python3.7 python3.8 python3.9 python3-pip python3-venv python3.8-venv python3.7-venv python3.9-venv
```

Starwhale works on MacOSX. If you run into issues with the default system Python3 on MacOS, try installing Python3 through the homebrew:

```bash
brew install python3
```

## Install Starwhale with venv

```bash
python3 -m venv ~/.cache/venv/starwhale
source ~/.cache/venv/starwhale/bin/activate
python3 -m pip install --pre starwhale

swcli --version

sudo rm -rf /usr/local/bin/swcli
sudo ln -s `which swcli` /usr/local/bin/
```

## Install Starwhale with conda

```bash
conda create --name starwhale --yes  python=3.9
conda activate starwhale
python3 -m pip install --pre starwhale

swcli --version

sudo rm -rf /usr/local/bin/swcli
sudo ln -s `which swcli` /usr/local/bin/
```

👏 Now you can use `swcli` in global environment.

## Upgrade Starwhale

```bash
#for venv
~/.cache/venv/starwhale/bin/python3 -m pip install --pre --upgrade starwhale

#for conda
conda run -n starwhale python3 -m pip install --pre --upgrade starwhale
```