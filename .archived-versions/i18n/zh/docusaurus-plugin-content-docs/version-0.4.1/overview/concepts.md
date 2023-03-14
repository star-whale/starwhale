---
title: 核心概念
---

![concepts-org.jpg](../img/concepts-org.jpg)

```graph
            Instance
               |
            Project
               |
   +-------+---+---+------+-------+
   |       |       |      |       |
 Model  Dataset Runtime  Job  Evaluation
   |       |       |      |       |
Version Version Version  Step   Version
  Tag     Tag    Tag      |
                         Task
```

## 1. Instance

每个Starwhale的安装都可以称之为一个Starwhale Instance，从实现方式上可以分为Standalone和Cloud：

- **Standalone Instance**
- **Cloud Instance**
  - **On-Premises**
  - **Cloud Hosted**

**Standalone Instance**是最简单的模式，仅需要swcli命令行工具。部署在笔记本或单台开发机上，在开发调试场景中使用，使用pip安装在Python环境中。所有的数据都会存放在本机环境中。

**Cloud Instance**是一种集群模式，包含Web界面、Starwhale Controller和周边基础设施，支持算力水平扩展。通常在生产场景中使用，使用Helm Charts安装在Kubernetes中，通过swcli或Web UI进行操作。
**On-Premises**模式的Starwhale可以私有化部署到用户私有集群中，称之为私有云模式。**Cloud Hosted**模式的Starwhale是由Starwhale团队托管的SaaS服务，称之为公有云模式。两种模式都是Cloud Instance，都包含Starwhale Controller和第三方基础设施，区别是私有云模式需要用户自行运维，公有云模式则由Starwhale团队负责保障服务质量。

**Starwhale 在不同类型的Instance上，能保持核心概念一致，通过该机制，可以用户非常容易的交换数据、迁移程序。**

## 2. Project

**Starwhale Project**用来进行资源组织，每个Project中包含Model、Runtime、Dataset和Evaluation等。用户可以使用Project概念实现多种层面的项目管理，比如按照团队、产品线或某一特定用途的模型来使用不同的Project。
用户通常情况下会在一个或多个Project下进行工作。Cloud Instance中，每个用户都有一个默认的Project。Standalone Instance中，也支持多Project机制，默认会创建一个叫`self`的Project。

## 3. Model

**Starwhale Model**定义了一种标准的模型格式(swmp)，用来进行模型打包和分发。一个Starwhale Model通常包含model.yaml，模型推理和评测的代码，训练好的模型文件、超参等配置文件和其他必要的文件。Starwhale Model可以根据交付的需要，按需进行裁剪，比如出于安全目的删除推理代码，或只提供一个针对边缘设备的最小可部署版本等。

## 4. Runtime

**Starwhale Runtime**描述如何“运行”模型，是一种标准的运行环境格式(swrt)，包括Python依赖、Native Libs、Wheels包、配置文件、脚本和其他文件等。

稳定的、可复现的、无歧义的运行环境对于“运行”模型来说是至关重要的。其中一个原因是，对于大多数数据科学家、算法工程师来说，安装、管理软件依赖关系是非常痛苦的，常常会浪费大量时间解决环境问题，造成无法按期交付。另外，在Deep Learning领域，一个微小的输入错误就会导致最终推理结果产生巨大的差异，甚至一个依赖库的不同版本也会严重影响模型的性能和准确性。因此，如何精准的定义软件运行环境，是十分重要的工程问题。

**Starwhale Runtime**希望能够最大程度的降低用户关于conda、venv和docker等操作困扰，让数据工程师和算法工程师等不需要过多的关于运行环境方面的工程知识储备，使用swcli和编写少量runtime.yaml，就能实现有版本追踪、可复现、可分享、不同Instance上一致的运行环境。**Starwhale Runtime**短期并不是要创建一个全新的Python包管理器或Docker二次封装的命令行工具，而是使用若干开源工具，针对MLOps场景，结合Starwhale其他组件，提供一个更简洁、更好用的Runtime Workflow，实现一次定义，处处使用。

## 5. Dataset

在ML/DL领域，数据和标签起到关键性作用。数据通常会有各种各样的格式，对同样的原始数据，按照不同用户，会制作不同的标签，供多种模型使用。**Starwhale Dataset**定义标准的数据集格式(swds)，实现数据和标签的统一描述、存储，帮助用户高效管理和加载数据。

## 6. Version

Starwhale的Model、Dataset、Runtime和Evaluation，都提供版本管理功能，每次更新都会产生一个新的版本。**Starwhale Version**通过一个自动生成的、唯一的version id来表示，并使用linear history模式，不支持分支和环。用户可以手工删除某个历史版本。

## 7. Job, Step, and Task

**Starwhale Job**包含一组程序，完成某个特定的工作。
一个**Starwhale Job**包含一个或多个**Starwhale Step**，每个**Starwhale Step**包含一个或多个**Starwhale Task**。
**Starwhale Step**表示任务中的某个具体阶段，不同的**Starwhale Step**通常包含不同代码，Step之间可以定义依赖关系，形成DAG。
**Starwhale Task**是**Starwhale Step**的具体运行实体，一个**Starwhale Step**包含的不同**Starwhale Task**运行代码逻辑相同，但输入数据不同。

Job-Step-Task的抽象是实现Starwhale分布式运行的基础，用来实现模型训练、评测和推理服务。

## 8. Evaluation

**Starwhale Evaluation** 目标是对模型评测进行全流程管理，包括创建Job、分发Task、查看模型评测报告和基本管理等。

![arch.jpg](../img/arch.jpg)
