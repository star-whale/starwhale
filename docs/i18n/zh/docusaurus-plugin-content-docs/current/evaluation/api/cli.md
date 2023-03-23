---
title: 模型评测命令
---

## 基本信息

```bash
swcli [GLOBAL OPTIONS] eval [OPTIONS] COMMAND [ARGS]...
```

eval命令提供适用于Standalone Instance和Cloud Instance的模型评测管理，包括运行、查看等管理功能。在Standalone Instance中，eval命令使用本地磁盘存储模型评测过程和结果文件。eval命令通过HTTP API对Cloud Instance对象进行操作。

**Evaluation URI** 格式: `[<Project URI>/evaluation]<evaluation id>`.

eval命令包含如下子命令：

|命令|别名|Standalone|Cloud|
|---|---|----------|-----|
|run||✅|✅|
|compare|cmp|✅|❌|
|info||✅|✅|
|list|ls|✅|✅|
|remove|rm|✅|✅|
|recover||✅|✅|
|pause||✅|✅|
|cancel||✅|✅|
|resume||✅|✅|

## 评测模型

```bash
swcli eval run [OPTIONS]
```

`eval run` 命令可以进行模型评测，功能上与 `swcli model eval` 命令大体相同，区别如下：
    - `eval run` 命令可以提交任务到Cloud Instance上。
    - 在Standalone Instance运行的时候，`eval run` 命令可以选用docker为载体进行模型评测，能有效解决类似Cuda等底层基础库的依赖问题，与Cloud Instance上运行的任务基础环境一致。

`eval run` 命令参数如下：

|参数|参数别名|必要性|类型|默认值|说明|
|---|-------|-----|---|------|---|
|`--version`||❌|String|生成一个随机ID|模型评测的版本号，可以使用该参数进行分Step调试。该参数也可以通过 `SW_EVALUATION_VERSION` 环境变量来设置。|
|`--step`||❌|String||评测任务的Step名称，实现只执行该Step的目的。如果不指定，则所有Step都会运行，对于继承 `starwhale.PipelineHandler` 的评测任务，就是ppl和cmp两个Step。|
|`--task-index`||❌|Integer|| `--task-index` 参数用来表明 `--step` 中Step的第n个Task会执行，如果不指定 `--task-index`，则该Step中所有Tasks都会执行。Task索引从0开始，会通过 `starwhale.Context` 传入用户程序中，很多场景下Task的索引值用来指导评测数据集如何分割，实现评测的时候同一个Step的不同Task处理不同部分的评测数据集，实现并行提速。另外需要注意的是，`--task-index` 只有在同时设置 `--step` 参数时才生效。|
|`--model`||✅||String||Model URI或model.yaml所在的目录。如果在Cloud Instance上运行，支持Model URI形式。|
|`--dataset`||✅||String||Dataset URI，该参数也可以通过 `SW_DATASET_URI` 环境变量来设置。|
|`--runtime`||❌|String||`--runtime`参数为Standalone Instance中的Runtime URI。若设置，则表示运行模型评测的时候会使用该Runtime提供的运行时环境；若不设置，则使用当前shell环境作为运行时。设置`--runtime`参数是安全的，只在运行时才会使用Runtime，不会污染当前shell环境。|
|`--name`||❌|String|default|任务运行的名称|
|`--desc`||❌|String||任务运行的描述|
|`--gencmd`||❌|Boolean|False|当选用设置 `--use-docker` 参数后，只输出docker run的命令，不真正运行。该参数只能在Standalone Instance中使用。|
|`--use-docker`||❌|Boolean|False|选用docker为载体的Runtime来运行模型评测过程，该参数只能在Standalone Instance中使用。|

## 对比模型评测结果

```bash
swcli eval compare BASE_JOB [JOB]...
```

`eval compare` 命令能支持两个或多个模型评测结果进行对比。`BASE_JOB` 参数和 `JOB` 参数为Evaluation URI。`JOB` 参数支持一个或多个。

![eval-compare.png](../../img/eval-compare.png)

## 查看模型评测结果

```bash
swcli eval info [OPTIONS] JOB
```

`eval info` 命令可以查看模型评测结果，展示的内容与cmp阶段通过evaluation log记录的信息有关。`JOB` 参数为Evaluation URI。其他参数如下：

|参数|参数别名|必要性|类型|默认值|说明|
|------|--------|-------|-----------|-----|-----------|
|`--page`||❌|Integer|1|Cloud Instance中分页显示中page序号。|
|`--size`||❌|Integer|20|Cloud Instance中分页显示中每页数量。|

## 展示模型评测列表

```bash
swcli eval list [OPTIONS]
```

`eval list` 命令输出当前选定的instance和project下的所有模型评测结果。命令参数如下：

|参数|参数别名|必要性|类型|默认值|说明|
|------|--------|-------|-----------|-----|-----------|
|`--project`|`-p`|❌|String|`swcli project select`命令选定的默认project|Project URI|
|`--fullname`||❌|Boolean|False|显示完整的版本信息，默认只显示版本号的前12位。|
|`--show-removed`||❌|Boolean|False|显示本地已经删除但能恢复的模型评测结果。|
|`--page`||❌|Integer|1|Cloud Instance中分页显示中page序号。|
|`--size`||❌|Integer|20|Cloud Instance中分页显示中每页数量。|

## 删除模型评测结果

```bash
swcli eval remove [OPTIONS] JOB
```

`eval remove` 命令可以删除模型评测结果。删除可以分为硬删除和软删除，默认为软删除，指定 `--force` 参数为硬删除。所有的软删除模型评测结果在没有GC之前，都可以通过 `swcli eval recover` 命令进行恢复。`Job` 参数为Evaluation URI。软删除的模型包，可以通过 `swcli eval list --show-removed` 命令查看。

|参数|参数别名|必要性|类型|默认值|说明|
|------|--------|-------|-----------|-----|-----------|
|`--force`|`-f`|❌|Boolean|False|强制删除，不可恢复|

## 恢复软删除的模型评测结果

```bash
swcli eval recover [OPTIONS] JOB
```

`eval recover` 命令可以对软删除的模型评测结果进行恢复。`Job` 参数为Evaluation URI。其他参数如下：

|参数|参数别名|必要性|类型|默认值|说明|
|------|--------|-------|-----------|-----|-----------|
|`--force`|`-f`|❌|Boolean|False|强制恢复，处理类似恢复版本冲突的情况。|

## 暂停模型评测任务的运行

```bash
swcli eval pause [OPTIONS] JOB
```

`eval pause` 命令能对运行中评测任务进行暂停。`Job` 参数为Evaluation URI。暂停的任务可以通过 `swcli eval resume` 命令进行恢复。在Standalone Instance中，只能暂停以docker为运行载体的评测任务。其他参数如下：

|参数|参数别名|必要性|类型|默认值|说明|
|------|--------|-------|-----------|-----|-----------|
|`--force`|`-f`|❌|Boolean|False|强制暂停|

## 取消模型评测任务的运行

```bash
swcli eval cancel [OPTIONS] JOB
```

`eval cancel` 命令能对运行中评测任务进行取消。`Job` 参数为Evaluation URI。在Standalone Instance中，只能取消以docker为运行载体的评测任务。其他参数如下：

|参数|参数别名|必要性|类型|默认值|说明|
|------|--------|-------|-----------|-----|-----------|
|`--force`|`-f`|❌|Boolean|False|强制取消|

## 恢复模型评测任务的运行

```bash
swcli eval resume [OPTIONS] JOB
```

`eval pause` 命令能对暂停的评测任务进行恢复运行。`Job` 参数为Evaluation URI。在Standalone Instance中，只能恢复以docker为运行载体的评测任务。其他参数如下：

|参数|参数别名|必要性|类型|默认值|说明|
|------|--------|-------|-----------|-----|-----------|
|`--force`|`-f`|❌|Boolean|False|强制恢复|
