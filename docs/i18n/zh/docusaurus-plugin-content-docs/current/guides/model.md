---
title: Starwhale Model-模型包
---

## 1. 设计概述

### 1.1 Starwhale Model 定位

在ML/DL领域中推理环节中，一个可运行的实体，通常会包含N个模型文件，PPL文件（Python文件居多，尤其是算法工程师、数据工程师产生的PPL）、模型超参文件、PPL配置文件和其他可选的附带制品等，这个实体可能会被用来进行模型评测、模型对分、模型Serving等，但目前缺少统一的、标准的打包格式，不利于进行分发、版本追踪、运行复现等，没有一个类似Docker Image之于Linux App之类的格式标准。`Starwhale Model` 提供一种标准的算法模型、代码、配置等的打包和分发机制，是工程师最经常修改、更新、测试的模块，是模型评测的实际代码载体。`Starwhale Model` 能无缝使用 `Starwhale Runtime` 和 `Starwhale Dataset`，是Starwhale MLOps工具链的重要组成部分。

### 1.2 核心功能

- **简单构建**：编写少量model.yaml后，使用一条build命令就能实现对模型、代码、配置等进行打包，生成swmp文件。
- **版本管理和模型包分发**：可以进行版本追踪，通过 `swcli model copy` 命令实现standalone instance和cloud instance的双向分发。
- **调试友好**：在Standalone Instance环境下，编写完成模型评测代码，不需要生成swmp就可以使用 `swcli model eval` 命令进行快速模型评测，可以反复修改代码直到产生预期结果，然后 `swcli model build` 构建带有版本追踪的模型包，并通过 `swcli model copy` 命令分发到Cloud Instance上，使用分布式Task进行更大规模数据集的模型评测。
- **制品存储**：Standalone Instance能存储本地构建或分发的swmp文件，Cloud Instance使用对象存储提供集中式的swmp制品存储。
- **Starwhale无缝集成**：`Starwhale Model` 设置 `--runtime` 参数使用构建好的 `Starwhale Runtime` 环境，设置 `--dataset` 参数使用构建好的 `Starwhale Dataset` 完成模型评测任务。

### 1.3 关键元素

- `swmp` 包文件：`swcli model build` 命令执行后生成模型包文件，目前为tar格式。`swmp` 包文件包含一个_manifest.yaml文件，一个 model.yaml文件，一组模型文件（一个或多个），一些Python脚本和其他需要打包到swmp中的文件。`swmp` 包文件通过 `swcli dataset copy` 命令进行分发。swmp 是Starwhale Model Package的简写。
- `model.yaml` 配置文件：描述 `Starwhale Model` 中文件是如何组织的、模型评测入口点等信息。
- `Starwhale Model` 命令行：一组model相关命令，包括构建、分发和管理等功能，具体说明参考[Cli Reference](../reference/cli/model.md)。
- Model Python SDK：包括默认评测流程的PipelineHandler和自定义流程step等SDK。
- 模型评测的Python脚本：使用Starwhale Python SDK编写的用来进行模型评测的一些列脚本。

### 1.4 Starwhale Model与Starwhale Evaluation的关系

`Starwhale Model` 中包含模型评测的所有代码、模型和配置，是一个静态的包文件，在 `Starwhale Runtime` 构建的运行环境中，使用 `Starwhale Dataset` 数据完成 `Starwhale Evaluation` 模型评测任务。`Starwhale Evaluation` 是一次或多次运行行为。

    `Starwhale Evaluation` = `Starwhale Model` + `Starwhale Dataset` + `Starwhale Runtime`

## 2. 最佳实践

`Starwhale Model` 的构建包括训练好的模型文件、超参配置和模型评测Python代码，若想将 `Starwhale Model` “运行”起来，需要有 `Starwhale Dataset` 和 `Starwhale Runtime` 前置依赖。

### 2.1 命令行分组

`Starwhale Runtime` 命令行从使用阶段的角度上，可以划分如下：

- 构建阶段
  - `swcli model build`
- 分发与复原阶段
  - `swcli model copy`
- 可视化阶段
  - `swcli model diff`
- 运行阶段
  - `swcli model eval`
  - `swcli model serve`
- 基本管理
  - `swcli model list`
  - `swcli model info`
  - `swcli model history`
  - `swcli model tag`
  - `swcli model remove`
  - `swcli model recover`

## 3. model.yaml 说明

model.yaml 对于 `Starwhale Model` 至关重要，描述模型包中文件是如何组织的、模型评测的入口点等关键信息。

### 3.1 YAML字段描述

|字段|描述|是否必要|类型|默认值|
|---|---|-------|---|-----|
|name|Starwhale Model的名字|是|String||
|version|model.yaml格式版本，目前仅支持填写 1.0|否|String|1.0|
|desc|模型包描述信息|否|String|""|
|run|模型包运行的配置|是|Dict||
|run.handler|模型评测的入口点，格式为 {module 路径}:{类名} |是|String||
|run.envs|模型包运行时注入的环境变量，格式为 {名称}={值}|否|List[String]||

### 3.2 使用示例

#### 3.2.1 最简示例

    ```yaml
    name: helloworld
    run:
      handler: model:ExampleHandler
    ```

helloworld模型包，模型评测程序的入口点是model.py中的ExampleHandler类。

#### 3.2.2 nmt模型包示例

    ```yaml
    version: 1.0
    name: nmt

    run:
      handler: nmt.evaluator:NMTPipeline

    desc: nmt by pytorch
    ```

nmt模型包定义handler为model.yaml所在目录中nmt/evaluator.py文件的NMTPipeline类。
