<div align="center">
    <img src="https://github.com/star-whale/docs/raw/main/static/img/starwhale.png" width="600" style="max-width: 600px;">
    <h1 align="center" style="margin-top: 10px">An MLOps/LLMOps Platform</h1>

🚀 ️☁️ [Starwhale Cloud](https://cloud.starwhale.cn) is now open to the public, try it! 🎉🍻
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
        <b>English</b> |
        <a href="https://github.com/star-whale/starwhale/blob/main/README_ZH.md">中文</a>
    <p>
</h4>

## What is Starwhale

Starwhale is an MLOps/LLMOps platform that make your model creation, evaluation and publication much easier. It aims to create a handy tool for data scientists and machine learning engineers. Starwhale helps you:

- 🏗️ Keep track of your training/testing dataset history including data items and their labels, so that you can easily access them.
- 🧳 Manage your model packages that you can share across your team.
- 🌊 Run your models in different environments, either on a Nvidia GPU server or on an embedded device like Cherry Pi.
- 🔥 Create a online service with interactive Web UI for your models.

![products](https://starwhale-examples.oss-cn-beijing.aliyuncs.com/docs/products.png)

## Key Concepts

### 🦍 Starwhale Instance

Each deployment of Starwhale is called an instance. All instances can be managed by the Starwhale Client (swcli). You can start using Starwhale with one of the following instance types:

- 👻 **Starwhale Standalone**:  Rather than a running service, Starwhale Standalone is actually a repository that resides in your local file system. It is created and managed by the Starwhale Client (SWCLI). You only need to install SWCLI to use it. Currently, each user on a single machine can have only ONE Starwhale Standalone instance. We recommend you use the Starwhale Standalone to build and test your datasets, runtime, and models before pushing them to Starwhale Server/Cloud instances.
- 🎍 **Starwhale Server**:  Starwhale Server is a service deployed on your local server. Besides text-only results from the Starwhale Client (SWCLI), Starwhale Server provides Web UI for you to manage your datasets and models, evaluate your models in your local Kubernetes cluster, and review the evaluation results.
- ☁️ **Starwhale Cloud**: Starwhale Cloud is a managed service hosted on public clouds. By registering an account on https://cloud.starwhale.cn , you are ready to use Starwhale without needing to install, operate, and maintain your own instances. Starwhale Cloud also provides public resources for you to download, like datasets, runtimes, and models. Check the "starwhale/public" project on Starwhale Cloud for more details.

**Starwhale tries to keep concepts consistent across different types of instances. In this way, people can easily exchange data and migrate between them.**

### 🐘 Starwhale Dataset

Starwhale Dataset offers efficient data storage, loading, and visualization capabilities, making it a dedicated data management tool tailored for the field of machine learning and deep learning

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

### 🐇 Starwhale Model

Starwhale Model is a standard format for packaging machine learning models that can be used for various purposes, like model fine-tuning, model evaluation, and online serving. A Starwhale Model contains the model file, inference codes, configuration files, and any other files required to run the model.

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

### 🐌 Starwhale Runtime

Starwhale Runtime aims to provide a reproducible and sharable running environment for python programs. You can easily share your working environment with your teammates or outsiders, and vice versa. Furthermore, you can run your programs on Starwhale Server or Starwhale Cloud without bothering with the dependencies.

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

### 🐄 Starwhale Evaluation

Starwhale Evaluation enables users to evaluate sophisticated, production-ready distributed models by writing just a few lines of code with Starwhale Python SDK.

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

### 🦍 Starwhale Fine-tuning

Starwhale Fine-tuning provides a full workflow for Large Language Model(LLM) tuning, including batch model evaluation, live demo and model release capabilities. Starwhale Fine-tuning Python SDK is very simple.

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

## Installation

### 🍉 Starwhale Standalone

Requirements: Python 3.7~3.11 in the Linux or macOS os.

```bash
python3 -m pip install starwhale
```

### 🥭 Starwhale Server

Starwhale Server is delivered as a Docker image, which can be run with Docker directly or deployed to a Kubernetes cluster. For the laptop environment, using `swcli server start` command is a appropriate choice that depends on Docker and Docker-Compose.

```bash
swcli server start
```

## Quick Tour

We use [MNIST](https://paperswithcode.com/dataset/mnist) as the hello world example to show the basic Starwhale Model workflow.

### 🪅 MNIST Evaluation in Starwhale Standalone

- Use your own Python environment, follow the [Standalone quickstart doc](https://starwhale.cn/docs/en/next/getting-started/standalone/).
- Use Google Colab environment, follow the [Jupyter notebook example](https://colab.research.google.com/github/star-whale/starwhale/blob/main/example/notebooks/quickstart-standalone.ipynb).

### 🪆 MNIST Evaluation in Starwhale Server

- Run it in the your private Starwhale Server instance, please read [Server installation(minikube)](https://starwhale.cn/docs/en/next/server/installation/minikube) and [Server quickstart](https://starwhale.cn/docs/en/next/getting-started/server) docs.
- Run it in the [Starwhale Cloud](https://cloud.starwhale.cn), please read [Cloud quickstart doc](https://starwhale.cn/docs/en/next/getting-started/cloud).

## Examples

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

## Documentation, Community, and Support

- Visit [Starwhale HomePage](https://starwhale.ai).
- More information in the [official documentation](https://doc.starwhale.ai).
- For general questions and support, join the [Slack](https://starwhale.slack.com/).
- For bug reports and feature requests, please use [Github Issue](https://github.com/star-whale/starwhale/issues).
- To get community updates, follow [@starwhaleai](https://twitter.com/starwhaleai) on Twitter.
- For Starwhale artifacts, please visit:

  - Python Package on [Pypi](https://pypi.org/project/starwhale/).
  - Helm Charts on [Artifacthub](https://artifacthub.io/packages/helm/starwhale/starwhale).
  - Docker Images on [Docker Hub](https://hub.docker.com/u/starwhaleai), [Github Packages](https://github.com/orgs/star-whale/packages) and [Starwhale Registry](https://docker-registry.starwhale.cn/).

- Additionally, you can always find us at *developer@starwhale.ai*.

## Contributing

🌼👏**PRs are always welcomed** 👍🍺. See [Contribution to Starwhale](https://doc.starwhale.ai/community/contribute) for more details.

## License

Starwhale is licensed under the [Apache License 2.0](https://github.com/star-whale/starwhale/blob/main/LICENSE).
