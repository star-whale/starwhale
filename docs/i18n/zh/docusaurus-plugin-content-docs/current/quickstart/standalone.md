---
title: Standalone 快速上手
---

## 1. 安装Starwhale CLI

Starwhale 有三种类型的Instances：Standalone-单机、On-Premises-私有化集群、Cloud Hosted-SaaS托管服务。Standalone是最简单的模式，可以从Standalone开启你的Starwhale MLOps之旅。
Starwhale Standalone 是用Python3编写的，可以通过pip命令安装：

```bash
python3 -m pip install --pre starwhale
```

:::note
使用 `--pre` 参数可以安装Preview版本的Starwhale CLI。
:::

系统环境要求：

- Python：3.7 ~ 3.10
- 操作系统：Linux或macOS

推荐阅读[Standalone 安装建议](../guides/install/standalone.md)。

## 2. 下载示例程序

```bash
git clone https://github.com/star-whale/starwhale.git
cd starwhale
```

我们选用ML/DL领域的HelloWorld程序-MNIST来介绍如何从零开始构建数据集、模型包和运行环境，并最终完成模型评测。接下来的操作都在 `starwhale` 目录中进行。

![Core Workflow](../img/core-workflow.gif)

## 3. 构建Starwhale Runtime运行环境

Runtime的示例程序在 `example/runtime/pytorch` 目录中。

- 构建Starwhale Runtime：

  ```bash
  cd example/runtime/pytorch
  swcli runtime build .
  ```

- 检查构建好的Starwhale Runtime：

  ```bash
  swcli runtime list
  swcli runtime info pytorch/version/latest
  ```

## 4. 构建Starwhale Model模型包

Model的示例程序在 `example/mnist` 目录中。

- 构建Starwhale Model：

  ```bash
  swcli model build .
  ```

- 检查构建好的Starwhale Runtime：

  ```bash
  swcli model list
  swcli model info mnist/version/latest
  ```

## 5. 构建Starwhale Dataset数据集

Dataset的示例程序在 `example/mnist` 目录中。

- 下载MNIST原始数据：

  ```bash
  mkdir -p data && cd data
  wget http://yann.lecun.com/exdb/mnist/train-images-idx3-ubyte.gz
  wget http://yann.lecun.com/exdb/mnist/train-labels-idx1-ubyte.gz
  wget http://yann.lecun.com/exdb/mnist/t10k-images-idx3-ubyte.gz
  wget http://yann.lecun.com/exdb/mnist/t10k-labels-idx1-ubyte.gz
  gzip -d *.gz
  cd ..
  ls -lah data/*
  ```

- 构建Starwhale Dataset：

  ```bash
  swcli dataset build .
  ```

- 检查构建好的Starwhale Dataset：

  ```bash
  swcli dataset list
  swcli dataset info mnist/version/latest
  ```

## 6. 运行模型评测任务

- 运行模型评测任务：

 ```bash
 swcli -vvv eval run --model mnist/version/latest --dataset mnist/version/latest --runtime pytorch/version/latest
 ```

- 查看模型评测结果：

 ```bash
 swcli eval list
 swcli eval info ${version}
 ```

:::tip
Runtime首次使用的时候会创建隔离的python环境并安装依赖，可能会用时较长，同时建议合理设置 ~/.pip/pip.conf 文件，选用下载速度快的pypi mirror地址。
:::

👏 恭喜，目前已经完成了Starwhale Standalone的基本操作任务。
