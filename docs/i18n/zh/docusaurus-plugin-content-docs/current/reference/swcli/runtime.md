---
title: Starwhale运行时相关命令
---

## 概述

```bash
swcli [全局选项] model [选项] <SUBCOMMAND> [参数]...
```

`runtime`命令用于管理Starwhale运行时，包括构建、列表、复制等等。

要在SWCLI中引用某个模型，您可以使用[运行时URI](../../swcli/uri.md#model-dataset-runtime)。

`runtime` 命令包括以下子命令：

| 子命令 | Standalone | Cloud |
| --- | --- | --- |
| `activate` or `actv` | ✅ | ❌ |
| `build` | ✅ | ❌ |
| `copy` or `cp` | ✅ | ✅ |
| `dockerize` | ✅ | ❌ |
| `history` | ✅ | ✅ |
| `info` | ✅ | ✅ |
| `list` or `ls` | ✅ | ✅ |
| `recover` | ✅ | ✅ |
| `remove` or `rm` | ✅ | ✅ |
| `tag` | ✅ | ❌ |

## swcli runtime activate {#activate}

```bash
swcli [全局选项] runtime activate [选项]
```

`runtime activate`根据指定的运行时创建一个全新的Python环境，类似`source venv/bin/activate`或`conda activate xxx`的效果。关闭当前shell或切换到其他shell后，需要重新激活Runtime。

**`runtime activate`仅适用于[Standalone实例](../../instances/standalone/index.md).**

对于已经激活的Runtime，如果想要退出该环境，需要在venv环境中执行 `deactivate` 命令或conda环境中执行`conda deactivate` 命令。

| 选项 | 必填项 | 类型 | 默认值 | 说明 |
| --- | --- | --- | --- | --- |
| `--uri` or `-u` | ❌ | String | | 运行时URI |
| `--path` | `-p` | ❌ | String | | venv或conda目录路径 |

`--uri`和`--path`不能同时使用。

## swcli runtime build {#build}

```bash
swcli [全局选项] runtime build [选项] <WORKDIR>
```

`runtime build`会在`WORKDIR`中查找`runtime.yaml`然后创建一个新的Starwhale运行时。

**`runtime build`仅适用于[Standalone实例](../../instances/standalone/index.md).**

| 选项 | 必填项 | 类型 | 默认值 | 说明 |
| --- | --- | --- | --- | --- |
| `--project`或`-p` | ❌ | String | [默认项目](../../swcli/uri.md#defaultProject) | 项目URI |
| `--include-editable` or `-ie` | ❌ | Boolean | False | 是否携带editable的python package |
| `--include-local-wheel` or `-ilw` | ❌ | Boolean | False | 是否在requirements-sw-lock.txt文件中包含本地的wheel包地址，仅对venv模式有效。 |
| `--disable-env-lock` or `-del` | ❌ | Boolean | False | 设置该参数后，不会进行环境初始化、安装依赖和环境导出，直接进行swrt打包。 |
| `--env-prefix-path` or `-ep` | ❌ | String |  | 使用venv或conda目录prefix path作为Python隔离环境 |
| `--env-name` or `-en` | ❌ | String |  | 使用conda环境的env name作为Python隔离环境 |
| `--env-use-shell` or `-es` | ❌ | Boolean | False | 使用当前Shell中Python环境 |
| `--no-cache` or `-nc` | ❌ | Boolean | False | 对于自动生成的Python隔离依赖环境，构建runtime时不使用已经安装过的Python Packages，相当于清除.starwhale/venv或.starwhale/conda目录后再安装依赖然后构建环境 |

## swcli runtime copy {#copy}

```bash
swcli [全局选项] runtime copy [选项] <SRC> <DEST>
```

`runtime copy`将runtime从`SRC`复制到`DEST`。这里`SRC`和`DEST`都是运行时URI。

| 选项 | 必填项 | 类型 | 默认值 | 说明 |
| ------ | ------- | ----------- | ----- | ----------- |
|`--force`或`-f`| ❌ | Boolean | False | 如果为true，`DEST`已经存在时会被强制覆盖。否则此命令会显示一条错误消息。 |

## swcli runtime dockerize {#dockerize}

```bash
swcli [全局选项] runtime dockerize [选项] <RUNTIME>
```

`runtime dockerize`基于指定的runtime创建一个docker镜像。Starwhale使用`docker buildx`来创建镜像。运行此命令需要预先安装Docker 19.03以上的版本。

| 选项 | 必填项 | 类型 | 默认值 | 说明 |
| --- | --- | --- | --- | --- |
| `--tag` or `-t` | ❌ | String |  | Docker镜像的tag，该选项可以重复多次。 |
| `--push` | ❌ | Boolean | False | 是否将创建的镜像推送到docker registry。 |
| `--platform` | ❌ | String | amd64 | 镜像的运行平台，可以是amd64或者arm64。该选项可以重复多次用于创建多平台镜像。|

![runtime-dockerize.png](../../img/runtime-dockerize.png)

**`runtime dockerize`仅适用于[Standalone实例](../../instances/standalone/index.md).**

## swcli runtime history {#history}

```bash
swcli [全局选项] runtime history [选项] <RUNTIME>
```

The `runtime history`命令输出指定Starwhale运行时的所有历史版本。

| 选项 | 必填项 | 类型 | 默认值 | 说明 |
| --- | --- | --- | --- | --- |
| `--fullname` | ❌ | Boolean | False | 显示完整的版本名称。如果没有使用该选项，则仅显示前 12 个字符。 |

## swcli runtime info {#info}

```bash
swcli [全局选项] runtime info [选项] RUNTIME
```

The `runtime info`命令输出指定Starwhale运行时版本的详细信息。

| 选项 | 必填项 | 类型 | 默认值 | 说明 |
| --- | --- | --- | --- | --- |
| `--output-filter` or `-of` | ❌ | Choice of [basic|runtime_yaml|manifest|lock|all] | basic | 设置输出的过滤规则，比如只显示Runtime的runtime.yaml。目前该参数仅对Standalone Instance的Runtime生效。 |

## swcli runtime list {#list}

```bash
swcli [全局选项] runtime list [选项]
```

`model list`命令显示所有的Starwhale运行时。

| 选项 | 必填项 | 类型 | 默认值 | 说明 |
| --- | --- | --- | --- | --- |
| `--project` | ❌ | String | | 要查看的项目的URI。如果未指定此选项，则使用[默认项目](../../swcli/uri.md#defaultProject)替代。 |
| `--fullname` | ❌ | Boolean | False | 显示完整的版本名称。如果没有使用该选项，则仅显示前 12 个字符。 |
|`--show-removed`或`-sr` | ❌ | Boolean | False | 如果使用了该选项，则结果中会包含已删除但未被垃圾回收的运行时。 |
| `--page` | ❌ | Integer | 1 | 起始页码。仅限Server和Cloud实例。 |
| `--size` | ❌ | Integer | 20 | 一页中的运行时数量。仅限Server和Cloud实例。 |
| `--filter`或`-fl` | ❌ | String | | 仅显示符合条件的运行时。该选项可以在一个命令中被多次重复使用。 |

| 过滤器 | 类型 | 说明 | 范例 |
| ---- | ------- | ----------- | ---- |
| `name` | Key-Value | 运行时名称前缀 | `--filter name=pytorch` |
| `owner` | Key-Value | 运行时所有者名字 | `--filter owner=starwhale` |
| `latest` | Flag | 如果指定了该选项，结果中仅显示最新版本。 | `--filter latest` |

## swcli runtime remove {#remove}

```bash
swcli [全局选项] runtime remove [选项] <RUNTIME>
```

`runtime remove`命令可以删除指定的Starwhale运行时或某个版本。

如果RUNTIME参数不包含版本，则删除所有版本。

被删除的Starwhale运行时或版本可以在垃圾回收之前通过`swcli runtime recover`恢复。要永久删除某个Starwhale运行时或版本，您可以使用`--force`选项。

被删除的Starwhale运行时或版本可以通过`swcli runtime list --show-removed`列出。

| 选项 | 必填项 | 类型 | 默认值 | 说明 |
| ------ | ------- | ----------- | ----- | ----------- |
| `--force`或`-f` | ❌ | Boolean | False | 使用此选项永久删除某个Starwhale运行时或版本。删除后不可恢复。 |

## swcli runtime recover {#recover}

```bash
swcli [全局选项] runtime recover [选项] <RUNTIME>
```

`runtime recover`命令可以恢复以前删除的Starwhale运行时或版本。

如果RUNTIME参数不指定版本，则会恢复所有删除的版本。

已经被垃圾回收或者使用`--force`选项删除的Starwhale运行时或版本无法使用本命令恢复。

| 选项 | 必填项 | 类型 | 默认值 | 说明 |
| ------ | ------- | ----------- | ----- | ----------- |
| `--force`或`-f` | ❌ | Boolean | False | 如果使用了该选项，当前同名的Starwhale运行时或版本会被强制覆盖。 |

## swcli runtime tag {#tag}

```bash
swcli [全局选项] runtime tag [选项] RUNTIME [TAGS]...
```

`runtime tag`命令将标签附加到指定的Starwhale运行时版本。可以在运行时URI中使用标签替代版本ID。

每个运行时版本可以包含任意数量的标签，但同一运行时中不允许有重复的标签名称。

**`runtime tag`仅适用于[Standalone实例](../../instances/standalone/index.md).**

| 选项 | 必填项 | 类型 | 默认值 | 说明 |
| --- | --- | --- | --- | --- |
| `--remove`或`-r` | ❌ | Boolean | False | 使用该选项删除标签 |
| `--quiet`或`-q` | ❌ | Boolean | False | 使用该选项以忽略错误，例如删除不存在的标签。 |
