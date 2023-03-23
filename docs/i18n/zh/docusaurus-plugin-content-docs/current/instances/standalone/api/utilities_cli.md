---
title: 全局管理命令
---

## 终端UI

```bash
swcli board
```

`board` 命令提供一种交互式的终端UI，该功能目前仍处于Preview阶段，后续会逐步完善。目前支持的键盘映射：

- `M` -> Starwhale Model
- `D` -> Starwhale Dataset
- `R` -> Starwhale Runtime
- `J` -> Evaluation Job
- `Ctrl+C` -> 退出Board界面

![board.gif](../../../img/board.gif)

## 垃圾回收

```bash
swcli gc [OPTIONS]
```

`gc` 命令能够对Standalone本地环境进行垃圾回收，目前主要是对软删除的Model、Runtime和Dataset做进一步清理。其他参数如下：

|参数|参数别名|必要性|类型|默认值|说明|
|------|--------|-------|-----------|-----|-----------|
|`--dry-run`||❌|Boolean|False|不做真正的清理，只是将待清理内容输出到终端。|
|`--yes`||❌|Boolean|False|所有待确认的输入都是Yes|

![gc.gif](../../../img/gc.gif)

## 快速打开UI界面

```bash
swcli ui [INSTANCE]
```

`ui` 命令可以拉起本地浏览器并进入到对应的Cloud Instance Web页面中。`INSTANCE` 参数为Instance URI，如果不指定该参数，则会使用 `swcli instance select` 选定的默认instance。

![open-ui.gif](../../../img/open-ui.gif)

## 命令提示补全

```bash
swcli completion show [[bash|zsh|fish]]
swcli completion install [[bash|zsh|fish]]
```

`completion show` 命令能输出bash|zsh|fish终端的补全命令，便于做检查。如果不指定shell类型，则会自动识别当前shell环境。

```bash
❯ swcli completion show
eval "$(_SWCLI_COMPLETE=zsh_source swcli)"
❯ swcli completion show zsh
eval "$(_SWCLI_COMPLETE=zsh_source swcli)"
❯ swcli completion show bash
eval "$(_SWCLI_COMPLETE=bash_source swcli)"
❯ swcli completion show fish
eval (env _SWCLI_COMPLETE=fish_source swcli)
```

`completion install` 命令能够对bash|zsh|fish终端安装swcli的补全命令。如果不指定shell类型，则会自动识别当前shell环境。执行完命令后，重新打开shell，就可以使用swcli的命令补全功能。

```bash
❯ swcli completion install
👏 swcli zsh completion installed in /home/liutianwei/.zshrc
🍺 run exec zsh command to activate shell completion
```

![completion.gif](../../../img/completion.gif)

## 环境检查

```bash
swcli check
```

`check` 命令能够检查swcli所在机器环境的外部依赖及其版本是否满足要求，如果不满足会提示相关错误及解决方法。下面例子中检测出来本地Docker并没有运行。

```bash
❯ swcli check
❌ Docker
         * 👉 Reason: exit code:1, command:b'20.10.13\nCannot connect to the Docker daemon at unix:///var/run/docker.sock. Is the docker daemon running?\n'
         * 📘 Min version: 19.03
         * 💁 Advice: Docker is an open platform for developing, shipping, and running applications.Starwhale uses Docker to run jobs. You can visit https://docs.docker.com/get-docker/ for more details.

✅ Conda 4.11.0
```
