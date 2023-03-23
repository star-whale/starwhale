---
title: 核心概念
---

## 设计概述

### Starwhale Dataset 定位

`Starwhale Dataset` 包含数据构建、数据加载和数据可视化三个核心阶段，是一款面向ML/DL领域的数据管理工具。`Starwhale Dataset` 能直接使用 `Starwhale Runtime` 构建的环境，能被 `Starwhale Model` 和 `Starwhale Evaluation` 无缝集成，是Starwhale MLOps工具链的重要组成部分。

根据 [Machine Learning Operations (MLOps): Overview, Definition, and Architecture](https://arxiv.org/abs/2205.02302) 对MLOps Roles的分类，Starwhale Dataset的三个阶段针对用户群体如下：

- 数据构建：Data Engineer、Data Scientist
- 数据加载：Data Scientist、ML Developer
- 数据可视化：Data Engineer、Data Scientist、ML Developer

![mlops-users](../img/mlops-users.png)

### 核心功能

- **高效加载**：数据集原始文件存储在OSS或NAS等外部存储上，使用时按需加载，不需要数据落盘。
- **简单构建**：通过编写简单的Python代码（非必须），少量的dataset.yaml（非必须）后，执行swcli dataset build 命令就能完成数据集的构建。
- **版本管理**：可以进行版本追踪、数据追加等操作，并通过内部抽象的ObjectStore，避免数据重复存储。
- **数据集分发**：通过copy命令，实现standalone instance和cloud instance双向的数据集分享。
- **数据可视化**：Cloud Instance的Web界面中可以对数据集提供多维度、多类型的数据呈现。
- **制品存储**：Standalone Instance能存储本地构建或分发的swds系列文件，Cloud Instance使用对象存储提供集中式的swds制品存储。
- **Starwhale无缝集成**：`Starwhale Dataset` 能使用 `Starwhale Runtime` 构建的运行环境构建数据集。`Starwhale Evaluation` 和 `Starwhale Model` 直接通过 `--dataset` 参数指定数据集，就能完成自动数据加载，便于进行推理、模型评测等环境。

### 关键元素

- `swds` 虚拟包文件：swds 与swmp和swrt不一样，不是一个打包的单一文件，而是一个虚拟的概念，具体指的是一个目录，是Starwhale Dataset某个版本包含的数据集相关的文件，包括 _manifest.yaml, dataset.yaml, 数据集构建的Python脚本和数据文件的链接等。可以通过 `swcli dataset info` 命令查看swds所在目录。swds 是Starwhale Dataset的简写。
![swds-tree.png](../img/swds-tree.png)
- `swcli dataset` 命令行：一组dataset相关的命令，包括构建、分发和管理等功能，具体说明参考[CLI Reference](api/cli.md)。
- `dataset.yaml` 配置文件：描述数据集的构建过程，可以完全省略，通过swcli dataset build参数指定，可以认为dataset.yaml是build命令行参数的一种配置文件表示方式。swcli dataset build 参数优先级高于 dataset.yaml。
- Dataset Python SDK：包括数据构建、数据加载和若干预定义的数据类型，具体说明参考[Python SDK](api/sdk.md)。
- 数据集构建的Python脚本：使用Starwhale Python SDK编写的用来构建数据集的一系列脚本。

## 最佳实践

`Starwhale Dataset` 的构建是独立进行的，如果编写构建脚本时需要引入第三方库，那么使用 `Starwhale Runtime` 可以简化Python的依赖管理，能保证数据集的构建可复现。Starwhale平台会尽可能多的内建开源数据集，让用户copy下来数据集后能立即使用。

`Starwhale Dataset` 构建的时候会自动将Python文件进行打包，可以设置 `.swignore` [文件](../instances/standalone/guides/swignore.md) 排除某些文件。

### 命令行分组

`Starwhale Dataset` 命令行从使用阶段的角度上，可以划分如下：

- 构建阶段
  - `swcli dataset build`
- 可视化阶段
  - `swcli dataset diff`
- 分发阶段：
  - `swcli dataset copy`
- 基本管理
  - `swcli dataset tag`
  - `swcli dataset info`
  - `swcli dataset history`
  - `swcli dataset list`
  - `swcli dataset summary`
  - `swcli dataset remove`
  - `swcli dataset recover`

### 核心流程

`Starwhale Dataset` 使用的核心流程如下图：

![dataset-workflow.jpg](../img/dataset-workflow.jpg)

## dataset.yaml 说明

`Starwhale Dataset` 构建的时候使用dataset.yaml，若省略dataset.yaml，则可以在 `swcli dataset build` 命令行参数中描述相关配置。可以认为dataset.yaml是build命令行的配置文件化表述。

### YAML 字段描述

|字段|描述|是否必要|类型|默认值|
|---|---|-------|---|-----|
|name|Starwhale Dataset的名字|是|String||
|handler|继承 `starwhale.SWDSBinBuildExecutor`, `starwhale.UserRawBuildExecutor` 或 `starwhale.BuildExecutor` 类的可import地址，或者为一个函数，该函数返回一个Generator或一个可迭代的对象，格式为 {module 路径}:{类名|函数名} |是|String||
|desc|数据集描述信息|否|String|""|
|version|dataset.yaml格式版本，目前仅支持填写 1.0|否|String|1.0|
|pkg_data|swds中包含的文件或目录，支持wildcard方式描述。默认会包含 `.py/.sh/.yaml` 文件|否|List[String]||
|exclude_pkg_data|swds中排除的文件或目录，支持wildcard方式描述。不在pkg_data中指定或`.py/.sh/.yaml`后缀的文件，都不会拷贝到swds中|否|List[String]||
|attr|数据集构建参数|否|Dict||
|attr.volume_size|swds-bin格式的数据集每个data文件的大小。当写数字时，单位bytes；也可以是数字+单位格式，如64M, 1GB等|否|Int或Str|64MB|
|attr.alignment_size|swds-bin格式的数据集每个数据块的数据alignment大小，如果设置alignment_size为4k，数据块大小为7.9K，则会补齐0.1K的空数据，让数据块为alignment_size的整数倍，提升page size等读取效率|否|Integer或String|4k|
|attr.data_mime_type|如果不在代码中为每条数据指定MIMEType，则会使用该字段，便于Dataset Viewer呈现|否|String|undefined|
|append|当append设置为True时，表示此次数据集构建会继承 `append_from`版本的数据集内容，实现追加数据集的目的。|否|Boolean|False|
|append_from|与 `append` 参数组合使用，指定继承数据集的版本，注意此处并不是Dataset URI，而是同一个数据集下的其他版本号或tag，默认为latest，即最近一次构建的版本。|否|String|latest|
|project_uri|Project URI|`swcli project select`命令设定的project|String||
|runtime_uri|Runtime URI，若设置，则表示数据集构建的时候会使用该Runtime提供的运行时环境；若不设置，则使用当前shell环境作为运行时|否|String||

当handler为一个函数时，需要该函数返回一个Generator（推荐做法）或一个可迭代的对象（比如一个列表）。Starwhale SDK会根据函数返回值判断首个元素为 `Starwhale.Link` 类型时，构建remote-link或user-raw格式的数据集，否则构建user-raw格式的数据集。不支持混合格式的数据集。

### 使用示例

#### 最简示例

```yaml
name: helloworld
handler: dataset:ExampleProcessExecutor
```

helloworld的数据集，使用dataset.yaml目录中dataset.py文件中的 `ExampleProcessExecutor` 类进行数据构建。

#### MNIST数据集构建示例

```yaml
name: mnist
handler: mnist.dataset:DatasetProcessExecutor

desc: MNIST data and label test dataset

attr:
  alignment_size: 1k
  volume_size: 4M
  data_mime_type: "x/grayscale"
```

#### handler为generator function的例子

dataset.yaml 内容：

```yaml
name: helloworld
handler: dataset:iter_item
```

dataset.py 内容：

```python
def iter_item():
    for i in range(10):
        yield {"img": f"image-{i}".encode(), "label": i}
```

本例中，handler为一个generator function，Starwhale SDK根据首个yield出来的元素为非`Starwhale.Link`类型，等同于继承 `starwhale.SWDSBinBuildExecutor` 类。

## Starwhale Dataset Viewer

目前Cloud Instance中Web UI可以对数据集进行可视化展示，目前只有使用Python SDK的[DataType](api/data_type.md) 才能被前端正确的解释，映射关系如下：

- Image：展示缩略图、放大图、MASK类型图片，支持 `image/png`、`image/jpeg`、`image/webp`、`image/svg+xml`、`image/gif`、`image/apng`、`image/avif` 格式。
- Audio：展示为音频wave图，可播放，支持 `audio/mp3` 和 `audio/wav` 格式。
- GrayscaleImage：展示灰度图，支持 `x/grayscale` 格式。
- Text：展示文本，支持 `text/plain` 格式，设置设置编码格式，默认为utf-8。
- Binary和Bytes：暂不支持展示。
- Link：上述几种多媒体类型都支持指定link作为存储路径。

## Starwhale Dataset 数据格式

`SWDS`以dict作为每个数据条目的格式，但是对key和value有一些简单的限制[L]：

- dict的key必须为str类型
- dict的value必须是int/float/bool/str/bytes/dict/list/tuple等python的基本类型，或者[Starwhale内置的数据类型](api/data_type.md)
- 不同条目的数据相同key的value必须有一致的数据类型
- 如果value是list或者tuple，其元素的数据类型必须一致
- value为dict时，其限制等同于限制[L]

例子：

```python
{
    "img": GrayscaleImage(
        link=Link(
            "123",
            offset=32,
            size=784,
            _swds_bin_offset=0,
            _swds_bin_size=8160,
        )
    ),
    "label": 0,
}
```

### 文件类数据的处理方式

Starwhale Dataset对文件类型的数据进行了特殊处理，如果您不关心Starwhale的实现方式，可以忽略本小节。

根据实际使用场景，Starwhale Dataset 对基类为`starwhale.BaseArtifact`的文件类数据有三种处理方式：

- bytes：Starwhale以自己的二进制格式将bytes合并成若干个大文件，能高效的进行索引、切片和加载。
- 本地文件：不改变原始的数据格式，只是建立索引关系，提供数据类型抽象，同时数据集分发的时候会携带原始数据，便于进行分享。
- 远端文件：满足用户的原始数据存放在某些外部存储上，比如OSS或NAS等，原始数据较多，不方便搬迁或者已经用一些内部的数据集实现进行封装过，那么只需要在数据中使用link，就能建立索引。
