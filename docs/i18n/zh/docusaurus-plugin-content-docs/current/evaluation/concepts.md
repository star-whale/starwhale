---
title: 核心概念
---

## 设计概述

### Starwhale Evaluation 定位

`Starwhale Evaluation` 目标是对模型评测进行全流程管理，包括创建Job、分发Task、查看模型评测报告和基本管理等。`Starwhale Evaluation` 是Starwhale构建的MLOps工具链，使用 `Starwhale Model`、`Starwhale Dataset`、`Starwhale Runtime` 三个基础元素，在模型评测这个场景上的具体应用，后续还会包含 `Starwhale ModelServing`、`Starwhale Training` 等应用场景。

### 核心功能

- **可视化展示**：CLI和Web UI都提供对模型评测结果的可视化展示，支持多个结果的对比等功能，同时用户可以自定义记录评测中间过程。
- **多场景适配**：不管是在笔记本的单机环境，还是在分布式服务器集群环境，都能使用统一的命令、Python脚本、制品和操作方法进行模型评测，满足不同算力、不同数据量的外部环境要求。
- **Starwhale无缝集成**：使用`Starwhale Runtime`提供的运行环境，将 `Starwhale Dataset` 作为数据输入，在 `Starwhale Model` 中运行模型评测任务，不管是在CLI、Python SDK还是Cloud Instance Web UI中，都能非常简单进行配置。

### 关键元素

- `swcli eval` 命令行：一组模型评测的相关命令，可以触发任务、结果展示和基本管理等，具体说明参考[CLI Reference](api/cli.md)。

## 最佳实践

### 命令行分组

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

### 典型工作流程

![eval-workflow.jpg](../img/eval-workflow.jpg)

## job-step-task 抽象

- job：一次模型评测任务就是一个job，一个job包含一个或多个step。
- step：step对应评测过程中的某个阶段。使用PipelineHandler的默认评测过程，step就是ppl和cmp；用户自定义的评测过程，step就是使用 `@step` 修饰的函数。step之间可以有依赖关系，形成一个DAG。一个step包含一个或多个task。同一step中的不同task，执行逻辑是一致的，只是输入参数不同，常见做法是将数据集分割成若干部分，然后传入每个task中，task可以并行执行。
- task：task是最终运行的实体。在Cloud Instance中，一个task就是一个Pod的container；在Standalone Instance中，一个task就是一个Python Thread。

job-step-task 的抽象是实现 `Starwhale Evaluation` 分布式运行的基础。

## 在服务器上运行评测任务

### 覆盖step配置

您可以在创建job的时候提交一个yaml来覆盖在`swmp`中硬编码的step配置。`resources`字段的行为与[K8S容器与pod的资源管理](https://kubernetes.io/docs/concepts/configuration/manage-resources-containers/)中描述的一样。

```yaml
  - job_name: default
    needs: [ ]
    resources:
      - type: cpu # nvidia.com/gpu, memory
        request: 1 # float
        limit: 1 # float
    name: ppl
    task_num: 2
  - job_name: default
    needs:
      - ppl
    resources:
      - type: cpu
        request: 1
        limit: 1
    name: cmp
    task_num: 1
```
