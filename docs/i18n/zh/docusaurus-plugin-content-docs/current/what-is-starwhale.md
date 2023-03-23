---
slug: /
title: 什么是Starwhale
---

## 概述

Starwhale是一个 MLOps平台，能够让您的模型创建、评估和发布流程变得更加轻松。 它旨在为数据科学家和机器学习工程师创建一个方便的工具。

Starwhale帮您

* 跟踪您的训练/测试数据历史记录，包括所有数据项及其相关标签，以便您轻松访问它们。
* 管理您可以在团队中共享的模型包。
* 在不同的环境中运行您的模型，无论是在 Nvidia GPU 服务器上还是在嵌入式设备（如 Cherry Pi）上。
* 为您的模型快速创建配备交互式 Web UI的在线服务。

Starwhale是一个开放的平台，您可以创建插件来满足自己的需求。

## 部署选项

Starwhale的每个部署称为一个实例。 所有实例都可以通过Starwhale Client（SWCLI）进行管理。

您可以任选以下实例类型之一开始使用：

* **Starwhale Standalone** - Starwhale Standalone本质上是一套存储在本地文件系统中的数据库。它由Starwhale Client（SWCLI）创建和管理。您只需安装SWCLI即可使用。 目前，一台机器上的每个用户只能拥有一个Starwhale Standalone实例。 我们建议您使用Starwhale Standalone来构建和测试您的数据集和模型，然后再将它们推送到Starwhale Server/Cloud实例。
* **Starwhale Server** - Starwhale Server是部署在您本地服务器上的服务。除了Starwhale Client（SWCLI）的文本交互界面，Starwhale Server还提供 Web UI供您管理数据集和模型，以及在Kubernetes集群中运行模型并查看运行结果。
* **Starwhale Cloud** - Starwhale Cloud 是托管在公共云上的托管服务。 通过在<https://cloud.starwhale.ai>或<https://cloud.starwhale.cn>注册一个账号，您就可以使用Starwhale，而无需安装、运行和维护您自己的实例。 Starwhale Cloud还提供公共资源供您下载，例如数据集和模型。 查看Starwhale Cloud上的“starwhale/public”项目以获取更多详细信息。

在您决定要使用的实例类型时，请考虑以下因素：

| 实例类型 | 部署位置 | 维护者 | 用户界面 | 可扩展性 |
| -------------- | -------------- | -------------- | -------------- | -------------- |
| Starwhale Standalone | 您的笔记本电脑或本地服务器 | 不需要 | 命令行 | 不可扩展 |
| Starwhale Server | 您的数据中心 | 您自己 | Web UI和命令行 | 可扩展，取决于您的 Kubernetes 集群 |
| Starwhale Cloud | 公共云，如AWS或阿里云 | Starwhale团队 | Web UI和命令行 | 可扩展，但目前受到云上免费可用资源的限制 |
