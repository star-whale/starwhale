---
title: SWCLI Installation Guide
---

Installing SWCLI in your system's global Python environment may cause a python dependency conflict. We recommend you build an independent virutalenv or conda environment to install SWCLI.

Here are some installation tips that can help you get a cleaner, unambiguous, no dependency conflicts `swcli` python environment.

# 1. Prerequisites

* Python 3.7+
* Linux or macOS
* [Conda](https://conda.io/) (optional)

In the Ubuntu system, you can run the following commands:

```bash
sudo apt-get install python3 python3-venv python3-pip

#If you want to install multi python versions
sudo add-apt-repository -y ppa:deadsnakes/ppa
sudo apt-get update
sudo apt-get install -y python3.7 python3.8 python3.9 python3-pip python3-venv python3.8-venv python3.7-venv python3.9-venv
```

SWCLI works on macOS. If you run into issues with the default system Python3 on macOS, try installing Python3 through the homebrew:

```bash
brew install python3
```

# 2. Install SWCLI

## Install with venv

```bash
python3 -m venv ~/.cache/venv/starwhale
source ~/.cache/venv/starwhale/bin/activate
python3 -m pip install starwhale

swcli --version

sudo rm -rf /usr/local/bin/swcli
sudo ln -s `which swcli` /usr/local/bin/
```

## Install with conda

```bash
conda create --name starwhale --yes  python=3.9
conda activate starwhale
python3 -m pip install starwhale

swcli --version

sudo rm -rf /usr/local/bin/swcli
sudo ln -s `which swcli` /usr/local/bin/
```

👏 Now, you can use `swcli` in the global environment.

# 3. Update SWCLI

```bash
#for venv
python3 -m pip install --upgrade starwhale

#for conda
conda run -n starwhale python3 -m pip install --upgrade starwhale
```

# 4. Uninstall SWCLI

```bash
python3 -m pip remove starwhale

rm -rf ~/.config/starwhale
rm -rf ~/.starwhale
```
