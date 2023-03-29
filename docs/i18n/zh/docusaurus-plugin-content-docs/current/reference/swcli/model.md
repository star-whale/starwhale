---
title: Starwhale Model相关命令
---

## 概述

```bash
swcli [全局选项] model [选项] <SUBCOMMAND> [参数]...
```

model命令用于管理Starwhale Model，包括构建、列表、复制、运行等等。

要在SWCLI中引用某个模型，您可以使用[Model URI](../../swcli/uri.md#model-dataset-runtime)。

`model` 命令包括以下子命令：

| 子命令 | Standalone | Cloud |
| --- | ---------- | ----- |
| `build` | ✅ | ❌ |
| `copy`或`cp` | ✅ | ✅ |
| `eval` | ✅ | ❌ |
| `history` | ✅ | ✅ |
| `info` | ✅ | ✅ |
| `list`或`ls` | ✅ | ✅ |
| `recover` | ✅ | ✅ |
| `remove`或`rm` | ✅ | ✅ |
| `tag` | ✅ | ❌ |
| `diff` | ✅ | ✅ |

## swcli model build {#build}

```bash
swcli model build [选项] <WORKDIR>
```

`model build`命令会将整个`WORKDIR`打包到Starwhale Model中，除了[.swignore](../../swcli/config/swignore.md)匹配的文件以外。

`model build`会尝试导入model.yaml中的`run.handler`参数指定的模块，然后生成运行模型所需要的配置。所以如果您指定的模块依赖第三方库，我们强烈建议您使用`--runtime`选项。如果不指定该选项，您需要确认swcli所使用的python环境已经安装了这些库。

**`model build`仅适用于[Standalone实例](../../instances/standalone/index.md).**

| 选项 | 必填项 | 类型 | 默认值 | 说明 |
| ------ | ------- | ----------- | ----- | ----------- |
| `-p`或`--project` | ❌ | String | [默认项目](../../swcli/uri.md#defaultProject) | 项目URI |
| `-f`或`--model-yaml` | ❌ | String | ${workdir}/model.yaml | model.yaml所在路径 |
| `--runtime` | ❌ | String | | 运行此命令时使用的[Starwhale Runtime](../../runtime/index.md)的URI。如果指定此选项，该命令将在Starwhale Runtime指定的独立python环境中运行。否则它将直接在swcli当前的python环境中运行。 |

### model.yaml

model.yaml描述了创建Starwhale Model所需的信息。

| 字段 | 说明 | 必填项 | 类型 | 默认值 |
| --- | --- | ------- | --- | ----- |
| name | Starwhale Model名字 | ✅ | String | |
| version | model.yaml的语法版本。仅支持 1.0。 | ❌ | String | 1.0 |
| desc | Starwhale Model的描述 | ❌ | String | |
| run.handler | 模型评估的入口。格式为“{模块路径}:{类名}”。此字段值是关于命令中指定的WORKDIR参数的相对路径。 | ✅ | String | |
| run.envs | 运行模型时注入的环境变量，格式为 {name}={value} | ❌ | List[String] | |

关于`run.handler` 和`run.envs` 的更多信息，参见[Starwhale Evaluation](../../evaluation/index.md)。

例子:

```yaml
name: demo
version: 1.0
desc: hello world
run:
  handler: model:ExampleHandler # model.py中定义的ExampleHandler类或函数
  envs:
    - a=1
    - b=2
```

## swcli model copy {#copy}

```bash
swcli model copy [选项] <SRC> <DEST>
```

`model copy`将模型从`SRC`复制到`DEST`。这里`SRC`和`DEST`都是模型 URI。

| 选项 | 必填项 | 类型 | 默认值 | 说明 |
| ------ | ------- | ----------- | ----- | ----------- |
|`--force`或`-f`| ❌ | Boolean | False | 如果为true，`DEST`已经存在时会被强制覆盖。否则此命令会显示一条错误消息。 |

## swcli model eval {#eval}

```bash
swcli model eval [选项] <MODEL>
```

`model eval`命令启动一个模型评估 `MODEL`参数可以是模型URI或包含model.yaml的本地目录。如果是本地目录，SWCLI将创建一个临时的Starwhale Model并基于它运行评估。

**`model eval`仅适用于[Standalone实例](../../instances/standalone/index.md).**

| 选项 | 必填项 | 类型 | 默认值 | 说明 |
| ------ | ------- | ----------- | ----- | ----------- |
| `--runtime` | ❌ | String | | 运行此命令时使用的[Starwhale Runtime](../../runtime/index.md)的URI。如果指定此选项，该命令将在Starwhale Runtime指定的独立python环境中运行。否则它将直接在swcli当前的python环境中运行。 |
| `--dataset` | ✅ | String | | 用于运行评估的数据集的 URI。 |
| `--use-docker` | ❌ | Boolean | False | 使用docker运行模型评估。此选项仅适用于Standalone实例。Server和Cloud实例始终使用docker镜像。 |
| `--image` | ❌ | Boolean | False | 运行模型评估所使用的docker镜像。此选项仅在指定--use-docker时生效。 |

![model-eval.gif](../../img/model-eval.gif)

## swcli model history {#history}

```bash
swcli model history [选项] <MODEL>
```

`model history` 命令输出指定Starwhale Model的所有历史版本。

| 选项 | 必填项 | 类型 | 默认值 | 说明 |
| ------ | ------- | ----------- | ----- | ----------- |
| `--fullname` | ❌ | Boolean | False | 显示完整的版本名称。如果没有使用该选项，则仅显示前 12 个字符。 |

## swcli model info {#info}

```bash
swcli model info [选项] <MODEL>
```

`model info`命令输出指定Starwhale模型版本的详细信息。

| 选项 | 必填项 | 类型 | 默认值 | 说明 |
| ------ | ------- | ----------- | ----- | ----------- |
| `--fullname` | ❌ | Boolean | False | 显示完整的版本名称。如果没有使用该选项，则仅显示前 12 个字符。 |

## swcli model list {#list}

```bash
swcli model list [选项]
```

`model list`命令显示所有的Starwhale Model。

| 选项 | 必填项 | 类型 | 默认值 | 说明 |
| ------ | ------- | ----------- | ----- | ----------- |
| `--project` | ❌ | String | | 要查看的项目的URI。如果未指定此选项，则使用[默认项目](../../swcli/uri.md#defaultProject)替代。 |
| `--fullname` | ❌ | Boolean | False | 显示完整的版本名称。如果没有使用该选项，则仅显示前 12 个字符。 |
|`--show-removed`或`-sr` | ❌ | Boolean | False | 如果使用了该选项，则结果中会包含已删除但未被垃圾回收的模型。 |
| `--page` | ❌ | Integer | 1 | 起始页码。仅限Server和Cloud实例。 |
| `--size` | ❌ | Integer | 20 | 一页中的模型数。仅限Server和Cloud实例。 |
| `--filter`或`-fl` | ❌ | String | | 仅显示符合条件的模型。该选项可以在一个命令中被多次重复使用。 |

| 过滤器 | 类型 | 说明 | 范例 |
| ---- | ------- | ----------- | ---- |
| `name` | Key-Value | 模型名称前缀 | `--filter name=mnist` |
| `owner` | Key-Value | 模型所有者名字 | `--filter owner=starwhale` |
| `latest` | Flag | 如果指定了该选项，结果中仅显示最新版本。 | `--filter latest` |

## swcli model remove {#remove}

```bash
swcli model remove [选项] <MODEL>
```

`model remove` 命令可以删除指定的Starwhale Model或某个版本。

如果MODEL参数不包含版本，则删除所有版本。

被删除的Starwhale Model或版本可以在垃圾回收之前通过`swcli model recover`恢复。要永久删除某个Starwhale Model或版本，您可以使用`--force`选项。

被删除的Starwhale Model或版本可以通过`swcli model list --show-removed`列出。

| 选项 | 必填项 | 类型 | 默认值 | 说明 |
| ------ | ------- | ----------- | ----- | ----------- |
| `--force`或`-f` | ❌ | Boolean | False | 使用此选项永久删除某个星鲸模型或版本。删除后不可恢复。 |

## swcli model recover {#recover}

```bash
swcli model recover [选项] <MODEL>
```

`model recover`命令可以恢复以前删除的Starwhale Model或版本。

如果MODEL参数不指定版本，则会恢复所有删除的版本。

已经被垃圾回收或者使用`--force`选项删除的Starwhale Model或版本无法使用本命令恢复。

| 选项 | 必填项 | 类型 | 默认值 | 说明 |
| ------ | ------- | ----------- | ----- | ----------- |
| `--force`或`-f` | ❌ | Boolean | False | 如果使用了该选项，当前同名的Starwhale Model或版本会被强制覆盖。 |

## swcli model tag {#tag}

```bash
swcli model tag [选项] <MODEL> [TAGS]...
```

`model tag`命令将标签附加到指定的Starwhale模型版本。可以在模型URI中使用标签替代版本ID。

每个模型版本可以包含任意数量的标签，但同一模型中不允许有重复的标签名称。

**`model tag`仅适用于[Standalone实例](../../instances/standalone/index.md).**

| 选项 | 必填项 | 类型 | 默认值 | 说明 |
| ------ | ------- | ----------- | ----- | ----------- |
| `--remove`或`-r` | ❌ | Boolean | False | 使用该选项删除标签 |
| `--quiet`或`-q` | ❌ | Boolean | False | 使用该选项以忽略错误，例如删除不存在的标签。 |

## swcli model diff {#diff}

```bash
swcli model diff [选项] <MODEL VERSION> <MODEL VERSION>
```

`model diff`命令比较同一模型的两个版本之间的差异。

| 选项 | 必填项 | 类型 | 默认值 | 说明 |
| ------ | ------- | ----------- | ----- | ----------- |
| `--show-details` | ❌ | Boolean | False | 使用该选项输出详细的差异信息。 |

![model-diff.png](../../img/model-diff.png)
