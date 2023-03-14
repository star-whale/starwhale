---
title: PyTorch Runtime的构建
---

本文提供一个Starwhale Runtime的构建和使用示例，预装了Pytorch的相关库，能满足Starwhale中mnist、speech commands、nmt、cifar10、ag_news和PennFudan这六个例子的依赖需求。相关代码的链接：[example/runtime/pytorch](https://github.com/star-whale/starwhale/tree/main/example/runtime/pytorch)。

从该例中，我们能实践如下Starwhale功能：

- 如何构建Starwhale Runtime。
- 如何在model evaluation和dataset build等过程中使用Starwhale Runtime。
- 如何发布Starwhale Runtime。

## 1. 前置条件

### 1.1 基础环境

- Python版本: 3.7 ~ 3.10。
- OS环境: Linux或macOS。
- Starwhale Client 完成安装，且版本不早于0.3.0。

### 1.2 代码准备

```shell
git clone --depth=1 https://github.com/star-whale/starwhale.git
cd starwhale/example/runtime/pytorch
```

在 `example/runtime` 目录中，有两个pytorch的例子：`pytorch` 是没有设置pypi mirror的，直接使用pypi.org下载包；`pytorch-cn-mirror` 是针对中国大陆网络情况，设置了pypi教育源。可以按需使用，Python依赖包的版本完全一致。`pytorch-cn-mirror` 的runtime.yaml中省略了dependencies中的`wheels`和`files`字段。

## 2. Starwhale Runtime构建

```console
❯ swcli runtime build .
🚧 start to build runtime bundle...
👷 uri:local/project/self/runtime/pytorch
🐦 runtime will ignore pypi editable package
🆕 version gy4wgmzugayw
📁 workdir: /home/liutianwei/.cache/starwhale/self/workdir/runtime/pytorch/gy/gy4wgmzugaywczjyg44tkzjwnvrgq4y
🐝 dump environment info...
dump dependencies info...
🌈 runtime docker image: ghcr.io/star-whale/starwhale:latest-cuda11.4  🌈
🦋 .swrt bundle:/home/liutianwei/.cache/starwhale/self/runtime/pytorch/gy/gy4wgmzugaywczjyg44tkzjwnvrgq4y.swrt
  10 out of 10 steps finished ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━ 100% 0:00:00 0:00:00
```

- Starwhale Runtime的构建核心在于`runtime.yaml`，该命令是一切构建的起点，能描述依赖、配置、基础环境和元信息等元素。构建好的Runtime可以用 `swcli runtime list` 和 `swcli runtime info` 等命令查看。
- 本例子中 `requirements-sw-lock.txt` 文件，是在已经安装好依赖的shell环境，通过执行 `swcli runtime lock` 命令自动产生的问题，也可手工进行修改，里面会描述Python依赖的准确版本，避免复现环境时候依赖不一致问题。当然，也可以不用lock命令固化版本。

![runtime-build](../../img/runtime-build.gif)

## 3. Standalone Instance中使用Runtime

### 3.1 在shell中使用

```console
# 根据runtime.yaml，在standalone instance相关目录中构建python隔离环境并下载依赖
swcli runtime restore pytorch/version/latest
# 在当前环境中激活Pytorch runtime
swcli runtime activate --uri pytorch/version/latest
```

- `restore`会下载python依赖包，此命令首次执行的时候可能会花费比较长的时间，取决于所在机器的网络情况。
- 完成activate后，在当前shell环境中就可以直接使用该Runtime，类似virtualenv的 `source venv/bin/activate` 或conda的 `conda activate` 命令。如果关闭shell或切换到其他shell，需要重新激活runtime。

### 3.2 在swcli命令中使用

```console
# 模型构建中使用Pytorch runtime
swcli model build . --runtime pytorch/version/latest
# 数据集构建中使用Pytorch runtime
swcli dataset build . --runtime pytorch/version/latest
# 模型评测中使用Pytorch runtime
swcli model eval . --dataset mnist/version/latest --runtime pytorch/version/latest
# 在docker环境中激活runtime，并运行评测
swcli eval run --model mnist/version/latest --dataset mnist/version/latest --runtime pytorch/version/latest --use-docker
```

- 在部分swcli命令中可以使用`--runtime`参数，swcli能自动restore并activate, 能保证该命令在对应的Runtime环境中执行。如果`--runtime`参数不指定，命令则会使用当前的shell环境作为依赖环境。
- 使用`--runtime`参数的命令在执行完后，不会污染命令执行前的shell环境。

## 4. 发布Starwhale Runtime

通过copy命令，可以发布Standalone Instance的Runtime到Cloud Instance上，可以在Cloud上直接使用，也可以被其他Standalone Instance下载使用。

```console
# 发布pytorch runtime到 pre-k8s cloud instance的starwhale project中
❯ swcli runtime copy pytorch/version/latest cloud://pre-k8s/project/starwhale
🚧 start to copy local/project/self/runtime/pytorch/version/latest -> http://console.pre.intra.starwhale.ai/project/starwhale...
  🎳 upload gfsdeyrtmqztezjyg44tkzjwmnttmoi.swrt ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━ 100% 0:00:00 30.7 kB ?
👏 copy done.
```
