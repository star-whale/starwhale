---
title: SWCLI安装指南
---

在系统的全局Python环境中安装SWCLI可能会导致python依赖项冲突。我们建议您使用一个独立的virutalenv或conda环境来安装SWCLI。

这里有一些安装技巧，可以帮助您获得一个更干净、明确、没有依赖冲突的`swcli`python环境。

## 先决条件

* Python3.7+
* Linux或macOS
* [Conda](https://conda.io/)（可选）

在Ubuntu系统中，可以运行以下命令：

```bash
sudo apt-get install python3 python3-venv python3-pip

#如果您想安装多个python版本
sudo add-apt-repository -y ppa:deadsnakes/ppa
sudo apt-get update
sudo apt-get install -y python3.7 python3.8 python3.9 python3-pip python3-venv python3.8-venv python3.7-venv python3.9-venv
```

SWCLI可以用在macOS上。如果您在macOS上遇到默认系统Python3的问题，请尝试通过homebrew安装Python3：

```bash
brew install python3
```

## 安装SWCLI

### 使用venv安装

```bash
python3 -m venv ~/.cache/venv/starwhale
source ~/.cache/venv/starwhale/bin/activate
python3 -m pip install starwhale

swcli --version

sudo rm -rf /usr/local/bin/swcli
sudo ln -s `which swcli` /usr/local/bin/
```

### 使用conda安装

```bash
conda create --name starwhale --yes  python=3.9
conda activate starwhale
python3 -m pip install starwhale

swcli --version

sudo rm -rf /usr/local/bin/swcli
sudo ln -s `which swcli` /usr/local/bin/
```

👏 现在，您可以在全局环境中使用`swcli`了。

## 更新 SWCLI

```bash
#适用于venv环境
python3 -m pip install --upgrade starwhale

#适用于conda环境
conda run -n starwhale python3 -m pip install --upgrade starwhale
```

## 卸载SWCLI

```bash
python3 -m pip remove starwhale

rm -rf ~/.config/starwhale
rm -rf ~/.starwhale
```
