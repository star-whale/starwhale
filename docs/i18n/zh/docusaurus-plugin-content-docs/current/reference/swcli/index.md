---
title: 概述
---

## 使用方式

```bash
swcli [选项] <COMMAND> [参数]...
```

:::note
`swcli`，`sw`和`starwhale`三个命令的作用是一样的。
:::

## 全局选项 {#global-option}

| 选项 | 说明 |
| --- | --- | ----------- |
| `--version` | 显示swcli的版本信息。 |
| `--verbose`或`-v` | 日志中输出更多信息，当 `-v` 参数越多，呈现信息越多，最多支持4个 `-v` 参数。 |
| `--help` | 输出命令帮助信息。 |

:::caution
需要注意的是，全局参数需要跟在swcli之后，命令之前。
:::

## 命令

* [`swcli instance`](./instance.md)
* [`swcli project`](./project.md)
* [`swcli model`](./model.md)
* [`swcli dataset`](./dataset.md)
* [`swcli runtime`](./runtime.md)
* [`swcli job`](./job.md)
* [`swcli gc`](./utilities.md#gc)
