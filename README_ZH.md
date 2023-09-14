<div align="center">
    <img src="https://github.com/star-whale/docs/raw/main/static/img/starwhale.png" width="600" style="max-width: 600px;">
    <h1 align="center" style="margin-top: 10px">An MLOps/LLMOps Platform</h1>

🚀 ️☁️ [Starwhale Cloud](https://cloud.starwhale.cn) 已经上线，欢迎试用! 🎉🍻
</div>

<p align="center">

<a href="https://pypi.org/project/starwhale/">
    <img src="https://img.shields.io/pypi/v/starwhale?style=flat">
</a>

<a href='https://artifacthub.io/packages/helm/starwhale/starwhale'>
    <img src='https://img.shields.io/endpoint?url=https://artifacthub.io/badge/repository/starwhale' alt='Artifact Hub'/>
</a>

<a href="https://pypi.org/project/starwhale/">
    <img alt="PyPI - Python Version" src="https://img.shields.io/pypi/pyversions/starwhale">
</a>

<a href="https://github.com/star-whale/starwhale/actions/workflows/client.yaml">
    <img src="https://github.com/star-whale/starwhale/actions/workflows/client.yaml/badge.svg"  alt="Client/SDK UT">
</a>

<a href="https://github.com/star-whale/starwhale/actions/workflows/server-ut-report.yml">
    <img src="https://github.com/star-whale/starwhale/actions/workflows/server-ut-report.yml/badge.svg" alt="Server UT">
</a>

<a href="https://github.com/star-whale/starwhale/actions/workflows/console.yml">
    <img src="https://github.com/star-whale/starwhale/actions/workflows/console.yml/badge.svg">
</a>

<a href="https://github.com/star-whale/starwhale/actions/workflows/e2e-test.yml">
    <img src='https://github.com/star-whale/starwhale/actions/workflows/e2e-test.yml/badge.svg' alt='Starwhale E2E Test'>
</a>

<a href='https://app.codecov.io/gh/star-whale/starwhale'>
    <img alt="Codecov" src="https://img.shields.io/codecov/c/github/star-whale/starwhale?flag=controller&label=Java%20Cov">
</a>

<a href="https://app.codecov.io/gh/star-whale/starwhale">
    <img alt="Codecov" src="https://img.shields.io/codecov/c/github/star-whale/starwhale?flag=standalone&label=Python%20cov">
</a>
</p>

<h4 align="center">
    <p>
        <b>中文</b> |
        <a href="https://github.com/star-whale/starwhale/blob/main/README.md">English</a>
    <p>
</h4>

## Starwhale 是什么

Starwhale是一个 MLOps/LLMOps平台，能够让您的模型创建、评估和发布流程变得更加轻松。它旨在为数据科学家和机器学习工程师创建一个方便的工具。Starwhale能够帮助您：

- 🏗️ 跟踪您的训练/测试数据集历史记录，包括所有数据项及其相关标签，以便您轻松访问它们。
- 🧳 管理您可以在团队中共享的模型包。
- 🌊 在不同的环境中运行您的模型，无论是在 Nvidia GPU服务器上还是在嵌入式设备（如 Cherry Pi）上。
- 🔥 为您的模型快速创建配备交互式 Web UI的在线服务。

## 核心概念

### 🦍 Starwhale 实例

Starwhale的每个部署称为一个实例。所有实例都可以通过Starwhale Client（swcli）进行管理。您可以任选以下实例类型之一开始使用：

- 👻 **Starwhale Standalone**: Starwhale Standalone 本质上是一套存储在本地文件系统中的数据库。它由 Starwhale Client（swcli）创建和管理。您只需安装 swcli 即可使用。目前，一台机器上的每个用户只能拥有一个Starwhale Standalone 实例。我们建议您使用 Starwhale Standalone 来构建和测试您的数据集和模型，然后再将它们推送到 Starwhale Server/Cloud 实例。
- 🎍 **Starwhale Server**: Starwhale Server 是部署在您本地服务器上的服务。除了 Starwhale Client（swcli）的文本交互界面，Starwhale Server还提供 Web UI供您管理数据集和模型，以及在Kubernetes集群中运行模型并查看运行结果。
- ☁️ **Starwhale Cloud**: Starwhale Cloud 是托管在公共云上的服务。 通过在https://cloud.starwhale.cn注册一个账号，您就可以使用Starwhale，而无需安装、运行和维护您自己的实例。 Starwhale Cloud 还提供公共资源供您下载，例如一些流行的开源集数据集、模型和运行时。查看 Starwhale Cloud 实例上的 “starwhale/public”项目以获取更多详细信息。
**Starwhale 会在不同实例上保持概念上的一致性，用户可以轻松的在不同实例上复制模型、数据集和运行时**。

### 🐘 Starwhale 数据集

Starwhale 数据集能够高效的数据存储、数据加载和数据可视化，是一款面向ML/DL领域的数据管理工具。

![dataset overview](https://starwhale-examples.oss-cn-beijing.aliyuncs.com/docs/dataset-overview.svg)

```python
import torch
from starwhale import dataset, Image

# build dataset for starwhale cloud instance
with dataset("https://cloud.starwhale.cn/project/starwhale:public/dataset/test-image", create="empty") as ds:
    for i in range(100):
        ds.append({"image": Image(f"{i}.png"), "label": i})
    ds.commit()

# load dataset
ds = dataset("https://cloud.starwhale.cn/project/starwhale:public/dataset/test-image")
print(len(ds))
print(ds[0].features.image.to_pil())
print(ds[0].features.label)

torch_ds = ds.to_pytorch()
torch_loader = torch.utils.data.DataLoader(torch_ds, batch_size=5)
print(next(iter(torch_loader)))
```

### 🐇 Starwhale 模型

Starwhale 模型是一种机器学习模型的标准包格式，可用于多种用途，例如模型微调、模型评估和在线服务。 Starwhale 模型包含模型文件、推理代码、配置文件等等。

![overview](https://starwhale-examples.oss-cn-beijing.aliyuncs.com/docs/model-overview.svg)

```bash
# model build
swcli model build . --module mnist.evaluate --runtime pytorch/version/v1 --name mnist

# model copy from standalone to cloud
swcli model cp mnist https://cloud.starwhale.cn/project/starwhale:public

# model run
swcli model run --uri mnist --runtime pytorch --dataset mnist
swcli model run --workdir . --module mnist.evaluator --handler mnist.evaluator:MNISTInference.cmp
```

### 🐌 Starwhale 运行时

Starwhale 运行时能够针对运行Python程序，提供一种可复现、可分享的运行环境。使用 Starwhale 运行时，可以非常容易的与他人分享，并且能在 Starwhale Server 和 Starwhale Cloud 实例上使用 Starwhale 运行时。

![overview](https://starwhale-examples.oss-cn-beijing.aliyuncs.com/docs/runtime-overview.svg)

```bash
# build from runtime.yaml, conda env, docker image or shell
swcli runtime build --yaml runtime.yaml
swcli runtime build --conda pytorch --name pytorch-runtime --cuda 11.4
swcli runtime build --docker pytorch/pytorch:1.9.0-cuda11.1-cudnn8-runtime
swcli runtime build --shell --name pytorch-runtime

# runtime activate
swcli runtime activate pytorch

# integrated with model and dataset
swcli model run --uri test --runtime pytorch
swcli model build . --runtime pytorch
swcli dataset build --runtime pytorch
```

### 🐄 Starwhale 模型评测

Starwhale 模型评测能让用户通过SDK写少量的Python 代码就能实现复杂的、生产级别的、分布式的模型评测任务。

```python
import typing as t
import gradio
from starwhale import evaluation
from starwhale.api.service import api

def model_generate(image):
    ...
    return predict_value, probability_matrix

@evaluation.predict(
    resources={"nvidia.com/gpu": 1},
    replicas=4,
)
def predict_image(data: dict, external: dict) -> None:
    return model_generate(data["image"])

@evaluation.evaluate(use_predict_auto_log=True, needs=[predict_image])
def evaluate_results(predict_result_iter: t.Iterator):
    for _data in predict_result_iter:
        ...
    evaluation.log_summary({"accuracy": 0.95, "benchmark": "test"})

@api(gradio.File(), gradio.Label())
def predict_view(file: t.Any) -> t.Any:
    with open(file.name, "rb") as f:
        data = Image(f.read(), shape=(28, 28, 1))
    _, prob = predict_image({"image": data})
    return {i: p for i, p in enumerate(prob)}
```

## 安装

### 🍉 Starwhale Standalone

前置条件: Python 3.7~3.11，运行 Linux 或 macOS 操作系统上。

```bash
python3 -m pip install starwhale
```

### 🥭 Starwhale Server

Starwhale Server 以 Docker 镜像的形式发布。您可以直接使用 Docker 运行，也可以部署到 Kubernetes 集群上。对于本地笔记本电脑环境，推荐使用 [Minikube](https://minikube.sigs.k8s.io/docs/start/) 进行安装。

```bash
minikube start --addons ingress
helm repo add starwhale https://star-whale.github.io/charts
helm repo update
helm pull starwhale/starwhale --untar --untardir ./charts

helm upgrade --install starwhale ./charts/starwhale -n starwhale --create-namespace -f ./charts/starwhale/values.minikube.global.yaml
```

## 快速指南

我们使用 [Minikube](https://minikube.sigs.k8s.io/docs/start/) 作为Starwhale 平台hello world例子，来展示 Starwhale 的典型工作流程。

### 🪅 Starwhale Standalone 上评测 MNIST

- 如果使用本地Python环境，请参考 [Standalone 快速入门文档](https://starwhale.cn/docs/en/next/getting-started/standalone/)。
- 如果使用Google Colab环境，请参考 [jupyter notebook 例子](https://colab.research.google.com/github/star-whale/starwhale/blob/main/example/notebooks/quickstart-standalone.ipynb)。

### 🪆 Starwhale Server 上评测 MNIST

- 如果想运行在私有化的 Starwhale Server 实例中，请阅读 [Server 安装](https://starwhale.cn/docs/en/next/server/installation/minikube) 和 [Server 快速入门](https://starwhale.cn/docs/en/next/getting-started/server)。
- 如果想运行在 [Starwhale Cloud](https://cloud.starwhale.cn) 中，请阅读 [Cloud 快速入门](https://starwhale.cn/docs/en/next/getting-started/cloud)文档。

## 例子

- 🚀 LLM:
  - 🐊 OpenSource LLMs Leaderboard: [Evaluation](https://cloud.starwhale.cn/projects/349/evaluations), [Code](https://github.com/star-whale/starwhale/tree/main/example/llm-leaderboard)
  - 🐢 Llama2: [Run llama2 chat in five minutes](https://starwhale.cn/docs/en/blog/run-llama2-chat-in-five-minutes/), [Code](https://github.com/star-whale/starwhale/tree/main/example/LLM/llama2)
  - 🦎 Stable Diffusion: [Cloud Demo](https://cloud.starwhale.cn/projects/374/models), [Code](https://github.com/star-whale/stable-diffusion-webui)
  - 🦙 LLAMA [evaluation and fine-tune](https://github.com/star-whale/starwhale/tree/main/example/LLM/llama)
  - 🎹 [MusicGen](https://github.com/star-whale/starwhale/tree/main/example/LLM/musicgen)

- 🦦 Image Classification:
  - 🐻‍❄️ MNIST: [Cloud Demo](https://cloud.starwhale.cn/projects/392/evaluations), [Code](https://github.com/star-whale/starwhale/tree/main/example/mnist)
  - 🦫 [CIFAR10](https://github.com/star-whale/starwhale/tree/main/example/cifar10)

- 🎙️ Speech Recognition: [Speech Command](https://github.com/star-whale/starwhale/tree/main/example/speech_command)
- 🐦 Object Detection: [Pedestrian Detection](https://github.com/star-whale/starwhale/tree/main/example/PennFudanPed)
- 📽️ Video Recognition: [UCF101](https://github.com/star-whale/starwhale/tree/main/example/ucf101)
- 🦋 Machine Translation: [Neural machine translation](https://github.com/star-whale/starwhale/tree/main/example/nmt)
- 🐜 Text Classification: [AG News](https://github.com/star-whale/starwhale/tree/main/example/text_cls_AG_NEWS)

## 文档、社区和帮助

- [Starwhale 首页](https://starwhale.ai)
- [官方文档](https://doc.starwhale.ai)
- 使用 [Github Issue](https://github.com/star-whale/starwhale/issues) 反馈Bug 和 提交Feature Request。
- 微信公众号：

  <img src="https://starwhale-examples.oss-cn-beijing.aliyuncs.com/wechat-public.jpg" width=240>

- Starwhale 发布的制品：

  - [Python Package](https://pypi.org/project/starwhale/)
  - [Helm Charts](https://artifacthub.io/packages/helm/starwhale/starwhale)
  - Docker Image: [Github Packages](https://github.com/orgs/star-whale/packages) 和 [Starwhale Registry](https://docker-registry.starwhale.cn/)。

- 更多帮助，请您通过邮箱 *developer@starwhale.ai* 联系我们。

## 贡献代码

🌼👏**欢迎提交PR** 👍🍺. 请阅读 [Starwhale 开源贡献指南](https://doc.starwhale.ai/community/contribute)。

## 开源协议

Starwhale 使用 [Apache License 2.0](https://github.com/star-whale/starwhale/blob/main/LICENSE) 协议。
