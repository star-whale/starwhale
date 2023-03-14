---
title: 核心概念
---

## 1. 设计概述

### 1.1 Starwhale Runtime 定位

`Starwhale Runtime` 是一个完备的、易用的、面向异构设备、开箱即用的ML/DL领域环境管理工具，能与 `Starwhale Dataset` 、`Starwhale Model` 和 `Starwhale Evaluation` 等无缝集成，是Starwhale MLOps工具链的重要组成部分。

`Starwhale Runtime` 希望能够最大程度的降低用户关于conda、venv和docker等操作困扰，让数据工程师和算法工程师等不需要过多的关于运行环境方面的工程知识储备，使用swcli和编写少量runtime.yaml，就能实现有版本追踪、可复现、可分享、不同Instance上一致的运行环境。`Starwhale Runtime` 短期并不是要创建一个全新的Python包管理器或Docker二次封装的命令行工具，而是使用若干开源工具，针对MLOps场景，结合Starwhale其他组件，提供一个更简洁、更好用的Runtime Workflow。

### 1.2 核心功能

- **YAML定义**：通过runtime.yaml描述最终Starwhale Runtime的期望。
- **丰富的Python环境依赖描述方式**：Python版本、conda或venv模式选择、pip依赖、Conda依赖、Wheels包、脚本文件、requirements.txt和conda.yaml文件。
- **版本管理和Runtime分发**：可以进行版本追踪、分发和复现。
- **多体系结构支持**：当使用Docker为运行载体时，目前支持amd64和aarch64两种体系结构。
- **ML/DL使用友好**：屏蔽Docker Image概念，提供优化过的Docker Base Image，并支持用户自定义CUDA和CUDNN版本。
- **制品存储**：Standalone Instance能存储本地构建或分发的swrt文件，Cloud Instance使用对象存储提供集中式的swrt制品存储。
- **Starwhale无缝集成**：`Starwhale Dataset` 构建，`Starwhale Model` 构建和 `Starwhale Evaluation` 运行时，可以直接在命令行中使用 `--runtime` 参数，就能直接使用某个构建好的 `Starwhale Runtime` 运行本次任务。

### 1.3 关键元素

- `runtime.yaml` 配置文件：描述 `Starwhale Runtime` 是如何被定义的，是构建 `Starwhale Runtime` 的起点。runtime.yaml 可以是用户从头编写的，也可以是通过 `swcli runtime quickstart` 命令生成的。
- `swrt` 包文件：`swcli runtime build` 命令执行后生成的runtime打包文件，目前为tar格式。`swrt` 包文件包含一个_manifest.yaml文件，一个 `runtime.yaml` 文件，一组 `requirements.txt`（包含零个或多个）和用户想打包到 `Starwhale Runtime` 的其他文件，包括 native libs, bin files, wheel files 或 python scripts等。`swrt` 在Standalone Instance环境生成后，可以通过 `swcli dataset copy` 命令进行分发。swrt 是Starwhale Runtime的简写。
- `swcli runtime` 命令行：一组runtime相关的命令，包括构建、分发和管理等功能。具体命令行说明，请参考 [CLI Reference](api/cli.md)。

## 2. 最佳实践

`Starwhale Runtime` 的构建是独立进行的，不需要 `Starwhale Model` 和 `Starwhale Dataset` 等前置依赖。通常情况下，在一个小团队中精心制作好一个或少数几个 `Starwhale Runtime` 并不断迭代更新版本，就能满足整组在运行环境方面的需求。

### 2.1 命令行分组

`Starwhale Runtime` 命令行从使用阶段的角度上，可以划分如下：

- 初始化阶段：
  - `swcli dataset quickstart uri`
  - `swcli dataset quickstart from`
- 构建阶段：
  - `swcli dataset lock`
  - `swcli dataset build`
  - `swcli dataset dockerize`
- 分发与复原阶段：
  - `swcli dataset activate`
  - `swcli dataset copy`
  - `swcli dataset extract`
  - `swcli dataset restore`
- 基本管理：
  - `swcli dataset list`
  - `swcli dataset history`
  - `swcli dataset info`
  - `swcli dataset tag`
  - `swcli dataset remove`
  - `swcli dataset recover`

### 2.2 核心流程

`Starwhale Runtime` 使用的核心流程如下图：

![runtime-workflow](../img/runtime-workflow.jpg)

## 3. runtime.yaml 说明

runtime.yaml 对于 `Starwhale Runtime` 至关重要，一切的构建都是从runtime.yaml开始的。

### 3.1 YAML字段描述

|字段|描述|是否必要|类型|默认值|
|---|---|-------|---|-----|
|name|Starwhale Runtime的名字|是|String||
|mode|Python隔离环境的模式，目前支持 venv 和 conda 两种方式|否|Choice[String]|venv|
|api_version|runtime.yaml的版本，目前只支持1.1版本|否|String|1.1|
|configs|描述包括conda, pip和docker等基本配置信息|否|Dict||
|configs.docker|运行swcli runtime dockerize命令是设置的一些docker配置信息，目前支持image字段|否|Dict||
|configs.docker.image|运行swcli runtime dockerize命令生成docker image的名称信息|否|Dict||
|configs.conda|conda的基础配置，目前支持channels设置。仅对runtime内部生效，不会污染宿主机环境|否|Dict||
|configs.conda.channels|conda的channels信息，默认会使用conda default channel，可以设置其他channels|否|List[String]||
|configs.pip|pip的基础配置，仅对runtime内部生效，不会污染宿主机环境。作用相当于设置 ~/.pip/pip.config 中的相关字段|否|Dict||
|configs.pip.index_url|pip config global.index-url信息|否|String||
|configs.pip.extra_index_url|pip config global.extra-index-url信息|否|String 或 List[String]||
|configs.pip.trusted_host|pip config install.trusted-host信息|否|String 或 List[String]||
|dependencies|Python依赖的多种描述方式，目前支持pip、conda、wheels、files、pip的requirements.txt(txt或in文件后缀)、conda的conda.yaml(yaml或yml文件后缀)|否|List||
|environment|描述Runtime的基本环境信息，目前仅对Docker为载体的Runtime的生效|否|Dict|
|environment.arch|可以指定一个或多个arch，目前支持 amd64/aarch64/noarch。当指定noarch时，会自动根据体系结构选择合适的基础镜像|否|Choice[String] 或 List[String]|amd64|
|environment.os|Docker Base Image的OS发行版版本，目前仅支持 ubuntu:20.04 |否|ubuntu:20.04|
|environment.python|运行的Python版本，目前支持3.7/3.8/3.9/3.10四个版本，当前无法对micro字段进行设置，即不支持类似3.7.11这种版本的设置。当生成swrt后会被固化在_manifest.yaml中，确保每次复原Runtime使用同一版本的Python|否|执行swcli命令所在的Python解释器的版本|
|environment.cuda|运行的CUDA版本，目前支持11.3/11.4/11.5/11.6/11.7四个版本。若不设置，则镜像中不包含cuda库。需要注意CUDA版本与宿主机上nvidia驱动版本有一定的[适配关系](https://docs.nvidia.com/deploy/cuda-compatibility/index.html)|否||
|environment.cudnn|运行的CUDNN版本，目前支持CUDNN 8。若不设置，则镜像中不包含cudnn库。设置cudnn字段必须要先设置cuda字段|否||
|environment.docker.image|运行使用的docker环境配置，目前支持设置 image, 一般建议不配置此字段，使用 Starwhale 提供的镜像|否|String||

- Python支持的设置版本与镜像实际使用的版本映射关系(starwhale 0.3.0)
  - Python3.7 --> Python3.7.13
  - Python3.8 --> Python3.8.10
  - Python3.9 --> Python3.9.13
  - Python3.10 --> Python3.10.6

- CUDA与CUDDNN版本映射关系(starwhale 0.3.0)
  - cuda11.3 <--> cudnn8
  - cuda11.4 <--> cudnn8
  - cuda11.5 <--> cudnn8
  - cuda11.6 <--> cudnn8
  - cuda11.7 <--> 目前无适配的cudnn库

- CUDA支持的设置版本与镜像实际使用的版本映射关系(starwhale 0.3.0)
  - cuda11.3 --> cuda11.3.109-1  --> cudnn8.2.1.32
  - cuda11.4 --> cuda11.4.148-1  --> cudnn8.2.2.26
  - cuda11.5 --> cuda11.5.117-1  --> cudnn8.3.3.40
  - cuda11.6 --> cuda11.6.55-1   --> cudnn8.4.1.50
  - cuda11.7 --> cuda11.7.60-1   --> 目前无适配的cudnn库

更全面的CUDNN和CUDNN库的版本信息，请参考Github上的[代码链接](https://github.com/star-whale/starwhale/blob/main/docker/cuda/render.py)。

### 3.2 使用示例

#### 3.2.1 最简示例

```yaml
name: helloworld
```

helloworld的runtime.yaml中只有一行关于name的配置，构建runtime时，就意味着使用如下的默认配置：

- venv作为Python隔离环境。
- python版本为执行 swcli runtime build 命令时，swcli所用的Python解释器的版本。
- arch会根据宿主机体系结构(aarch64或amd64)自动选择合适的基础镜像。
- 选用ubuntu:20.04作为基础镜像的OS版本。
- 使用 `swcli runtime lock`命令或 `swcli runtime build`时自动lock出来的 requirements-sw-lock.txt（存放在.starwhale/lock目录中）中描述的依赖作为Python依赖。

#### 3.2.2 Pytorch Runtime的示例

```yaml
api_version: 1.1
configs:
  conda:
    channels:
      - conda-forge
  docker:
    image: ghcr.io/star-whale/runtime/pytorch
  pip:
    extra_index_url: https://mirrors.bfsu.edu.cn/pypi/web/simple
    index_url: https://pypi.tuna.tsinghua.edu.cn/simple
    trusted_host:
      - pypi.tuna.tsinghua.edu.cn
      - mirrors.bfsu.edu.cn
dependencies:
  - pip:
      - Pillow
      - numpy
      - scikit-learn
      - torchvision
      - torch
      - torchdata
      - torchtext
      - torchaudio
      - pycocotools
  - wheels:
      - dummy-0.0.0-py3-none-any.whl
  - files:
      - dest: bin/prepare.sh
        name: prepare
        src: scripts/prepare.sh
environment:
  arch: noarch
  os: ubuntu:20.04
  cuda: 11.4
  python: 3.8
mode: venv
name: pytorch
```

pytorch runtime例子描述了一个比较复杂的runtime.yaml编写方式，设计pip，wheel，files和requirements.txt四种依赖的描述。

#### 3.2.3 dependencies 示例

仅使用 requirements.txt 格式描述依赖，一般通过 pip freeze，手工编写或其他工具产生该文件。 支持.txt或.in文件后缀。

```yaml
dependencies:
 - requirements.txt
```

使用 conda.yaml 格式描述依赖，一般通过 conda export 命令导出某个conda环境的依赖。支持.yaml或.yml文件后缀。

```yaml
dependencies:
  - conda.yaml
```

使用pip package名称描述依赖。与requirements.txt格式一致，一般是手动填写。

```yaml
dependencies:
  - pip:
    - torch
    - torchaudio==0.1.1
    - importlib-metadata>=4.0.0, <=4.2.0;python_version < 3.8
```

使用离线的wheel包，会被自动安装到Runtime的Python环境中，一般都是内部制作的whl包。wheel包会被打包进入Starwhale Runtime的swrt包中，保证分享到其他环境后可重现。可以指定一个或多个wheel包，文件后缀为 `.whl` 。

```yaml
dependencies:
  - wheels:
    - dummy-0.0.0-py3-none-any.whl
    - test.whl
```

包含文件、库等，会一同进行打包，并按照设置放置到特定位置。通常应用在Runtime中包含一些非Python的包时使用。

```yaml
dependencies:
  - files:
      - dest: bin/prepare.sh       # 目标位置。若是相对路径，则存放在 workdir/export/[conda/venv] 目录的相对位置。若是绝对路径，当在docker环境中restore runtime，则按照预期拷贝到目标目录；非docker环境restore runtime，则存放在workdir/export/[conda/venv] 目录的相对位置。workdir是restore runtime时解压的目标目录。
        name: prepare              # required
        src: scripts/prepare.sh  # 相对runtime.yaml所在目录（workdir）的相对路径，支持文件或目录，若目录则会递归拷贝，required
```

## 4. Starwhale Runtime 与 Docker 的关系

- Starwhale Runtime 并不是Docker Image的上层封装，Docker只是Starwhale Runtime在服务器场景下的一种运行载体。可以理解为Starwhale Runtime是一种抽象，Docker Image是在某些场景下的一种实现方式。
- Starwhale Runtime 未来会在嵌入式设备上提供其他的运行载体，但用户仍采用统一的Runtime描述方式。
- Starwhale Runtime 会对用户屏蔽Docker的相关概念，且目前不支持用户自定义Docker Base Image。
- 用户可以通过 `swcli runtime dockerize` 命令生成Starwhale Runtime的Dockerfile和Image，可以进行个性化的定制。
- Starwhale Runtime 使用docker buildx 为后端构建多体系结构的Docker 镜像。
