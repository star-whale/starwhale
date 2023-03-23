---
title: Standalone Instance的安装建议
---

我们使用swcli命令行工具完成所有的Standalone Instance上的操作，由于swcli是由纯Python3编写，可以在自己的笔记本或开发机上，使用pip命令完成安装。本文会提供一些安装建议，帮助您获得一个干净的、无依赖冲突的swcli python环境。

:::caution 安装建议
非常不建议将Starwhale安装在系统的全局Python环境中，可能会导致Python的依赖冲突问题。使用venv或conda创建一个隔离的Python环境，并在其中安装Starwhale，是Python推荐的做法。
:::

## 前置条件

- Python 3.7+
- Linux or macOS
- [Conda](https://conda.io/) (optional)

在Ubuntu系统中，可以执行如下命令：

```bash
sudo apt-get install python3 python3-venv python3-pip

#If you want to install multi python versions
sudo add-apt-repository -y ppa:deadsnakes/ppa
sudo apt-get update
sudo apt-get install -y python3.7 python3.8 python3.9 python3-pip python3-venv python3.8-venv python3.7-venv python3.9-venv
```

Starwhale 可以在macOS下工作，包括arm(M1 Chip)和x86(Intel Chip)两种体系结构。但macOS下自带的Python3可能会遇到一些Python自身的问题，推荐使用homebrew进行安装：

```bash
brew install python3
```

## venv环境中安装Starwhale

venv环境即可以使用Python3自带的venv，也可以virtualenv工具。

```bash
python3 -m venv ~/.cache/venv/starwhale
source ~/.cache/venv/starwhale/bin/activate
python3 -m pip install starwhale

swcli --version

sudo rm -rf /usr/local/bin/swcli
sudo ln -s `which swcli` /usr/local/bin/
```

## conda环境中安装Starwhale

```bash
conda create --name starwhale --yes  python=3.9
conda activate starwhale
python3 -m pip install starwhale

swcli --version

sudo rm -rf /usr/local/bin/swcli
sudo ln -s `which swcli` /usr/local/bin/
```

👏 现在可以在全局环境中使用swcli命令行。

需要注意的是，在Linux/macOS中，不使用venv/conda隔离环境，而是使用全局Python，有时会将Starwhale包安装到 ~/.local 下，需要将 ~/.local/bin 添加到PATH中，才能使用 swcli 命令行。

## Starwhale升级

```bash
#for venv
~/.cache/venv/starwhale/bin/python3 -m pip install --upgrade starwhale

#for conda
conda run -n starwhale python3 -m pip install --upgrade starwhale
```

## Starwhale卸载

```bash
python3 -m pip remove starwhale

rm -rf ~/.config/starwhale
rm -rf ~/.starwhale
sudo rm -rf /usr/local/bin/swcli
```
