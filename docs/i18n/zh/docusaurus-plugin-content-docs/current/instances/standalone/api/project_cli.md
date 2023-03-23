---
title: Project命令
---

## 基本信息

```bash
swcli project [OPTIONS] COMMAND [ARGS]...
```

project命令提供适用于Standalone Instance和Cloud Instance的Starwhale Project全生命周期的管理，包括创建、查看、选择默认Project等功能。在Standalone Instance中，project 代表在 ROOTDIR下的一个目录，里面存储Runtime、Model、Dataset、Job等信息，ROOTDIR默认路径是 `~/.starwhale` 。project命令通过HTTP API对Cloud Instance对象进行操作。

**Project URI**格式: `[<Instance URI>/project]<project name>`。

project包含如下子命令：

|命令| 别名      |Standalone|Cloud|
|----|---------|----------|-----|
|create| new,add |✅|✅|
|info|| ✅       |✅|
|list| ls      |✅|✅|
|remove| rm      |✅|✅|
|recover|| ✅       |✅|
|select| use     |✅|✅|

## 创建Project

```bash
swcli project create PROJECT
```

`project create` 命令能够创建一个新的Project，`PROJECT` 参数为Project URI。

```bash
❯ swcli project create myproject
👏 do successfully
❯ swcli project create myproject
🤿 failed to run, reason:/home/liutianwei/.cache/starwhale/myproject was already existed
```

## 查看Project详细信息

```bash
swcli project info PROJECT
```

`project info` 命令输出Project详细信息。`PROJECT` 参数为Project URI。

## 展示Project列表

```bash
swcli project list [OPTIONS]
```

`project list` 命令输出当前选定Instance的Project列表，命令参数如下：

|参数|参数别名|必要性|类型|默认值|说明|
|------|--------|-------|-----------|-----|-----------|
|`--instance`|`-i`|❌|String|`swcli instance select` 选定的Instance|Instance URI|
|`--page`||❌|Integer|1|Cloud Instance中分页显示中page序号。|
|`--size`||❌|Integer|20|Cloud Instance中分页显示中每页数量。|

## 删除Project

```bash
swcli project remove PROJECT
```

`project remove` 命令软删除Project，在没有GC之前，都可以通过 `swcli project recover` 命令进行恢复。`PROJECT` 参数为Project URI。

```bash
❯ swcli project remove myproject
🐶 remove project myproject. You can recover it, don't panic.
```

## 恢复软删除的Project

```bash
swcli project recover PROJECT
```

`project recover` 命令恢复软删除的Project。`PROJECT` 参数为Project URI。

```bash
❯ swcli project recover myproject
👏 recover project myproject
```

## 选择当前Instance下默认的Project

```bash
swcli project select PROJECT
```

`project select` 命令选择当前Instance下默认的Project，设置后如果省略Model URI、Runtime URI、Dataset URI中的project 字段，就会根据Instance对应的默认Project进行填充。`PROJECT` 参数为Project URI。对于Cloud Instance，需要首选登陆Instance才能进行project select。

```bash
❯ swcli project select local/project/self
👏 select instance:local, project:self successfully
```
