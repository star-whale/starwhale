---
title: Starwhale模型用户指南
---

Starwhale模型是一种机器学习模型的标准包格式，可用于多种用途，例如模型微调、模型评估和在线服务。 Starwhale模型包含模型文件、推理代码、配置文件等等。

有关包格式的更多信息，请参阅[存储格式](#format)。

## 创建一个Starwhale模型

创建Starwhale模型有两种方法：通过[SWCLI](../reference/swcli/model.md) 或通过SDK。

### 使用SWCLI创建Starwhale模型

使用SWCLI创建Starwhale模型之前，您需要定义一个model.yaml，其中描述了关于Starwhale模型的一些必要信息，然后运行以下命令：

```bash
swcli model build <您的model.yaml所在目录的路径>
```

有关该命令和 model.yaml 的更多信息，请参阅[SWCLI参考](../reference/swcli/model.md#build)。

### 使用SWSDK创建Starwhale模型

## 管理Starwhale模型

### 使用SWCLI管理Starwhale模型

| 命令 | 说明 |
| ------- | ----------- |
| [`swcli model list`](../reference/swcli/model.md#list) | 列出项目中所有Starwhale模型 |
| [`swcli model info`](../reference/swcli/model.md#info) | 显示有关Starwhale模型的详细信息 |
| [`swcli model copy`](../reference/swcli/model.md#copy) | 将Starwhale模型复制到另一个位置 |
| [`swcli model remove`](../reference/swcli/model.md#remove) | 删除Starwhale模型 |
| [`swcli model recover`](../reference/swcli/model.md#recover) | 恢复之前删除的Starwhale模型 |

### 使用Web界面管理Starwhale模型

## 管理Starwhale模型的历史版本

Starwhale模型是版本化的。关于版本的基本信息可以参考[Starwhale中的资源版本控制](../concepts/versioning.md)。

## 使用SWCLI管理Starwhale模型的历史版本

| 命令 | 说明 |
| ------- | ----------- |
| [`swcli model history`](../reference/swcli/model.md#list) | 列出Starwhale模型的所有版本 |
| [`swcli model info`](../reference/swcli/model.md#info) | 显示某个Starwhale模型版本的详细信息 |
| [`swcli model diff`](../reference/swcli/model.md#diff) | 比较两个版本的Starwhale模型 |
| [`swcli model copy`](../reference/swcli/model.md#copy) | 复制某个Starwhale模型版本到新的版本 |
| [`swcli model remove`](../reference/swcli/model.md#remove) | 删除某个Starwhale模型版本 |
| [`swcli model recover`](../reference/swcli/model.md#recover) | 恢复以前删除的Starwhale模型版本 |

## 使用Web界面管理Starwhale模型的历史版本

## 模型评估

### 使用SWCLI进行模型评估

| 命令 | 说明 |
| ------- | ----------- |
| [`swcli model eval`](../reference/swcli/model.md#eval) | 指定某个Starwhale模型进行模型评估 |

### 使用Web界面进行模型评估

#### 使用Web界面运行Starwhale模型

## 存储格式 {#format}

Starwhale模型是一个打包了原始目录的tar文件。
