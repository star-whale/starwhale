---
title: Starwhale Standalone入门指南
---

当[Starwhale Client（SWCLI）](../swcli)安装完成后，您就可以使用Starwhale Standalone了。

## 下载例子

通过以下方式克隆Starwhale项目来下载Starwhale示例：

```bash
git clone https://github.com/star-whale/starwhale.git
cd starwhale
```

我们将使用 MNIST 和 PyTorch 来开始您的 Starwhale 之旅。 以下步骤均在 starwhale 目录下进行。
![核心工作流程](../img/standalone-core-workflow.gif)

## 构建 Pytorch 运行时

运行时示例代码位于example/runtime/pytorch目录中。

- 构建Starwhale运行时包：

  ```bash
  swcli runtime build example/runtime/pytorch
  ```

- 检查您本地的Starwhale运行时：

  ```bash
  swcli runtime list
  swcli runtime info pytorch/version/latest
  ```

## 建立模型

模型示例代码位于 example/mnist 目录中。

- 下载预训练模型文件：

  ```bash
  mkdir -p example/mnist/models
  wget -O example/mnist/models/mnist_cnn.pt https://starwhale-examples.s3.us-west-1.amazonaws.com/mnist_cnn.pt
  
  # 中国大陆用户可以使用以下网址提高下载速度：
  # wget -O example/mnist/models/mnist_cnn.pt https://starwhale-examples.oss-cn-beijing.aliyuncs.com/mnist_cnn.pt
  ```

- 建立一个Starwhale模型：

  ```bash
  swcli model build example/mnist
  ```

- 检查您本地的Starwhale模型：

  ```bash
  swcli model list
  swcli model info mnist/version/latest
  ```

## 构建数据集

数据集示例代码位于example/mnist目录中。

- 下载MNIST原始数据：

  ```bash
  mkdir -p example/mnist/data
  wget -O example/mnist/data/t10k-images-idx3-ubyte.gz http://yann.lecun.com/exdb/mnist/t10k-images-idx3-ubyte.gz
  wget -O example/mnist/data/t10k-labels-idx1-ubyte.gz http://yann.lecun.com/exdb/mnist/t10k-labels-idx1-ubyte.gz
  gzip -d example/mnist/data/*.gz
  ls -lah example/mnist/data/*
  ```

- 构建Starwhale数据集：

  ```bash
  swcli dataset build example/mnist
  ```

- 检查您本地的Starwhale数据集：

  ```bash
  swcli dataset list
  swcli dataset info mnist/version/latest
  ```

## 运行评估作业

- 创建评估工作

 ```bash
 swcli -vvv eval run --model mnist/version/latest --dataset mnist/version/latest --runtime pytorch/version/latest
 ```

- 检查评估结果

 ```bash
 swcli eval list
 swcli eval info ${version}
 ```

:::tip
如果您在eval run命令中使用了新的运行时，可能需要花费大量时间来下载python依赖项。我们建议您在 ~/.pip/pip.conf文件中设置合适的 PyPI 镜像。
:::

**恭喜！ 您已完成Starwhale Standalone的入门指南。**
