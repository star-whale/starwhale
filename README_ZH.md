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

<a href="https://github.com/star-whale/starwhale/actions/workflows/client.yml">
    <img src="https://github.com/star-whale/starwhale/actions/workflows/client.yml/badge.svg"  alt="Client/SDK UT">
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

Starwhale是一个 MLOps/LLMOps平台，面向机器学习项目提供研发运营管理能力，建立标准化的模型开发、测试、部署和运营流程，连接业务团队、AI团队和运营团队。解决机器学习过程中模型迭代周期长、团队协作、人力资源浪费等问题。

![products](https://starwhale-examples.oss-cn-beijing.aliyuncs.com/docs/products.png)

Starwhale提供Standalone, Server 和 Cloud 三种实例方式，满足单机环境开发，私有化集群部署和Starwhale团队托管的云服务多种部署场景。

- 🐥 **Standalone** - 部署在本地开发环境中，通过 swcli 命令行工具进行管理，满足开发调试需求。
- 🦅 **Server** - 部署在私有数据中心里，依赖 Kubernetes 集群，提供集中化的、Web交互式的、安全的服务。
- 🦉 **Cloud** - 托管在公共云上的服务，访问地址为<https://cloud.starwhale.cn>，由 Starwhale 团队负责运维，无需安装，注册账户后即可使用。

Starwhale 抽象了**模型**、**数据集**和**运行时**作为平台的根基，并在此基础上提供满足特定领域的功能需求：

- 🔥 **Models Evaluation** - Starwhale 模型评测能让用户通过SDK写少量的Python 代码就能实现复杂的、生产级别的、分布式的模型评测任务。
- 🌟 **Live Demo** - 能够通过Web UI方式对模型进行在线评测。
- 🌊 **LLM Fine-tuning** - 提供面向LLM的全流程模型微调工具链，包括模型微调，批量评测对比，在线评测对比和模型发布功能。

Starwhale 同时也是一个开源的平台，使用 [Apache-2.0 协议](https://github.com/star-whale/starwhale/blob/main/LICENSE)。 Starwhale 框架是易于理解的，能非常容易的进行二次开发。

![framework](https://starwhale-examples.oss-cn-beijing.aliyuncs.com/docs/framework.png)

## 核心概念

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

### 🦍 Starwhale 模型微调

Starwhale 模型微调提供针对大语言模型(LLM)的全流程微调工具链，包括模型批量评测、在线评测和模型发布等功能。Starwhale 模型评测的 Python SDK 非常简单，例子如下：

```python
import typing as t
from starwhale import finetune, Dataset
from transformers import Trainer

@finetune(
    resources={"nvidia.com/gpu":4, "memory": "32G"},
    require_train_datasets=True,
    require_validation_datasets=True,
    model_modules=["evaluation", "finetune"],
)
def lora_finetune(train_datasets: t.List[Dataset], val_datasets: t.List[Dataset]) -> None:
    # init model and tokenizer
    trainer = Trainer(
        model=model, tokenizer=tokenizer,
        train_dataset=train_datasets[0].to_pytorch(), # convert Starwhale Dataset into Pytorch Dataset
        eval_dataset=val_datasets[0].to_pytorch())
    trainer.train()
    trainer.save_state()
    trainer.save_model()
    # save weights, then Starwhale SDK will package them into Starwhale Model
```

## 安装

### 🍉 Starwhale Standalone

前置条件: Python 3.7~3.11，运行 Linux 或 macOS 操作系统上。

```bash
python3 -m pip install starwhale
```

### 🥭 Starwhale Server

Starwhale Server 以 Docker 镜像的形式发布。您可以直接使用 Docker 运行，也可以部署到 Kubernetes 集群上。对于本地笔记本电脑环境，推荐使用 `swcli` 命令启动 Starwhale Server，该方式需要本地安装 Docker 和 Docker Compose。

```bash
swcli server start
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

- 🔥 Helloworld: [Cloud](https://cloud.starwhale.cn/projects/15/evaluations), [Code](https://github.com/star-whale/starwhale/tree/main/example/helloworld).
- 🚀 LLM:
  - 🐊 OpenSource LLMs Leaderboard: [Evaluation](https://cloud.starwhale.cn/projects/349/evaluations), [Code](https://github.com/star-whale/starwhale/tree/main/example/llm-leaderboard)
  - 🐢 Llama2: [Run llama2 chat in five minutes](https://starwhale.cn/docs/en/blog/run-llama2-chat-in-five-minutes/), [Code](https://github.com/star-whale/starwhale/tree/main/example/LLM/llama2)
  - 🦎 Stable Diffusion: [Cloud Demo](https://cloud.starwhale.cn/projects/374/models), [Code](https://github.com/star-whale/stable-diffusion-webui)
  - 🦙 LLAMA [evaluation and fine-tune](https://github.com/star-whale/starwhale/tree/main/example/LLM/llama)
  - 🎹 Text-to-Music: [Cloud Demo](https://cloud.starwhale.cn/projects/400/overview), [Code](https://github.com/star-whale/starwhale/tree/main/example/LLM/musicgen)
  - 🍏 Code Generation: [Cloud Demo](https://cloud.starwhale.cn/projects/404/overview), [Code](https://github.com/star-whale/starwhale/tree/main/example/code-generation/code-llama)

- 🌋 Fine-tuning:
  - 🐏 Baichuan2: [Cloud Demo](https://cloud.starwhale.cn/projects/401/overview), [Code](https://github.com/star-whale/starwhale/tree/main/example/llm-finetune/models/baichuan2)
  - 🐫 ChatGLM3: [Cloud Demo](https://cloud.starwhale.cn/projects/401/overview), [Code](https://github.com/star-whale/starwhale/tree/main/example/llm-finetune/models/chatglm3)
  - 🦏 Stable Diffusion: [Cloud Demo](https://cloud.starwhale.cn/projects/374/spaces/3/fine-tune-runs), [Code](https://github.com/star-whale/starwhale/tree/main/example/stable-diffusion/txt2img-ft)

- 🦦 Image Classification:
  - 🐻‍❄️ MNIST: [Cloud Demo](https://cloud.starwhale.cn/projects/392/evaluations), [Code](https://github.com/star-whale/starwhale/tree/main/example/mnist).
  - 🦫 [CIFAR10](https://github.com/star-whale/starwhale/tree/main/example/cifar10)
  - 🦓 Vision Transformer(ViT): [Cloud Demo](https://cloud.starwhale.cn/projects/399/overview), [Code](https://github.com/star-whale/starwhale/tree/main/example/image-classification)
- 🐃 Image Segmentation:
  - Segment Anything(SAM): [Cloud Demo](https://cloud.starwhale.cn/projects/398/overview), [Code](https://github.com/star-whale/starwhale/tree/main/example/image-segmentation)
- 🐦 Object Detection:
  - 🦊 YOLO: [Cloud Demo](https://cloud.starwhale.cn/projects/397/overview), [Code](https://github.com/star-whale/starwhale/tree/main/example/object-detection)
  - 🐯 [Pedestrian Detection](https://github.com/star-whale/starwhale/tree/main/example/PennFudanPed)
- 📽️ Video Recognition: [UCF101](https://github.com/star-whale/starwhale/tree/main/example/ucf101)
- 🦋 Machine Translation: [Neural machine translation](https://github.com/star-whale/starwhale/tree/main/example/nmt)
- 🐜 Text Classification: [AG News](https://github.com/star-whale/starwhale/tree/main/example/text_cls_AG_NEWS)
- 🎙️ Speech Recognition: [Speech Command](https://github.com/star-whale/starwhale/tree/main/example/speech_command)

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
