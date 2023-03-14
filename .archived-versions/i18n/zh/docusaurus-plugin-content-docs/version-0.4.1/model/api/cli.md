---
title: 模型包命令
---

## 1. 基本信息

```bash
swcli [GLOBAL OPTIONS] model [OPTIONS] COMMAND [ARGS]...
```

model命令提供适用于Standalone Instance和Cloud Instance的Starwhale Model全生命周期的管理，包括构建、查看、分发等功能。在Standalone Instance中，model命令使用本地磁盘存储模型包文件。model命令通过HTTP API对Cloud Instance对象进行操作。

**Model URI**格式：`[<Project URI>/model/]<model name>[/version/<version id>]`。

model包含如下子命令：

|命令|别名|Standalone|Cloud|
|---|---|----------|-----|
|build||✅|❌|
|copy|cp|✅|✅|
|eval||✅|❌|
|history||✅|✅|
|info||✅|✅|
|list|ls|✅|✅|
|recover||✅|✅|
|remove|rm|✅|✅|
|tag||✅|❌|
|diff||✅|✅|

## 2. 构建模型包

```bash
swcli model build [OPTIONS] WORKDIR
```

`model build` 命令在 `WORKDIR` 目录中寻找 `model.yaml` 文件，并以该文件的配置为起点开始构建Starwhale Model。Starwhale Model也可以成为模型包，包括训练好的模型文件、配置文件、评测相关的Python代码、model.yaml和其他文件，文件后缀为 `*.swmp`。`WORKDIR`参数应为一个合法的路径字符串，`model.yaml` 中涉及文件路径的配置，都是相对于 `WORKDIR` 目录的。**`model build`命令目前只能在standalone instance下执行, 支持 `.swignore` 特性**。

`model build` 命令在执行的时候，会import model.yaml中的run.handler 用户代码，用来生成Step-Task的DAG描述文件，如果用户代码中有第三方库的使用，建议使用Starwhale Runtime对依赖进行管理，便于后续迭代。

`model build` 命令参数如下：

|参数|参数别名|必要性|类型|默认值|说明|
|------|--------|-------|-----------|-----|-----------|
|`--project`|`-p`|❌|String|`swcli project select`命令设定的project|Project URI|
|`--model-yaml`|`-f`|❌|String|${workdir}/model.yaml|model yaml文件的路径。|
|`--runtime`||❌|String||`--runtime`参数为Standalone Instance中的Runtime URI。若设置，则表示模型包构建的时候会使用该Runtime提供的运行时环境；若不设置，则使用当前shell环境作为运行时。设置`--runtime`参数是安全的，只在build运行时才会使用Runtime，不会污染当前shell环境。|

## 3. 分发模型包

```bash
swcli model copy [OPTIONS] SRC DEST
```

`model copy` 命令能对构建好的模型包实现高效的分发，既可以从Standalone Instance上传模型包到Cloud Instance，又可以从Cloud Instance上下载模型包到本地的Standalone Instance，目前不支持Cloud A 到 Cloud B， Standalone A到Standalone B这种层面的模型包分发。`SRC` 参数为 `Model URI` 格式，`DEST` 参数为 `Project URI` 或 `Model URI`（忽略version部分）格式。

`dataset copy` 命令参数如下：

|参数|参数别名|必要性|类型|默认值|说明|
|------|--------|-------|-----------|-----|-----------|
|`--force`|`-f`|❌|Boolean|False|`DEST` 存在相同version的模型包，指定该参数后执行copy命令就会强制覆盖。|
|`--dest-local-project`|`-dlp`|❌|String|当从Cloud Instance向Standalone Instance拷贝模型包时，指定的目标Project，若不设置则通过DEST uri进行推断|Project URI|

## 4. 评测模型

```bash
swcli model eval [OPTIONS] TARGET
```

`model eval` 命令可以在本机环境中对模型进行快速评测，目前**该命令只能在standalone instance下执行**。`TARGET` 参数有两种形式，一种是表示Standalone Instance中的某个Model URI，一种是包含model.yaml的目录。常见的场景是，用户在本地环境中开发调试模型评测，运行小规模数据集，反复执行 `model eval` 命令，保证程序运行通畅，结果大致符合预期后，再使用 `model build` 命令进行模型打包，然后分发到Cloud Instance上运行更大规模的数据集，得到可视化的评测报告。

`model eval` 命令其他参数如下：

|参数|参数别名|必要性|类型|默认值|说明|
|------|--------|-------|-----------|-----|-----------|
|`--model-yaml`|`-f`|❌|String|model.yaml|建议使用默认的model.yaml，无需修改。|
|`--project`|`-p`|❌|String|`swcli project select`命令设定的project|Project URI, 该参数也可以通过`SW_PROJECT`环境变量来设置。优先级：参数指定 > 环境变量 > 默认值。|
|`--version`||❌|String|生成一个随机ID|模型评测的版本号，可以使用该参数进行分Step调试。该参数也可以通过 `SW_EVALUATION_VERSION` 环境变量来设置。|
|`--step`||❌|String||评测任务的Step名称，实现只执行该Step的目的。如果不指定，则所有Step都会运行，对于继承 `starwhale.PipelineHandler` 的评测任务，就是ppl和cmp两个Step。|
|`--task-index`||❌|Integer|| `--task-index` 参数用来表明 `--step` 中Step的第n个Task会执行，如果不指定 `--task-index`，则该Step中所有Tasks都会执行。Task索引从0开始，会通过 `starwhale.Context` 传入用户程序中，很多场景下Task的索引值用来指导评测数据集如何分割，实现评测的时候同一个Step的不同Task处理不同部分的评测数据集，实现并行提速。另外需要注意的是，`--task-index` 只有在同时设置 `--step` 参数时才生效。|
|`--runtime`||❌|String||`--runtime`参数为Standalone Instance中的Runtime URI。若设置，则表示模型包构建的时候会使用该Runtime提供的运行时环境；若不设置，则使用当前shell环境作为运行时。设置`--runtime`参数是安全的，只在build运行时才会使用Runtime，不会污染当前shell环境。|
|`--dataset`||✅||String||Dataset URI，该参数也可以通过 `SW_DATASET_URI` 环境变量来设置。|
|`--gencmd`||❌|Boolean|False|当选用设置 `--use-docker` 参数后，只输出docker run的命令，不真正运行。该参数只能在Standalone Instance中使用。|
|`--use-docker`||❌|Boolean|False|选用docker为载体来运行模型评测过程，该参数只能在Standalone Instance中使用。|
|`--image`||❌|Boolean|False|当选用设置 `--use-docker` 参数后, 该参数生效。此image必须支持swcli命令，你可以先使用`--gencmd`查看具体生成的docker命令。如果`--runtime`被同时指定了，`swcli`会调用runtime的baseimage,本参数不再生效|

![model-eval.gif](../../img/model-eval.gif)

## 5. 查看模型包历史版本

```bash
swcli model history [OPTIONS] MODEL
```

`model history` 命令输出模型包所有未删除的版本信息。`MODEL` 参数为Model URI，其他参数如下：

|参数|参数别名|必要性|类型|默认值|说明|
|------|--------|-------|-----------|-----|-----------|
|`--fullname`||❌|Boolean|False|显示完整的版本信息，默认只显示版本号的前12位。|

## 6. 查看模型包详细信息

```bash
swcli model info [OPTIONS] MODEL
```

`model info` 命令输出模型包或模型包中某个版本的详细信息。`MODEL` 参数为Model URI，其他参数如下：

|参数|参数别名|必要性|类型|默认值|说明|
|------|--------|-------|-----------|-----|-----------|
|`--fullname`||❌|Boolean|False|显示完整的版本信息，默认只显示版本号的前12位。|

## 7. 展示模型包列表

```bash
swcli model list [OPTIONS]
```

`model list` 命令输出当前选定的instance和project下的所有模型包及相关版本。命令参数如下：

|参数|参数别名|必要性|类型|默认值| 说明                                                 |
|---|---|---|---|---|----------------------------------------------------|
|`--project`|`-p`|❌|String|`swcli project select`命令选定的默认project | Project URI|
|`--fullname`|`-f`|❌|Boolean|False| 显示完整的版本信息，默认只显示版本号的前12位。|
|`--show-removed`|`-sr`|❌|Boolean|False| 显示本地已经删除但能恢复的模型包。|
|`--page`| |❌|Integer|1| Cloud Instance中分页显示中page序号。|
|`--size`| |❌|Integer|20| Cloud Instance中分页显示中每页数量。|
|`--filter`|`-fl`|❌|String| | 过滤器，使用key=value格式或者flag，可使用多个filter，具体支持的filter如下： |

|Filter|类型| 说明 | 示例|
|----|-------|-----------|----|
|`name`| Key-Value | 模型名称前缀 |--filter name=mnist|
|`owner`| Key-Value | 拥有者名称  |--filter owner=starwhale|
|`latest`|Flag| Cloud Instance: 仅展示最新版本 <br/> Standalone Instance: 仅展示带有latest标签的版本 |--filter latest|

## 8. 删除模型包

```bash
swcli model remove [OPTIONS] MODEL
```

`model remove` 命令可以删除整个模型包或模型包中的某个版本。删除可以分为硬删除和软删除，默认为软删除，指定 `--force`
参数为硬删除。所有的软删除模型包在没有GC之前，都可以通过 `swcli model recover` 命令进行恢复。`MODEL` 参数为Model
URI，当没有指定版本时，会对整个模型包所有版本进行删除，若URI带有版本信息，则只会对该版本进行删除。软删除的模型包，可以通过 `swcli model list --show-removed`
命令查看。

|参数|参数别名|必要性|类型|默认值|说明|
|------|--------|-------|-----------|-----|-----------|
|`--force`|`-f`|❌|Boolean|False|强制删除，不可恢复|

## 9. 恢复软删除的模型包

```bash
swcli model recover [OPTIONS] MODEL
```

`model recover` 命令可以对软删除的模型包进行恢复。`MODEL` 参数为Model URI，若有版本信息则需要是完整的版本号，不支持简写。支持整个模型包的恢复和某个tag 模型包的恢复。其他参数如下：

|参数|参数别名|必要性|类型|默认值|说明|
|------|--------|-------|-----------|-----|-----------|
|`--force`|`-f`|❌|Boolean|False|强制恢复，处理类似恢复版本冲突的情况。|

## 10. 标记模型包

```bash
swcli model tag [OPTIONS] MODEL [TAGS]...
```

`model tag` 命令可以对模型包中的某个版本标记Tag，后续可以通过Tag对模型包进行操作。`MODEL` 参数为Model URI，需包含 `/version/{version id or tag}` 部分。 `TAGS` 参数可以指定一个或多个，为字符串格式。默认为追加标签，当指定 `--remove` 参数后，则为删除标签。 **`model tag`命令目前只能在standalone instance下执行**。

一个模型包中的某个版本可以指定零个或多个标签，一个标签在该模型包范围内，只能指向一个版本。当本地构建或从cloud instance下载某个版本的模型包时，swcli会提供auto-fast-tag机制，标记类似 `v1`, `v2` 这种自增的、简易使用的标签。当本地构建一个模型包的新版时，会自动标记 `latest` 标签。

|参数|参数别名|必要性|类型|默认值|说明|
|------|--------|-------|-----------|-----|-----------|
|`--remove`|`-r`|❌|Boolean|False|删除标签|
|`--quiet`|`-q`|❌|Boolean|False|忽略标签操作中的错误，例如删除不存在的标签，添加不合法的标签等|

## 11. 对比模型包差异

```bash
swcli model diff [OPTIONS] BASE_URI COMPARE_URI
```

`model diff` 命令提供同一个模型包中两个不同版本的对比。`BASE_URI` 为Base的Model URI，`COMPARE_URI` 为与Base对比的Model URI。

`model diff` 命令参数如下：

|参数|参数别名|必要性|类型|默认值|说明|
|------|--------|-------|-----------|-----|-----------|
|`--show-details`||❌|Boolean|False|指定该参数后会输出对比的详细信息，包括每行数据的差异，可能会输出较多内容。|

![model-diff.png](../../img/model-diff.png)
