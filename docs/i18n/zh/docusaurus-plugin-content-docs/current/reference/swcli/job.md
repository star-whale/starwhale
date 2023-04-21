---
title: swcli job
---

## 概述

```bash
swcli [全局选项] job [选项] <子命令> [参数]...
```

`job`命令包括以下子命令：

* `cancel`
* `info`
* `list`
* `pause`
* `recover`
* `remove`
* `resume`

## swcli job cancel {#cancel}

```bash
swcli [全局选项] job cancel [选项] <JOB>
```

`job cancel`停止指定的作业。

`JOB`是一个[作业URI](../../swcli/uri.md#job)。

| 选项 | 必填项 | 类型 | 默认值 | 说明 |
| --- | --- | --- | --- | --- |
| `--force` or `-f` | ❌ | Boolean | False | 如果为真，强制停止指定的作业。 |

## swcli job info {#info}

```bash
swcli [全局选项] job info [选项] <JOB>
```

`job info`输出指定作业的详细信息。

`JOB`是一个[作业URI](../../swcli/uri.md#job)。

| 选项 | 必填项 | 类型 | 默认值 | 说明 |
| --- | --- | --- | --- | --- |
| `--page` | ❌ | Integer | 1 | 起始页码。仅限Server和Cloud实例。 |
| `--size` | ❌ | Integer | 20 | 一页中的作业数。仅限Server和Cloud实例。 |

## swcli job list {#list}

```bash
swcli [全局选项] job list [选项]
```

`job list`显示所有的Starwhale作业。

| 选项 | 必填项 | 类型 | 默认值 | 说明 |
| --- | --- | --- | --- | --- |
| `--project` | ❌ | String | | 要查看的项目的URI。如果未指定此选项，则使用[默认项目](../../swcli/uri.md#defaultProject)替代。 |
| `--show-removed` | ❌ | Boolean | False | 如果使用了该选项，则结果中会包含已删除但未被垃圾回收的作业。 |
| `--page` | ❌ | Integer | 1 | 起始页码。仅限Server和Cloud实例。 |
| `--size` | ❌ | Integer | 20 | 一页中的作业数。仅限Server和Cloud实例。 |

## swcli job pause {#pause}

```bash
swcli [全局选项] job pause [选项] <JOB>
```

`job pause`暂停指定的作业. 被暂停的作业可以使用`job resume`恢复。

`JOB`是一个[作业URI](../../swcli/uri.md#job)。

`pause`和`cancel`功能上基本相同。它们的差别在于被暂停的作业会保留作业ID，在恢复时继续使用。作业的开发者需要定期保存作业数据并在恢复的时候重新加载相关数据。作业ID可以用作保存数据的键值。

| 选项 | 必填项 | 类型 | 默认值 | 说明 |
| --- | --- | --- | --- | --- |
| `--force` or `-f` | ❌ | Boolean | False | 如果为真，强制停止指定的作业。 |

## swcli job resume {#resume}

```bash
swcli [全局选项] job resume [选项] <JOB>
```

`job resume`恢复指定的作业。

`JOB`是一个[作业URI](../../swcli/uri.md#job)。
