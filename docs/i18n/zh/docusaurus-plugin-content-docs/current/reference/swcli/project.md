---
title: swcli project
---

## Overview

```bash
swcli [全局选项] project [选项] <子命令> [参数]...
```

`project`命令包括以下子命令：

* `create`
* `info`
* `list`
* `recover`
* `remove`
* `use`

## swcli project create {#create}

```bash
swcli [全局选项] project create <PROJECT>
```

`project create`创建一个新的项目。

`PROJECT`是一个[项目URI](../../swcli/uri.md#project)。

## swcli project info {#info}

```bash
swcli [全局选项] project info [选项] <PROJECT>
```

`project info`输出指定项目的详细信息。

`PROJECT`是一个[项目URI](../../swcli/uri.md#project)。

## swcli project list {#list}

```bash
swcli [全局选项] project list [选项]
```

`project list`显示所有的项目。

| 选项 | 必填项 | 类型 | 默认值 | 说明 |
| --- | --- | --- | --- | --- |
| `--instance` | ❌ | String | | 要显示的实例URI。如果不指定该选项，则显示[默认实例](../../swcli/uri.md#defaultInstance). |
| `--show-removed` | ❌ | Boolean | False | 如果使用了该选项，则结果中会包含已删除但未被垃圾回收的项目。 |
| `--page` | ❌ | Integer | 1 | 起始页码。仅限Server和Cloud实例。 |
| `--size` | ❌ | Integer | 20 | 一页中的项目数。仅限Server和Cloud实例。 |

## swcli project recover {#recover}

```bash
swcli [全局选项] project recover [选项] <PROJECT>
```

`project recover`恢复以前删除的项目。

`PROJECT`是一个[项目URI](../../swcli/uri.md#project)。

已经被垃圾回收或者使用`--force`选项删除的项目无法使用本命令恢复。

## swcli project remove {#remove}

```bash
swcli [全局选项] project remove [选项] <PROJECT>
```

`project remove`删除指定的项目。

`PROJECT`是一个[项目URI](../../swcli/uri.md#project)。

被删除的项目可以在垃圾回收之前通过`swcli project recover`恢复。要永久删除某个项目，您可以使用`--force`选项。

被删除的项目可以通过`swcli project list --show-removed`列出。

| 选项 | 必填项 | 类型 | 默认值 | 说明 |
| --- | --- | --- | --- | --- |
| `--force`或`-f` | ❌ | Boolean | False | 使用此选项永久删除某个Starwhale模型或版本。删除后不可恢复。 |

## swcli project use {#use}

```bash
swcli [全局选项] project use <PROJECT>
```

`project use`将指定的项目设置为[默认项目](../../swcli/uri.md#defaultProject)。如果要指定Server/Cloud实例上的项目，您需要先[登录](#login)才能运行本命令。
