---
title: Commands for Starwhale Runtime
---

## Overview

```bash
swcli [GLOBAL OPTIONS] model [OPTIONS] <SUBCOMMAND> [ARGS]...
```

The runtime command manages Starwhale Runtimes, including build, list, copy, etc.

To reference a runtime in SWCLI, you can use the [Runtime URI](../../swcli/uri.md#model-dataset-runtime).

The `runtime` command includes the following subcommands:

| Subcommmand | Standalone | Cloud |
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
swcli [GLOBAL OPTIONS] runtime activate [OPTIONS] URI
```

Like `source venv/bin/activate` or `conda activate xxx`, `runtime activate` setups a new python environment according to the settings of the specified runtime. When the current shell is closed or switched to another shell, you need to reactivate the runtime.

**`runtime activate` only works for the [standalone instance](../../instances/standalone/index.md).** When the 'activate' command is used to activate an environment, it will check if the environment corresponding to the Runtime URI has been locally built. If not, it will automatically create a 'venv' or 'conda' environment, and download the Python dependencies for the corresponding Runtime.

If you want to quit the activated runtime environment, please run `venv deactivate` for the venv environment or `conda deactivate` in the conda environment.

| Option | Required | Type | Defaults | Description |
| --- | --- | --- | --- | --- |
|`--force-restore`|`-f`|❌|Bool|False|Force to restore runtime into the related snapshot workdir even the runtime has been restored|

## swcli runtime build {#build}

```bash
swcli [GLOBAL OPTIONS] runtime build [OPTIONS] <WORKDIR>
```

`runtime build` looks for `runtime.yaml` in `WORKDIR` and creates a new Starwhale Runtime.

**`runtime build` only works for the [standalone instance](../../instances/standalone/index.md).**

| Option | Required | Type | Defaults | Description |
| --- | --- | --- | --- | --- |
| `--project` or `-p` | ❌ | String | [the default project](../../swcli/uri.md#defaultProject) | the project URI |
| `--include-editable` or `-ie` | ❌ | Boolean | False | 是否携带editable的python package |
| `--include-local-wheel` or `-ilw` | ❌ | Boolean | False | 是否在requirements-sw-lock.txt文件中包含本地的wheel包地址，仅对venv模式有效。 |
| `--disable-env-lock` or `-del` | ❌ | Boolean | False | 设置该参数后，不会进行环境初始化、安装依赖和环境导出，直接进行swrt打包。 |
| `--env-prefix-path` or `-ep` | ❌ | String |  | 使用venv或conda目录prefix path作为Python隔离环境 |
| `--env-name` or `-en` | ❌ | String |  | 使用conda环境的env name作为Python隔离环境 |
| `--env-use-shell` or `-es` | ❌ | Boolean | False | 使用当前Shell中Python环境 |
| `--no-cache` or `-nc` | ❌ | Boolean | False | 对于自动生成的Python隔离依赖环境，构建runtime时不使用已经安装过的Python Packages，相当于清除.starwhale/venv或.starwhale/conda目录后再安装依赖然后构建环境 |

## swcli runtime copy {#copy}

```bash
swcli [GLOBAL OPTIONS] runtime copy [OPTIONS] <SRC> <DEST>
```

`runtime copy` copies from `SRC` to `DEST`. `SRC` and `DEST` are both runtime URIs.

| Option | Required | Type | Defaults | Description |
| --- | --- | --- | --- | --- |
|`--force` or `-f`| ❌ | Boolean | False | If true, `DEST` will be overwritten if it exists; otherwise, this command displays an error message. |

## swcli runtime dockerize {#dockerize}

```bash
swcli [GLOBAL OPTIONS] runtime dockerize [OPTIONS] <RUNTIME>
```

`runtime dockerize` generates a docker image based on the specified runtime. Starwhale uses `docker buildx` to create the image. Docker 19.03 or later is required to run this command.

| Option | Required | Type | Defaults | Description |
| --- | --- | --- | --- | --- |
| `--tag` or `-t` | ❌ | String |  | The tag of the docker image. This option can be repeated multiple times. |
| `--push` | ❌ | Boolean | False | If true, push the image to the docker registry |
| `--platform` | ❌ | String | amd64 | The target platform，can be either amd64 or arm64. This option can be repeated multiple times to create a multi-platform image. |

![runtime-dockerize.png](../../img/runtime-dockerize.png)

**`runtime dockerize` only works for the [standalone instance](../../instances/standalone/index.md).**

## swcli runtime history {#history}

```bash
swcli [GLOBAL OPTIONS] runtime history [OPTIONS] <RUNTIME>
```

The `runtime history` command outputs all history versions of the specified Starwhale Runtime.

| Option | Required | Type | Defaults | Description |
| --- | --- | --- | --- | --- |
| `--fullname` | ❌ | Boolean | False | Show the full version name. Only the first 12 characters are shown if this option is false. |

## swcli runtime info {#info}

```bash
swcli [GLOBAL OPTIONS] runtime info [OPTIONS] RUNTIME
```

`runtime info` outputs detailed information about a specified Starwhale Runtime version.

| Option | Required | Type | Defaults | Description |
| --- | --- | --- | --- | --- |
| `--fullname` | ❌ | Boolean | False | Show the full version name. Only the first 12 characters are shown if this option is false. |

## swcli runtime list {#list}

```bash
swcli [GLOBAL OPTIONS] runtime list [OPTIONS]
```

`model list` shows all Starwhale Runtimes.

| Option | Required | Type | Defaults | Description |
| --- | --- | --- | --- | --- |
| `--project` | ❌ | String | | The URI of the project to list. Use the [default project](../../swcli/uri.md#defaultProject) if not specified. |
| `--fullname` | ❌ | Boolean | False | Show the full version name. Only the first 12 characters are shown if this option is false. |
|`--show-removed` or `-sr` | ❌ | Boolean | False | If true, include runtimes that are removed but not garbage collected. |
| `--page` | ❌ | Integer | 1 | The start page number.  Server and cloud instances only. |
| `--size` | ❌ | Integer | 20 | The number of items in one page. Server and cloud instances only. |
| `--filter` or `-fl` | ❌ | String | | Show only Starwhale Runtimes that match specified filters. This option can be used multiple times in one command. |

| Filter | Type | Description | Example |
| --- | --- | --- | --- |
| `name` | Key-Value | The name prefix of runtimes | `--filter name=pytorch` |
| `owner` | Key-Value | The runtime owner name  | `--filter owner=starwhale` |
| `latest` | Flag | If specified, it shows only the latest version. | `--filter latest` |

## swcli runtime remove {#remove}

```bash
swcli [GLOBAL OPTIONS] runtime remove [OPTIONS] <RUNTIME>
```

`model remove` removes the specified Starwhale Runtime or version.

If the version part of the RUNTIME argument is omitted, all versions are removed.

Removed Starwhale Runtimes or versions can be recovered by `swcli runtime recover` before garbage collection. Use the `-- force` option to persistently remove a Starwhale Runtime or version.

Removed Starwhale Runtimes or versions can be listed by `swcli runtime list --show-removed`.

| Option | Required | Type | Defaults | Description |
| --- | --- | --- | --- | --- |
| `--force` or `-f` | ❌ | Boolean | False | If true, persistently delete the Starwhale Runtime or version. It can not be recovered. |

## swcli runtime recover {#recover}

```bash
swcli [GLOBAL OPTIONS] runtime recover [OPTIONS] <RUNTIME>
```

`runtime recover` can recover previously removed Starwhale Runtimes or versions.

If the version part of the RUNTIME argument is omitted, all removed versions are recovered.

Garbage-collected Starwhale Runtimes or versions can not be recovered, as well as those are removed with the `--force` option.

| Option | Required | Type | Defaults | Description |
| --- | --- | --- | --- | --- |
| `--force` or `-f` | ❌ | Boolean | False | If true, overwrite the Starwhale Runtime or version with the same name or version id. |

## swcli runtime tag {#tag}

```bash
swcli [GLOBAL OPTIONS] runtime tag [OPTIONS] RUNTIME [TAGS]...
```

`runtime tag` attaches a tag to a specified Starwhale Runtime version. The tag can be used in a runtime URI instead of the version id.

Each runtime version can have any number of tags， but duplicated tag names are not allowed in the same runtime.

**`runtime tag` only works for the [standalone instance](../../instances/standalone/index.md).**

| Option | Required | Type | Defaults | Description |
| --- | --- | --- | --- | --- |
| `--remove` or `-r` | ❌ | Boolean | False | remove the tag if true |
| `--quiet` or `-q` | ❌ | Boolean | False | ignore errors, for example, removing tags that do not exist. |
