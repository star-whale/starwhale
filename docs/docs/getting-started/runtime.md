---
title: Getting Started with Starwhale Runtime
---

This article demonstrates how to build a Starwhale Runtime of the Pytorch environment and how to use it. This runtime can meet the dependency requirements of the six examples in Starwhale: mnist, speech commands, nmt, cifar10, ag_news, and PennFudan. Links to relevant code: [example/runtime/pytorch](https://github.com/star-whale/starwhale/tree/main/example/runtime/pytorch).

You can learn the following things from this tutorial:

* How to build a Starwhale Runtime.
* How to use a Starwhale Runtime in different scenarios.
* How to release a Starwhale Runtime.

## Prerequisites

* Python 3.7+
* Linux or macOS
* [Starwhale Client](../swcli/index.md) 0.3.0+

Run the following command to clone the example code:

```shell
git clone --depth=1 https://github.com/star-whale/starwhale.git
cd starwhale/example/runtime/pytorch # for users in the mainland of China, use pytorch-cn-mirror instead.
```

## Build Starwhale Runtime

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

![runtime-build](../img/runtime-build.gif)

## Use Starwhale Runtime in the standalone instance

### Use Starwhale Runtime in the shell

```console
# Create a python environment base on the runtime
swcli runtime restore pytorch
# Activate the runtime
swcli runtime activate --uri pytorch
```

`swcli runtime restore` will download all python dependencies of the runtime, which may take a long time.

All dependencies are ready in your python environment when the runtime is activated. It is similar to `source venv/bin/activate` of virtualenv or the `conda activate` command of conda. If you close the shell or switch to another shell, you need to reactivate the runtime.

### Use Starwhale Runtime in SWCLI

```console
# Use the runtime when building a Starwhale Model
swcli model build . --runtime pytorch
# Use the runtime when building a Starwhale Dataset
swcli dataset build . --runtime pytorch
# Run a model evaluation with the runtime
swcli model eval . --dataset mnist --runtime pytorch
```

## Copy Starwhale Runtime to another instance

You can copy the runtime to a server/cloud instance, which can then be used in the server/cloud instance or downloaded by other users.

```console
# Copy the runtime to a server instance named 'pre-k8s'
❯ swcli runtime copy pytorch cloud://pre-k8s/project/starwhale
🚧 start to copy local/project/self/runtime/pytorch/version/latest -> http://console.pre.intra.starwhale.ai/project/starwhale...
  🎳 upload gfsdeyrtmqztezjyg44tkzjwmnttmoi.swrt ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━ 100% 0:00:00 30.7 kB ?
👏 copy done.
```
