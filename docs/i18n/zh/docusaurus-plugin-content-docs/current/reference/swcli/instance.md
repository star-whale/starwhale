---
title: swcli instance
---

## 概述

```bash
swcli [全局选项] instance [选项] <SUBCOMMAND> [参数]
```

`instance`命令包括以下子命令：

* `info`
* `list`
* `login`
* `logout`
* `use`

## swcli instance info {#info}

```bash
swcli [全局选项] instance info [选项] <INSTANCE>
```

`instance info`输出指定Starwhale实例的详细信息。

`INSTANCE`是一个[实例URI](../../swcli/uri.md#instance)。

## swcli instance list {#list}

```bash
swcli [全局选项] instance list [选项]
```

`instance list`显示所有的Starwhale实例。

## swcli instance login {#login}

```bash
swcli [全局选项] instance login [选项] <INSTANCE>
```

`instance login`连接到一个Server/Cloud实例并将它设置为[默认实例](../../swcli/uri.md#defaultInstance).

`INSTANCE`是一个[实例URI](../../swcli/uri.md#instance)。

| 选项 | 必填项 | 类型 | 默认值 | 说明 |
| --- | --- | --- | --- | --- |
| `--username` | ❌ | String | | 登录用户名 |
| `--password` | ❌ | String | | 登录密码 |
| `--token` | ❌ | String | | 登录令牌 |
| `--alias` | ✅ | String | | 实例别名。您可以在任何需要实例URI的地方使用对应的别名替代。 |

`--username`和`--password`不能和`--token`一起使用。

## swcli instance logout {#logout}

```bash
swcli [全局选项] instance logout [INSTANCE]
```

`instance logout`断开和Server/Cloud实例的连接并清除本地保存的信息。

`INSTANCE`是一个[实例URI](../../swcli/uri.md#instance)。如果不指定，将使用[默认实例](../../swcli/uri.md#defaultInstance)。

## swcli instance use {#use}

```bash
swcli [全局选项] instance use <INSTANCE>
```

`instance use`将指定的实例设置为[默认实例](../../swcli/uri.md#defaultInstance).

`INSTANCE`是一个[实例URI](../../swcli/uri.md#instance)。
