---
title: 基本参数
---

```bash
swcli [OPTIONS] COMMAND [ARGS]...
```

:::note
`swcli`，`sw`和`starwhale`三个命令的作用是一样的。
:::

swcli 命令全局参数如下：

|参数|参数别名|说明|
|---|---|-----------|
|`--version`||显示swcli的版本信息。|
|`--verbose`|`-v`|日志中输出更多信息，当 `-v` 参数越多，呈现信息越多，最多支持4个 `-v` 参数。|
|`--output`|`-o`|目前支持 `-o json` 参数的写法，能以json方式输出某些命令的输出，便于通过管道方式做二次处理。|
|`--help`||输出命令帮助信息。|

**需要注意的是，全局参数需要跟在swcli之后，子命令之前。**

```bash
swcli -vvv model eval . --dataset  speech_commands_validation/version/small

swcli -o json dataset diff  mnist/version/gfrtmobxha3w  mnist/version/latest

swcli -o json model list
```

swcli支持子命令的前缀简写输入，如果遇到冲突会有相关提示：

```bash
swcli r -h
swcli ru -h
swcli run -h
swcli runt -h
```

上面四个命令等价于 `swcli runtime -h`。
