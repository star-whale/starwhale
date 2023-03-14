---
title: Starwhale Resources URI定义
---

:::tip
**Starwhale Resources URI广泛使用在swcli中，通过URI机制，能够非常容易的指向Standalone Instance或Cloud Instance某个资源，并对其进行操作**。
:::

![concepts-org.jpg](../../../img/concepts-org.jpg)

## 1. Instance URI

Instance URI 可以指向某一个Starwhale的部署实例，格式如下：

- `local`：Standalone Instance。
- `[http(s)://]<hostname or ip>[:<port>]`：Cloud Instance，使用http(s)地址访问。
- `[cloud://]<cloud alias>`：Cloud Instance，使用`swcli instance login`阶段设置的instance别名访问。

:::caution
需要注意的是，在Instance URI 中 `local` 并不等同于 `localhost`。`local` 表示本机的Standalone Instance，`localhost` 表示Cloud Instance Controller使用的是本地回环网络地址，并使用默认端口8082。
:::

```bash
# login http://console.pre.intra.starwhale.ai instance, the alias is pre-k8s
swcli instance login --username starwhale --password abcd1234 http://console.pre.intra.starwhale.ai --alias pre-k8s
# copy model from the local instance, default project into cloud instance, instance field uses the alias name: pre-k8s.
swcli model copy mnist/version/latest cloud://pre-k8s/project/1
# copy runtime into cloud instance: localhost:8081
swcli runtime copy pytorch/version/v1.0 http://localhost:8081/project/myproject
```

## 2. Project URI

Project URI格式为：`[<Instance URI>/project/]<project name>`。如果Instance URI字段没有指定，则使用 `swcli instance select` 设置的默认Instance。

```bash
swcli project select self   # select self project in the current instance
swcli project info local/project/self  # inspect self project info in the local instance
```

## 3. Model/Dataset/Runtime URI

- Model URI格式： `[<Project URI>/model/]<model name>[/version/<version id|tag>]`
- Dataset URI格式： `[<Project URI>/dataset/]<dataset name>[/version/<version id|tag>]`
- Runtime URI格式: `[<Project URI>/runtime/]<runtime name>[/version/<version id|tag>]`

`swcli` 支持不少于5位字符的version id简写表达，但目前 `swcli model recover`，`swcli dataset recover` 和 `swcli runtime recover` 命令只能使用完整的version id 进行资源软删除恢复。如果Project URI没有指定，则使用 `swcli project select` 设置的默认Project。

```bash
swcli model info mnist/version/hbtdenjxgm4ggnrtmftdgyjzm43tioi  # inspect model info, model name: mnist, version:hbtdenjxgm4ggnrtmftdgyjzm43tioi
swcli model remove mnist/version/hbtdenj  # short version
swcli model info mnist  # inspect mnist model info
swcli eval run --model mnist/version/latest --runtime pytorch-mnist/version/latest --dataset mnist/version/latest
```

## 4. Evaluation URI

Evaluation URI的格式为: `[<Project URI>/evaluation/]<job id>`。如果Project URI没有指定，则使用 `swcli project select` 设置的默认Project。`swcli` 支持不少于5位字符的job id简写表达，但目前 `swcli eval recover` 命令只能使用完整的job id进行资源软删除恢复。

```bash
swcli eval info mezdayjzge3w   # Inspect mezdayjzge3w version in default instance and default project
swcli eval info local/project/self/job/mezday # Inspect the local instance, self project, with short job version:mezday
```

## 5. 命名规范约定

Project名称，Model名称，Dataset名称，Runtime名称和所有资源Tag有如下的命名规范约定：

- 大小写不敏感。
- 名称只能包含数字（`0-9`）、字母（`A-Z a-z`），中划线（`-`），下划线（`_`）和点符号（`.`）。
- 名称首字母只能是字母（`A-Z a-z`）和下划线（`_`）。
- 名称长度最长不超过80个字符。

### 5.1 名称唯一性要求

- **资源名称在其所有者的作用域中需要保证唯一性**，比如Project名称在所对应的Instance中唯一，Model名称在其所属的Project中保持唯一，不能在Project "apple"中出现两个Model都叫"Alice"。
- 两个不同类型的资源可以重名，比如Project和Model名称都叫"Alice"。
- 在不同作用域下的两个相同类型的资源可以重名，比如Project "apple"中和Project "banana"中可以都有叫“Alice”的Model。
- 在同一个作用域下的两种不同资源可以重名，比如同一个Project下，可以出现Model和Dataset都叫“Alice”。
- 当软删除资源的时候，资源名称是不会被释放的。只有硬删除或GC后，才能在对应作用中释放相应的资源名称。
