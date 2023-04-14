---
title: swcli model
---

## 概述

```bash
swcli [全局选项] model [选项] <SUBCOMMAND> [参数]...
```

`model`命令包括以下子命令：

* `build`
* `copy`
* `diff`
* `history`
* `info`
* `list`
* `recover`
* `remove`
* `run`
* `tag`

## swcli model build {#build}

```bash
swcli model build [选项] <WORKDIR>
```

`model build`会将整个`WORKDIR`打包到Starwhale模型中，[.swignore](../../swcli/swignore.md)匹配的文件除外。

`model build`会导入`--module`参数指定的模块，然后生成运行模型所需要的配置。如果您指定的模块依赖第三方库，我们强烈建议您使用`--runtime`选项。如果不指定该选项，您需要确保swcli所使用的python环境已经安装了这些库。

| 选项 | 必填项 | 类型 | 默认值 | 说明 |
| --- | --- | --- | --- | --- |
| `--project`或`-p` | ❌ | String | [默认项目](../../swcli/uri.md#defaultProject) | 项目URI |
| `--module` | ❌ | String | | 构建时导入的模块。如果有多个模块，用逗号进行分隔。Starwhale会将这些模块中包含的handler导出到模型包。 |
| `--runtime` | ❌ | String | | 运行此命令时使用的[Starwhale Runtime](../../runtime/index.md)的URI。如果指定此选项，该命令将在Starwhale Runtime指定的独立python环境中运行。否则它将直接在swcli当前的python环境中运行。 |

## swcli model copy {#copy}

```bash
swcli model copy [选项] <SRC> <DEST>
```

`model copy`将模型从`SRC`复制到`DEST`。这里`SRC`和`DEST`都是[模型URI](../../swcli/uri.md#model-dataset-runtime)。

| 选项 | 必填项 | 类型 | 默认值 | 说明 |
| --- | --- | --- | --- | --- |
|`--force`或`-f`| ❌ | Boolean | False | 如果为true，`DEST`已经存在时会被强制覆盖。否则此命令会显示一条错误消息。 |

## swcli model diff {#diff}

```bash
swcli model diff [选项] <MODEL VERSION> <MODEL VERSION>
```

`model diff`比较同一模型的两个版本之间的差异。

`MODEL VERSION`是一个[模型URI](../../swcli/uri.md#model-dataset-runtime)。

| 选项 | 必填项 | 类型 | 默认值 | 说明 |
| --- | --- | --- | --- | --- |
| `--show-details` | ❌ | Boolean | False | 使用该选项输出详细的差异信息。 |

## swcli model history {#history}

```bash
swcli model history [选项] <MODEL>
```

`model history`输出指定Starwhale模型的所有历史版本。

`MODEL`是一个[模型URI](../../swcli/uri.md#model-dataset-runtime)。

| 选项 | 必填项 | 类型 | 默认值 | 说明 |
| --- | --- | --- | --- | --- |
| `--fullname` | ❌ | Boolean | False | 显示完整的版本名称。如果没有使用该选项，则仅显示前 12 个字符。 |

## swcli model info {#info}

```bash
swcli model info [选项] <MODEL>
```

`model info`输出指定Starwhale模型版本的详细信息。

`MODEL`是一个[模型URI](../../swcli/uri.md#model-dataset-runtime)。

| 选项 | 必填项 | 类型 | 默认值 | 说明 |
| --- | --- | --- | --- | --- |
| `--fullname` | ❌ | Boolean | False | 显示完整的版本名称。如果没有使用该选项，则仅显示前 12 个字符。 |

## swcli model list {#list}

```bash
swcli model list [选项]
```

`model list`显示所有的Starwhale模型。

| 选项 | 必填项 | 类型 | 默认值 | 说明 |
| --- | --- | --- | --- | --- |
| `--project` | ❌ | String | | 要查看的项目的URI。如果未指定此选项，则使用[默认项目](../../swcli/uri.md#defaultProject)替代。 |
| `--fullname` | ❌ | Boolean | False | 显示完整的版本名称。如果没有使用该选项，则仅显示前 12 个字符。 |
| `--show-removed` | ❌ | Boolean | False | 如果使用了该选项，则结果中会包含已删除但未被垃圾回收的模型。 |
| `--page` | ❌ | Integer | 1 | 起始页码。仅限Server和Cloud实例。 |
| `--size` | ❌ | Integer | 20 | 一页中的模型数。仅限Server和Cloud实例。 |
| `--filter`或`-fl` | ❌ | String | | 仅显示符合条件的模型。该选项可以在一个命令中被多次重复使用。 |

| 过滤器 | 类型 | 说明 | 范例 |
| --- | --- | --- | --- |
| `name` | Key-Value | 模型名称前缀 | `--filter name=mnist` |
| `owner` | Key-Value | 模型所有者名字 | `--filter owner=starwhale` |
| `latest` | Flag | 如果指定了该选项，结果中仅显示最新版本。 | `--filter latest` |

## swcli model recover {#recover}

```bash
swcli model recover [选项] <MODEL>
```

`model recover`恢复以前删除的Starwhale模型或版本。

`MODEL`是一个[模型URI](../../swcli/uri.md#model-dataset-runtime)。如果URI不包含版本，则会恢复所有删除的版本。

已经被垃圾回收或者使用`--force`选项删除的Starwhale模型或版本无法使用本命令恢复。

| 选项 | 必填项 | 类型 | 默认值 | 说明 |
| --- | --- | --- | --- | --- |
| `--force`或`-f` | ❌ | Boolean | False | 如果使用了该选项，当前同名的Starwhale模型或版本会被强制覆盖。 |

## swcli model remove {#remove}

```bash
swcli model remove [选项] <MODEL>
```

`model remove`删除指定的Starwhale模型或某个版本。

`MODEL`是一个[模型URI](../../swcli/uri.md#model-dataset-runtime)。如果URI不包含版本，则删除指定模型的所有版本。

被删除的Starwhale模型或版本可以在垃圾回收之前通过`swcli model recover`恢复。要永久删除某个Starwhale模型或版本，您可以使用`--force`选项。

被删除的Starwhale模型或版本可以通过`swcli model list --show-removed`列出。

| 选项 | 必填项 | 类型 | 默认值 | 说明 |
| --- | --- | --- | --- | --- |
| `--force`或`-f` | ❌ | Boolean | False | 使用此选项永久删除某个Starwhale模型或版本。删除后不可恢复。 |

## swcli model run {#run}

```bash
swcli model run [选项] <MODEL> <HANDLER>
```

`model run`运行一个模型Handler。 `MODEL`参数可以是[模型URI](../../swcli/uri.md#model-dataset-runtime)或一个Python模块。对于后者，SWCLI将构建一个临时的Starwhale模型。

| 选项 | 必填项 | 类型 | 默认值 | 说明 |
| --- | --- | --- | --- | --- |
| `--runtime` | ❌ | String | | 运行此命令时使用的[Starwhale Runtime](../../runtime/index.md)的URI。如果指定此选项，该命令将在Starwhale Runtime指定的独立python环境中运行。否则它将直接在swcli当前的python环境中运行。 |
| `--use-docker` | ❌ | Boolean | False | 使用docker镜像来运行模型。此选项仅适用于Standalone实例。Server和Cloud实例始终使用docker镜像。如果指定的runtime是基于docker镜像构建的，此选项总是为真。 |

## swcli model tag {#tag}

```bash
swcli model tag [选项] <MODEL> [TAGS]...
```

`model tag`将标签附加到指定的Starwhale模型版本。可以在模型URI中使用标签替代版本ID。

`MODEL`是一个[模型URI](../../swcli/uri.md#model-dataset-runtime)。

每个模型版本可以包含任意数量的标签，但同一模型中不允许有重复的标签名称。

| 选项 | 必填项 | 类型 | 默认值 | 说明 |
| --- | --- | --- | --- | --- |
| `--remove`或`-r` | ❌ | Boolean | False | 使用该选项删除标签 |
| `--quiet`或`-q` | ❌ | Boolean | False | 使用该选项以忽略错误，例如删除不存在的标签。 |
