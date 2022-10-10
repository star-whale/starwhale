---
title: Runtime命令
---

## 基本信息

```bash
swcli runtime [OPTIONS] COMMAND [ARGS]...
```

runtime命令提供适用于Standalone Instance和Cloud Instance的Starwhale Runtime全生命周期管理，包括构建、分发、查看、环境复现等功能。通过runtime命令构建的Starwhale Runtime能与model、dataset、evaluation等命令深度集成，一键应用环境。在Standalone Instance中，runtime命令使用本地磁盘存储Runtime文件。runtime命令通过HTTP API对Cloud Instance对象进行操作。

**Runtime URI**格式：`[<Project URI>/runtime]<runtime name>[/version/<version id>]`。

runtime包含如下子命令：

|Command|Standalone|Cloud|
|-------|----------|-----|
|`activate`|✅|❌|
|`build`|✅|❌|
|`copy`|✅|✅|
|`dockerize`|✅|❌|
|`extract`|✅|❌|
|`history`|✅|✅|
|`info`|✅|✅|
|`list`|✅|✅|
|`lock`|✅|❌|
|`quickstart`|✅|❌|
|`recover`|✅|✅|
|`remove`|✅|✅|
|`restore`|✅|❌|
|`tag`|✅|❌|

## 激活Runtime

```bash
swcli runtime activate [OPTIONS]
```

`runtime activate` 命令支持bash/zsh/fish下直接激活Starwhale Runtime，起到类似virtualenv中 `source venv/bin/activate` 或 `conda activate xxx` 的作用。当关闭当前shell或切换到其他shell时，需要重新激活Runtime。`--uri` 和 `--path` 参数只能选择一个进行设置。
**`runtime activate`命令目前只能在standalone instance下执行**。如果对某个Runtime URI进行激活，需要确保该Runtime已经被重建，一般通过执行 `swcli runtime restore` 命令实现。

对于已经激活的Runtime，如果想要退出该环境，需要在venv mode中执行 `deactivate` 命令或conda mode中执行`conda deactivate` 命令。

该命令参数如下：

|参数|参数别名|必要性|类型|默认值|说明|
|------|--------|-------|-----------|-----|-----------|
|`--uri`|`-u`|❌|String||Standalone Instance下Runtime URI|
|`--path`|`-p`|❌|String||venv或conda目录路径|

## 构建Runtime

```bash
swcli runtime build [OPTIONS] WORKDIR
```

`runtime build` 命令在 `WORKDIR` 目录中寻找 `runtime.yaml` 文件，并以该文件的配置为起点开始构建Runtime。`WORKDIR`参数应为一个合法的路径字符串，`runtime.yaml` 中涉及文件路径的配置，都是相对于 `WORKDIR` 目录的。**`runtime build`命令目前只能在standalone instance下执行, 且不支持 `.swignore` 特性**。

`runtime build` 命令当设置 `--enable-lock` 参数时，会锁定当前环境的Python依赖信息到 `WORKDIR` 目录的 `requirements-sw-lock.txt` 文件中。"当前环境"指的是某个venv或conda的Python环境，可以通过 `--env-prefix-path` 或 `--env-name` 参数指定，这两个参数不能同时设置。若没有指定具体的环境，则会对当前shell环境进行依赖锁定。设置 `--enable-lock` 参数相当于先执行 `swcli runtime lock` , 再执行 `swcli runtime build` 命令。

`runtime build` 命令参数如下：

|参数|参数别名|必要性|类型|默认值|说明|
|------|--------|-------|-----------|-----|-----------|
|`--project`|`-p`|❌|String|`swcli project select`命令设定的project|Project URI|
|`--runtime-yaml`|`-f`|❌|String|runtime.yaml|建议使用默认的runtime.yaml，无需修改。|
|`--gen-all-bundles`||❌|Boolean|False|设置该参数后，会在本地的venv或conda中下载所有python依赖，进行整体打包，多数情况下产生的Runtime swrt文件会比较大。|
|`--include-editable`||❌|Boolean|False|是否携带editable的python package|
|`--enable-lock`||❌|Boolean|False|设置该参数后，会在build开始时锁定当前环境的Python依赖，生成或更新`requirements-sw-lock.txt`文件。|
|`--env-prefix-path`||❌|String||设定 `--enable-lock` 参数后，锁定venv或conda Python环境的路径|
|`--env-name`||❌|String||设定 `--enable-lock` 参数后, 锁定conda环境的env名字|

## 分发Runtime

```bash
swcli runtime copy [OPTIONS] SRC DEST
```

`runtime copy` 命令能对构建好的Starwhale Runtime实现分发，既可以从Standalone Instance上传Runtime到Cloud Instance，又可以从Cloud Instance上下载Runtime到本地的Standalone Instance，目前不支持Cloud A 到 Cloud B， Standalone A到Standalone B这种层面的Starwhale Runtime分发。`SRC` 参数为 `Runtime URI` 格式，`DEST` 参数为 `Project URI`格式。

`runtime copy` 命令参数如下：

|参数|参数别名|必要性|类型|默认值|说明|
|------|--------|-------|-----------|-----|-----------|
|`--force`|`-f`|❌|Boolean|False|`DEST` 存在相同version的Runtime，指定该参数后执行copy命令就会强制覆盖。|

## 制作Runtime的Docker Image

```bash
swcli runtime dockerize [OPTIONS] URI
```

`runtime dockerize` 命令能够根据Runtime生成Dockerfile和Docker Image，目前**该命令只能在standalone instance下执行**。swcli使用docker buildx构建镜像，需要保证本地安装Docker并且版本不早于19.03，可以使用 `swcli check` 命令对本机环境做检查。`URI` 参数为Runtime URI。

`runtime dockerize` 命令指定的时候支持获取当前shell中的HTTP_PROXY/HTTPS_PROXY环境变量，并设置到容器内部环境中，解决由于网络问题导致某些包无法下载的问题。生成的Dockerfile路径为Runtime URI所对应的${snapshot_workdir}/export/docker/Dockerfile。

|参数|参数别名|必要性|类型|默认值|说明|
|------|--------|-------|-----------|-----|-----------|
|`--tag`|`-t`|❌|String||Docker的tag，该参数可以指定多次|
|`--push`||❌|Bool|False|是否push image到远端docker registry|
|`--platform`||❌|Choice|amd64|支持多体系结构Image的构建，目前可选amd64和arm64，支持多次指定，同时构建多种体系结构的Image|
|`--dry-run`||❌|Bool|False|不真正运行，只生成Dockerfile和打印docker构建命令|
|`--use-starwhale-builder`||❌|Bool|False|创建一个支持多体系结构的starwhale buildx builder，用户也可以使用自己的builder，具体参考buildx相关命令|
|`--reset-qemu-static`||❌|Bool|False|重置QEMU的配置，一般用来修复多体系结构构建失败问题|

![runtime-dockerize.png](../../img/runtime-dockerize.png)

## 解压Runtime包文件

```bash
swcli runtime extract [OPTIONS] RUNTIME
```

`runtime extract` 命令可以解压Starwhale Runtime的 `swrt` 格式文件，查看原始Runtime文件内容，目前**该命令只能在standalone instance下执行**。`RUNTIME` 参数为Runtime URI，其他参数如下：

|参数|参数别名|必要性|类型|默认值|说明|
|------|--------|-------|-----------|-----|-----------|
|`--force`|`-f`|❌|Boolean|False|若曾经在target-dir目录中extract过，设定该参数后，会强制在target-dir目录覆盖旧的内容|
|`--target-dir`||❌|String|Runtime URI对应的snapshot_workdir目录|解压Runtime后存储相关内容的目录|

## 查看Runtime历史版本

```bash
swcli runtime history [OPTIONS] RUNTIME
```

`dataset history` 命令输出Runtime所有未删除的版本信息。`RUNTIME` 参数为Runtime URI，其他参数如下：

|参数|参数别名|必要性|类型|默认值|说明|
|------|--------|-------|-----------|-----|-----------|
|`--fullname`||❌|Boolean|False|显示完整的版本信息，默认只显示版本号的前12位。|

## 查看Runtime详细信息

```bash
swcli runtime info [OPTIONS] RUNTIME
```

`runtime info` 命令输出Runtime或Runtime中某个版本的详细信息。`RUNTIME` 参数为Runtime URI，其他参数如下：

|参数|参数别名|必要性|类型|默认值|说明|
|------|--------|-------|-----------|-----|-----------|
|`--fullname`||❌|Boolean|False|显示完整的版本信息，默认只显示版本号的前12位。|

## 展示Runtime列表

```bash
swcli runtime list [OPTIONS]
```

`runtime list` 命令输出当前选定的instance和project下的所有Runtime及相关版本。命令参数如下：

|参数|参数别名|必要性|类型|默认值|说明|
|------|--------|-------|-----------|-----|-----------|
|`--project`|`-p`|❌|String|`swcli project select`命令选定的默认project|Project URI|
|`--fullname`||❌|Boolean|False|显示完整的版本信息，默认只显示版本号的前12位。|
|`--show-removed`||❌|Boolean|False|显示本地已经删除但能恢复的Runtime。|
|`--page`||❌|Integer|1|Cloud Instance中分页显示中page序号。|
|`--size`||❌|Integer|20|Cloud Instance中分页显示中每页数量。|

## 锁定Python依赖信息

```bash
swcli runtime lock [OPTIONS] [TARGET_DIR]
```

`runtime lock` 命令能将指定的Python环境中的依赖进行导出，在 `TARGET_DIR` 目录中生成 `requirements-sw-lock.txt` 文件，典型内容形式如下：

```txt
# Generated by Starwhale(0.3.0) Runtime Lock
--index-url 'https://pypi.tuna.tsinghua.edu.cn/simple'
--extra-index-url 'https://mirrors.bfsu.edu.cn/pypi/web/simple'
--trusted-host 'pypi.tuna.tsinghua.edu.cn mirrors.bfsu.edu.cn'
appdirs==1.4.4
attrs==21.4.0
boto3==1.21.0
botocore==1.24.46
...
```

lock的内容会包含所有Python的依赖及详细版本、本机使用的pip config内容等，使用lock来管理依赖是推荐的方式，能保证环境的稳定性。`TARGET_DIR` 参数是lock产生文件的存储目录，如果不写，则会存储到当前目录。lock支持对当前shell环境、某个路径(`--prefix` 参数)的venv环境、某个路径(`--prefix` 参数)或具体env name(`--name` 参数)的conda环境进行Python依赖导出。

lock命令一般用在将一个本地已经调试好的venv或conda环境固化的场景，目前**该命令只能在standalone instance下执行**。

`runtime lock` 命令其他参数如下：

|参数|参数别名|必要性|类型|默认值|说明|
|------|--------|-------|-----------|-----|-----------|
|`--yaml-name`||❌|String|runtime.yaml|lock后会自动将 `requirements-sw-lock.txt` 注入到runtime yaml文件的dependencies字段中。默认会寻找 `TARGET_DIR` 目录中的 runtime.yaml 文件，也可以指定其他runtime yaml 文件名字。|
|`--disable-auto-inject`||❌|Boolean|False|若设置该参数，则不会自动注入`requirements-sw-lock.txt` 到runtime.yaml中。|
|`--name`|`-n`|❌|String||Conda env的名称|
|`--prefix`|`-p`|❌|String||Conda或Venv的目录Prefix|
|`--stdout`||❌|Boolean|False|将lock的内容只在shell终端中输出，而不会真正写入到 `requirements-sw-lock.txt` 文件中，一般用来做调试检查。|
|`--include-editable`||❌|Boolean|False|lock时候是否包含editable的Python Package，某些editable的package可能使用本地目录，会在分发到其他环境后无法使用。目前该参数只针对venv场景，conda的无法导出editable package。|
|`--emit-pip-options`||❌|Boolean|False|是否忽略lock文件中pip config信息，一般包含index-url、extra-index-url和trusted-host等。|

![runtime-lock.gif](../../img/runtime-lock.gif)

## 快速创建全新的Runtime

目前**runtime quickstart只能在standalone instance下执行**。

### 在终端中交互式产生新的Runtime

```bash
swcli runtime quickstart shell [OPTIONS] WORKDIR
```

`runtime quickstart shell` 命令可以在终端shell中交互式或参数指定方式快速生成一个Runtime模板，主要是在 `WORKDIR` 目录中产生一个runtime.yaml，便于做后续定制修改。如果不指定任何参数，则交互式的提示用户输入相关内容。其他参数如下：

|参数|参数别名|必要性|类型|默认值|说明|
|------|--------|-------|-----------|-----|-----------|
|`--force`|`-f`|❌|Boolean|False|若曾经在WORKDIR目录中quickstart过，设定该参数后，会强制在WORKDIR目录覆盖旧的内容|
|`--name`|`-n`|❌|String|父目录名称|runtime的名称|
|`--python-env`|`-p`|❌|Choice|venv|runtime的mode：venv或conda|
|`--create-env`|`-c`|❌|Boolean|False|是否直接创建隔离的venv或conda环境，并安装基础软件包|
|`--interactive`|`-i`|❌|Boolean|False|若设置`--create-env`后，指定interactive参数，会尝试直接激活该环境|

![runtime-quickstart.gif](../../img/quickstart-shell.gif)

### 以某个已经存在的Runtime为基础创建新的Runtime

```bash
swcli runtime quickstart uri [OPTIONS] URI WORKDIR
```

`runtime quickstart uri` 命令能从某个Standalone Instance上已经存在的Runtime URI为基础，继承相关配置和文件，在 `WORKDIR` 中创建一个全新的Runtime，包括runtime.yaml、必要文件和隔离的Python环境等，用户可以定制其内容，然后在构建和发布这个Runtime。`URI` 参数为Runtime URI，目前支持Standalone Instance上的Runtime，Cloud Instance的Runtime需要先copy到本地然后再使用。`WORKDIR` 参数为合法的目录路径。其他参数如下：

|参数|参数别名|必要性|类型|默认值|说明|
|------|--------|-------|-----------|-----|-----------|
|`--force`|`-f`|❌|Boolean|False|若曾经在WORKDIR目录中quickstart过，设定该参数后，会强制在WORKDIR目录覆盖旧的内容|
|`--name`|`-n`|❌|String|父目录名称|runtime的名称|
|`--restore`||❌|Boolean|False|是否重建Runtime的Python依赖，包括创建venv或conda环境，并下载安装runtime.yaml的依赖|

## 删除Runtime

```bash
swcli runtime remove [OPTIONS] RUNTIME
```

`runtime remove` 命令可以删除整个Runtime或Runtime中的某个版本。删除可以分为硬删除和软删除，默认为软删除，指定 `--force` 参数为硬删除。所有的软删除Runtime在没有GC之前，都可以通过 `swcli runtime recover` 命令进行恢复。`RUNTIME` 参数为Runtime URI，当没有指定版本时，会对整个Runtime所有版本进行删除，若URI带有版本信息，则只会对该版本进行删除。软删除的Runtime，可以通过 `swcli runtime list --show-removed` 命令查看。

|参数|参数别名|必要性|类型|默认值|说明|
|------|--------|-------|-----------|-----|-----------|
|`--force`|`-f`|❌|Boolean|False|强制删除，不可恢复|

## 恢复软删除的Runtime

```bash
swcli runtime recover [OPTIONS] RUNTIME
```

`runtime recover` 命令可以对软删除的Runtime进行恢复。`RUNTIME` 参数为Runtime URI，若有版本信息则需要是完整的版本号，不支持简写。支持整个runtime的恢复和某个tag runtime的恢复。其他参数如下：

|参数|参数别名|必要性|类型|默认值|说明|
|------|--------|-------|-----------|-----|-----------|
|`--force`|`-f`|❌|Boolean|False|强制恢复，处理类似恢复版本冲突的情况。|

## 重建Runtime

```bash
swcli runtime restore [OPTIONS] TARGET
```

`runtime restore` 命令可以根据 `TARGET` 参数信息创建venv或conda的Python隔离环境，并下载和安装Runtime所描述的Python依赖。**该命令目前只能在standalone instance下执行**。restore操作用时取决于所在机器的网络情况，可以通过合理设置pip.conf或代理提升安装速度。

`TARGET` 参数有两种形式，一种是表示某个 runtime uri的snapshot_workdir， 另一种是某个Standalone Instance下的Runtime URI。

## 标记Runtime

```bash
swcli runtime tag [OPTIONS] RUNTIME [TAGS]...
```

`runtime tag` 命令可以对Runtime中的某个版本标记Tag，后续可以通过Tag对Runtime进行操作。`RUNTIME` 参数为Runtime URI，需包含 `/version/{version id or tag}` 部分。 `TAGS` 参数可以指定一个或多个，为字符串格式。默认为追加标签，当指定 `--remove` 参数后，则为删除标签。 **`runtime tag`命令目前只能在standalone instance下执行**。

一个Runtime中的某个版本可以指定零个或多个标签，一个标签在该Runtime范围内，只能指向一个版本。当本地构建或从cloud instance下载某个版本的Runtime时，swcli会提供auto-fast-tag机制，标记类似 `v1`, `v2` 这种自增的、简易使用的标签。当本地构建一个Runtime的新版时，会自动标记 `latest` 标签。

|参数|参数别名|必要性|类型|默认值|说明|
|------|--------|-------|-----------|-----|-----------|
|`--remove`|`-r`|❌|Boolean|False|删除标签|
|`--quiet`|`-q`|❌|Boolean|False|忽略标签操作中的错误，例如删除不存在的标签，添加不合法的标签等|
