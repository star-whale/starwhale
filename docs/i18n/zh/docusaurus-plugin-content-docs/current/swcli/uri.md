---
title: Starwhale资源URI
---

:::tip
**资源URI在Starwhale命令行工具中被广泛使用。URI可以引用本地实例中的资源或远程实例中的任何其他资源。 这样Starwhale命令行工具就可以轻松操作任何资源。**
:::

![concepts-org.jpg](../img/concepts-org.jpg)

## 1. 实例URI

实例URI可以是以下形式之一:

- `local`: 指本地的Standalone实例.
- `[http(s)://]<hostname or ip>[:<port>]`：指向一个Starwhale Cloud实例。
- `[cloud://]<cloud alias>`：云实例别名，可以在实例登录阶段配置。

:::caution
“local”不同于“localhost”。前者表示Standalone实例，而后者是一个URL，指向本地运行的Starwhale Server实例。
:::

例子:

```bash
# 登录Starwhale Cloud，别名为swcloud
swcli instance login --username <your account name> --password <your password> https://cloud.starwhale.ai --alias swcloud
# 将模型从本地实例复制到云实例
swcli model copy mnist/version/latest swcloud/project/<your account name>/demo
# 将运行时复制到Starwhale Server实例：http://localhost:8081
swcli runtime copy pytorch/version/v1 http://localhost:8081/project/<your account name>/demo
```

## 2. 项目URI

项目URI的格式为“[<实例URI>/project/]&lt;project name>”。 如果未指定实例URI，则使用当前实例。

例子:

```bash
swcli project select self   # 选择当前实例中的self项目
swcli project info local/project/self  # 查看本地实例中的self项目信息
```

## 3. 模型/数据集/运行时URI

- 模型URI: `[<项目URI>/model/]<model name>[/version/<version id|tag>]`.
- 数据集URI: `[<项目URI>/dataset/]<dataset name>[/version/<version id|tag>]`.
- 运行时URI: `[<项目URI>/runtime/]<runtime name>[/version/<version id|tag>]`.
- `swcli` 支持更加人性化的短版本ID。您可以只键入版本ID的前几个字符，前提是它至少有四个字符长且唯一指向某个版本ID。但是，`recover` 命令必须使用完整的版本ID。
- 如果未指定项目URI，将使用默认项目。
- 您始终可以使用版本标签而不是版本ID。

例子：

```bash
swcli model info mnist/version/hbtdenjxgm4ggnrtmftdgyjzm43tioi  # 检查模型信息，模型名称：mnist，版本：hbtdenjxgm4ggnrtmftdgyjzm43tioi
swcli model remove mnist/version/hbtdenj  # 使用短版本ID
swcli model info mnist  # 检查mnist模型信息
swcli job create --model mnist/version/latest --runtime pytorch-mnist/version/latest --dataset mnist/version/latest # 使用版本标签
```

## 4. 评估URI

- 格式: `[<项目URI>/eval/]<evaluation id>`.
- 如果未指定项目URI，将使用默认项目。

例子：

```bash
swcli eval info mezdayjzge3w   # 查看默认实例和默认项目中的mezdayjzge3w版本
swcli eval info local/project/self/eval/mezday # 检查本地实例，self项目，评估id:mezday
```
