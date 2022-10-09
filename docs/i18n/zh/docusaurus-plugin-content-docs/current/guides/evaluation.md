---
title: Starwhale Evaluation-模型评测
---

## 1. 设计概述

### 1.1 Starwhale Evaluation 定位

`Starwhale Evaluation` 目标是对模型评测进行全流程管理，包括创建Job、分发Task、查看模型评测报告和基本管理等。`Starwhale Evaluation` 是Starwhale构建的MLOps工具链，使用 `Starwhale Model`、`Starwhale Dataset`、`Starwhale Runtime` 三个基础元素，在模型评测这个场景上的具体应用，后续还会包含 `Starwhale ModelServing`、`Starwhale Training` 等应用场景。

### 1.2 核心功能

- **可视化展示**：CLI和Web UI都提供对模型评测结果的可视化展示，支持多个结果的对比等功能，同时用户可以自定义记录评测中间过程。
- **多场景适配**：不管是在笔记本的单机环境，还是在分布式服务器集群环境，都能使用统一的命令、Python脚本、制品和操作方法进行模型评测，满足不同算力、不同数据量的外部环境要求。
- **Starwhale无缝集成**：使用`Starwhale Runtime`提供的运行环境，将 `Starwhale Dataset` 作为数据输入，在 `Starwhale Model` 中运行模型评测任务，不管是在CLI、Python SDK还是Cloud Instance Web UI中，都能非常简单进行配置。

### 1.3 关键元素

- `swcli eval` 命令行：一组模型评测的相关命令，可以触发任务、结果展示和基本管理等，具体说明参考[CLI Reference](../reference/cli/eval.md)。

## 2. 最佳实践

### 2.1 命令行分组

从完成 `Starwhale Evaluation` 全流程任务的角度，可以将所涉及的命令分组如下：

- 基础准备阶段
  - `swcli dataset build`
  - `swcli model build`
  - `swcli runtime build`
- 评测阶段
  - `swcli model eval`
  - `swcli eval run`
  - `swcli eval cancel`
  - `swcli eval pause`
  - `swcli eval resume`
- 结果展示阶段
  - `swcli eval info`
  - `swcli eval compare`
- 基本管理
  - `swcli eval list`
  - `swcli eval remove`
  - `swcli eval recover`


### 2.2 基本工作流程

## 3. job-step-task 抽象

## 4. 模型评测报告
