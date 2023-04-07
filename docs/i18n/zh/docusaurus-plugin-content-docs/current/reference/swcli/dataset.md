---
title: swcli dataset
---

## 概述

```bash
swcli [全局选项] dataset [选项] <SUBCOMMAND> [参数]...
```

`dataset`命令包括以下子命令：

* `copy`
* `diff`
* `history`
* `info`
* `list`
* `recover`
* `remove`
* `summary`
* `tag`
* `head`

## swcli dataset copy {#copy}

```bash
swcli [全局选项] dataset copy [选项] <SRC> <DEST>
```

`dataset copy`将数据集从`SRC`复制到`DEST`。这里`SRC`和`DEST`都是[数据集URI](../../swcli/uri.md#model-dataset-runtime)。

| 选项 | 必填项 | 类型 | 默认值 | 说明 |
| --- | --- | --- | --- | --- |
|`--force`或`-f`| ❌ | Boolean | False | 如果为true，`DEST`已经存在时会被强制覆盖。否则此命令会显示一条错误消息。 |

## swcli dataset diff {#diff}

```bash
swcli [全局选项] dataset diff [选项] <DATASET VERSION> <DATASET VERSION>
```

`dataset diff`比较同一数据集的两个版本之间的差异。

`DATASET VERSION`是一个[数据集URI](../../swcli/uri.md#model-dataset-runtime)。

| 选项 | 必填项 | 类型 | 默认值 | 说明 |
| --- | --- | --- | --- | --- |
| `--show-details` | ❌ | Boolean | False | 使用该选项输出详细的差异信息。 |

## swcli dataset history {#history}

```bash
swcli [全局选项] dataset history [选项] <DATASET>
```

`dataset history`输出指定Starwhale数据集的所有历史版本。

`DATASET`是一个[数据集URI](../../swcli/uri.md#model-dataset-runtime)。

| 选项 | 必填项 | 类型 | 默认值 | 说明 |
| --- | --- | --- | --- | --- |
| `--fullname` | ❌ | Boolean | False | 显示完整的版本名称。如果没有使用该选项，则仅显示前 12 个字符。 |

## swcli dataset info {#info}

```bash
swcli [全局选项] dataset info [选项] <DATASET>
```

`dataset info`输出指定Starwhale数据集版本的详细信息。

`DATASET`是一个[数据集URI](../../swcli/uri.md#model-dataset-runtime)。

| 选项 | 必填项 | 类型 | 默认值 | 说明 |
| --- | --- | --- | --- | --- |
| `--fullname` | ❌ | Boolean | False | 显示完整的版本名称。如果没有使用该选项，则仅显示前 12 个字符。 |

## swcli dataset list {#list}

```bash
swcli [全局选项] dataset list [选项]
```

`dataset list`命令显示所有的Starwhale数据集。

| `--project` | ❌ | String | | 要查看的项目的URI。如果未指定此选项，则使用[默认项目](../../swcli/uri.md#defaultProject)替代。 |
| `--fullname` | ❌ | Boolean | False | 显示完整的版本名称。如果没有使用该选项，则仅显示前 12 个字符。 |
|`--show-removed`或`-sr` | ❌ | Boolean | False | 如果使用了该选项，则结果中会包含已删除但未被垃圾回收的数据集。 |
| `--page` | ❌ | Integer | 1 | 起始页码。仅限Server和Cloud实例。 |
| `--size` | ❌ | Integer | 20 | 一页中的数据集数量。仅限Server和Cloud实例。 |
| `--filter`或`-fl` | ❌ | String | | 仅显示符合条件的数据集。该选项可以在一个命令中被多次重复使用。 |

| 过滤器 | 类型 | 说明 | 范例 |
| --- | --- | --- | --- |
| `name` | Key-Value | 数据集名称前缀 | `--filter name=mnist` |
| `owner` | Key-Value | 数据集所有者名字 | `--filter owner=starwhale` |
| `latest` | Flag | 如果指定了该选项，结果中仅显示最新版本。 | `--filter latest` |

## swcli dataset recover {#recover}

```bash
swcli [全局选项] dataset recover [选项] <DATASET>
```

`dataset recover`恢复以前删除的Starwhale数据集或版本。

`DATASET`是一个[数据集URI](../../swcli/uri.md#model-dataset-runtime)。如果URI不包含版本，则会恢复所有删除的版本。

已经被垃圾回收或者使用`--force`选项删除的Starwhale数据集或版本无法使用本命令恢复。

| 选项 | 必填项 | 类型 | 默认值 | 说明 |
| --- | --- | --- | --- | --- |
| `--force`或`-f` | ❌ | Boolean | False | 如果使用了该选项，当前同名的Starwhale数据集或版本会被强制覆盖。 |

## swcli dataset remove {#remove}

```bash
swcli [全局选项] dataset remove [选项] <DATASET>
```

`dataset remove`删除指定的Starwhale数据集或某个版本。

`DATASET`是一个[数据集URI](../../swcli/uri.md#model-dataset-runtime)。如果URI不包含版本，则删除指定数据集的所有版本。

Removed Starwhale Datasets or versions can be recovered by `swcli dataset recover` before garbage collection. Use the `--force` option to persistently remove a Starwhale Dataset or version.

被删除的Starwhale数据集或版本可以通过`swcli dataset list --show-removed`列出。

| 选项 | 必填项 | 类型 | 默认值 | 说明 |
| --- | --- | --- | --- | --- |
| `--force`或`-f` | ❌ | Boolean | False | 使用此选项永久删除某个Starwhale数据集或版本。删除后不可恢复。 |

## swcli dataset tag {#tag}

```bash
swcli [全局选项] dataset tag [选项] <DATASET> [TAGS]...
```

`dataset tag`将标签附加到指定的Starwhale数据集版本。可以在数据集URI中使用标签替代版本ID。

`DATASET`是一个[数据集URI](../../swcli/uri.md#model-dataset-runtime)。

每个数据集版本可以包含任意数量的标签，但同一数据集中不允许有重复的标签名称。

| 选项 | 必填项 | 类型 | 默认值 | 说明 |
| --- | --- | --- | --- | --- |
| `--remove`或`-r` | ❌ | Boolean | False | 使用该选项删除标签 |
| `--quiet`或`-q` | ❌ | Boolean | False | 使用该选项以忽略错误，例如删除不存在的标签。 |
