---
title: 快速入门
---

**本教程也提供Jupyter Notebook版本，可以在[Colab Notebook](https://colab.research.google.com/github/star-whale/starwhale/blob/main/example/notebooks/quickstart-standalone.ipynb)中在线体验。**

![Core Workflow](../../img/standalone-core-workflow.gif)

## 安装Starwhale CLI

Starwhale 有三种类型的Instances：Standalone-单机、On-Premises-私有化集群、Cloud Hosted-SaaS托管服务。Standalone是最简单的模式，可以从Standalone开启您的Starwhale MLOps之旅。Starwhale Standalone 是用Python3编写的，可以通过pip命令安装：

```bash
python3 -m pip install starwhale
```

:::note
使用 `--pre` 参数可以安装Preview版本的Starwhale CLI。
:::

系统环境要求：

- Python：3.7 ~ 3.11
- 操作系统：Linux或macOS

推荐阅读[Standalone 安装建议](install.md)。

## 下载示例程序

```bash
GIT_LFS_SKIP_SMUDGE=1 git clone https://github.com/star-whale/starwhale.git --depth 1
cd starwhale
```

为了节省例子的下载时间，我们执行git clone命令时，忽略了git-lfs，并只保留最近一次的commit信息。我们选用ML/DL领域的HelloWorld程序-MNIST来介绍如何从零开始构建数据集、模型包和运行环境，并最终完成模型评测。接下来的操作都在 `starwhale` 目录中进行。

## 构建Starwhale Runtime运行环境

Runtime的示例程序在 `example/runtime/pytorch` 目录中。

- 构建Starwhale Runtime：

  :::tip

  当首次构建Starwhale Runtime时，由于需要创建venv或conda隔离环境，并下载相关的Python依赖，命令执行需要花费一段时间。时间长短取决与所在机器的网络情况和runtime.yaml中Python依赖的数量。建议合理设置机器的 `~/.pip/pip.conf` 文件，填写缓存路径和适合当前网络环境的pypi mirror地址。

  处于中国大陆网络环境中的用户，可以参考如下配置：

    ```conf
    [global]
    cache-dir = ~/.cache/pip
    index-url = https://mirrors.aliyun.com/pypi/simple/
    extra-index-url = https://pypi.doubanio.com/simple
    ```

  :::

  ```bash
  swcli runtime build example/runtime/pytorch
  ```

- 检查构建好的Starwhale Runtime：

  ```bash
  swcli runtime list
  swcli runtime info pytorch/version/latest
  ```

- 预先restore Starwhale Runtime(可选):

  ```bash
  swcli runtime restore pytorch/version/latest
  ```

## 构建Starwhale Model模型包

Model的示例程序在 `example/mnist` 目录中。

- 下载预先训练好的模型文件：

  ```bash
  cd example/mnist
  CN=1 make download-model
  # 对于非中国大陆网络环境用户，可以去掉make命令前的 `CN=1` 环境变量
  # make download-model
  cd -
  ```

- 使用Starwhale Runtime来构建Starwhale Model：

  ```bash
  swcli model build example/mnist --runtime pytorch/version/latest
  ```

- 检查构建好的Starwhale Runtime：

  ```bash
  swcli model list
  swcli model info mnist/version/latest
  ```

## 构建Starwhale Dataset数据集

Dataset的示例程序在 `example/mnist` 目录中。

- 下载MNIST原始数据：

  ```bash
  cd example/mnist
  CN=1 make download-data
  # 对于非中国大陆网络环境用户，可以去掉make命令前的 `CN=1` 环境变量
  # make download-data
  cd -
  ```

- 构建Starwhale Dataset：

  ```bash
  swcli dataset build example/mnist --runtime pytorch/version/latest
  ```

- 检查构建好的Starwhale Dataset：

  ```bash
  swcli dataset list
  swcli dataset info mnist/version/latest
  ```

- 查看数据集的前几条数据：

  ```bash
  swcli dataset head mnist/version/latest
  ```

## 运行模型评测任务

- 运行模型评测任务：

 ```bash
 swcli eval run --model mnist/version/latest --dataset mnist/version/latest --runtime pytorch/version/latest
 ```

- 查看模型评测结果：

 ```bash
 swcli eval list
 swcli eval info $(swcli eval list | grep mnist | grep success | awk '{print $1}' | head -n 1)
 ```

👏 恭喜，目前已经完成了Starwhale Standalone的基本操作任务。
