---
title: 基本信息
---

Starwhale 提供一系列的Python SDK，帮助用户更容易的制作数据集、调用模型评测、追踪和展示评测结果等。Python SDK多数场景下与YAML和CLI配合使用，完成模型评测等核心任务。

## 类

- `class PipelineHandler`: 提供默认的模型评测过程定义，需要用户实现 `ppl` 和 `cmp` 函数。
- `class Context`: 执行模型评测过程中传入的上下文信息，包括Project、Task ID等。
- `class SWDSBinBuildExecutor`: 提供swds-bin格式的数据集构建类，需要用户实现 `iter_item` 函数。
- `class UserRawBuildExecutor`: 提供remote-link和user-raw格式的数据集构建类，需要用户实现 `iter_item` 函数。
- `class BuildExecutor`: `SWDSBinBuildExecutor` 类的别称，同为swds-bin格式的数据集构建类。
- `class URI`: starwhale uri的类定义，可以将字符串转化成URI对象。

## 函数

- `multi_classification`: 修饰器，适用于多分类问题，用来简化cmp结果的进一步计算和结果存储，能更好的呈现评测结果。
- `step`: 修饰器，可以指定DAG的依赖关系和Task数量、资源等配置，实现用户自定义评测过程。

## 数据类型

- `COCOObjectAnnotation`: 提供COCO类型的定义。
- `GrayscaleImage`: 灰度图类型，比如MNIST中数字手写体图片，是 `Image` 类型的一个特例。
- `BoundingBox`: 边界框类型，目前为 `LTWH` 格式，即 `left_x`, `top_y`, `width` 和 `height`。
- `ClassLabel`: 描述label的数量和类型。
- `Image`: 图片类型。
- `Audio`: 音频类型。
- `Text`: 文本类型，默认为 `utf-8` 格式。
- `Binary`: 二进制类型，用bytes存储。
- `Link`: Link类型，用来制作 `remote-link` 和 `user-raw` 类型的数据集。
- `S3LinkAuth`: 当数据存储在基于S3协议的对象存储上时，该类型负责描述授权、密钥信息。
- `LocalFSLinkAuth`: 描述数据存储在本地文件系统上。
- `DefaultS3LinkAuth`: 使用默认值初始化 `S3LinkAuth` 类型后得到的变量。
- `MIMEType`: 描述Starwhale支持的多媒体类型，用在 `Image`、`Video` 等类型的mime_type 属性上，能更好的进行Dataset Viewer。
- `LinkType`: 描述Starwhale支持的remote-link类型，目前支持 `LocalFS` 和 `S3` 两种类型。

## 其他

- `__version__`: Starwhale SDK和Cli版本，是字符串常量。
- `URIType`: 描述 `starwhale.URI` 类型。
