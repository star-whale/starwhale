---
title: swcli runtime
---

## Overview

```bash
swcli [GLOBAL OPTIONS] runtime [OPTIONS] <SUBCOMMAND> [ARGS]...
```

The `runtime` command includes the following subcommands:

* `activate`
* `build`
* `copy`
* `dockerize`
* `history`
* `info`
* `list`
* `recover`
* `remove`
* `tag`

## swcli runtime activate {#activate}

```bash
swcli [GLOBAL OPTIONS] runtime activate [OPTIONS] <RUNTIME>
```

Like `source venv/bin/activate` or `conda activate xxx`, `runtime activate` setups a new python environment according to the settings of the specified runtime. When the current shell is closed or switched to another one, you need to reactivate the runtime.

`RUNTIME` is a [Runtime URI](../../swcli/uri.md#model-dataset-runtime).

If you want to quit the activated runtime environment, please run `venv deactivate` in the venv environment or `conda deactivate` in the conda environment.

## swcli runtime build {#build}

```bash
swcli [GLOBAL OPTIONS] runtime build [OPTIONS] <WORKDIR>
```

`runtime build` and creates a new Starwhale Runtime from a conda enviroment, a virtualenv environment, or a docker image.

| Option | Required | Type | Defaults | Description |
| --- | --- | --- | --- | --- |
| `--project` or `-p` | ❌ | String | [the default project](../../swcli/uri.md#defaultProject) | the project URI |
| `--conda` | ❌ | String | The name of the conda environment to build from |
| `--conda-prefix` | ❌ | String | The path of the conda environment to build from |
| `--venv` | ❌ | String | The path of the virtualenv environment to build from |
| `--docker` | ❌ | String | The docker image to build from |

## swcli runtime copy {#copy}

```bash
swcli [GLOBAL OPTIONS] runtime copy [OPTIONS] <SRC> <DEST>
```

`runtime copy` copies from `SRC` to `DEST`.

`SRC` and `DEST` are both [Runtime URIs](../../swcli/uri.md#model-dataset-runtime).

| Option | Required | Type | Defaults | Description |
| --- | --- | --- | --- | --- |
|`--force` or `-f`| ❌ | Boolean | False | If true, `DEST` will be overwritten if it exists; otherwise, this command displays an error message. |

## swcli runtime dockerize {#dockerize}

```bash
swcli [GLOBAL OPTIONS] runtime dockerize [OPTIONS] <RUNTIME>
```

`runtime dockerize` generates a docker image based on the specified runtime. Starwhale uses `docker buildx` to create the image. Docker 19.03 or later is required to run this command.

`RUNTIME` is a [Runtime URI](../../swcli/uri.md#model-dataset-runtime).

| Option | Required | Type | Defaults | Description |
| --- | --- | --- | --- | --- |
| `--tag` or `-t` | ❌ | String |  | The tag of the docker image. This option can be repeated multiple times. |
| `--push` | ❌ | Boolean | False | If true, push the image to the docker registry |
| `--platform` | ❌ | String | amd64 | The target platform，can be either amd64 or arm64. This option can be repeated multiple times to create a multi-platform image. |

## swcli runtime history {#history}

```bash
swcli [GLOBAL OPTIONS] runtime history [OPTIONS] <RUNTIME>
```

`runtime history` outputs all history versions of the specified Starwhale Runtime.

`RUNTIME` is a [Runtime URI](../../swcli/uri.md#model-dataset-runtime).

| Option | Required | Type | Defaults | Description |
| --- | --- | --- | --- | --- |
| `--fullname` | ❌ | Boolean | False | Show the full version name. Only the first 12 characters are shown if this option is false. |

## swcli runtime info {#info}

```bash
swcli [GLOBAL OPTIONS] runtime info [OPTIONS] <RUNTIME>
```

`runtime info` outputs detailed information about a specified Starwhale Runtime version.

`RUNTIME` is a [Runtime URI](../../swcli/uri.md#model-dataset-runtime).

| Option | Required | Type | Defaults | Description |
| --- | --- | --- | --- | --- |
| `--output-filter` or `-of` | ❌ | Choice of [basic|runtime_yaml|manifest|lock|all] | basic | Filter the output content. Only standalone instance supports this option. |

## swcli runtime list {#list}

```bash
swcli [GLOBAL OPTIONS] runtime list [OPTIONS]
```

`runtime list` shows all Starwhale Runtimes.

| Option | Required | Type | Defaults | Description |
| --- | --- | --- | --- | --- |
| `--project` | ❌ | String | | The URI of the project to list. Use the [default project](../../swcli/uri.md#defaultProject) if not specified. |
| `--fullname` | ❌ | Boolean | False | Show the full version name. Only the first 12 characters are shown if this option is false. |
|`--show-removed` or `-sr` | ❌ | Boolean | False | If true, include runtimes that are removed but not garbage collected. |
| `--page` | ❌ | Integer | 1 | The starting page number.  Server and cloud instances only. |
| `--size` | ❌ | Integer | 20 | The number of items in one page. Server and cloud instances only. |
| `--filter` or `-fl` | ❌ | String | | Show only Starwhale Runtimes that match specified filters. This option can be used multiple times in one command. |

| Filter | Type | Description | Example |
| --- | --- | --- | --- |
| `name` | Key-Value | The name prefix of runtimes | `--filter name=pytorch` |
| `owner` | Key-Value | The runtime owner name  | `--filter owner=starwhale` |
| `latest` | Flag | If specified, it shows only the latest version. | `--filter latest` |

## swcli runtime recover {#recover}

```bash
swcli [GLOBAL OPTIONS] runtime recover [OPTIONS] <RUNTIME>
```

`runtime recover` can recover previously removed Starwhale Runtimes or versions.

`RUNTIME` is a [Runtime URI](../../swcli/uri.md#model-dataset-runtime). If the version part of the URI is omitted, all removed versions are recovered.

Garbage-collected Starwhale Runtimes or versions can not be recovered, as well as those are removed with the `--force` option.

| Option | Required | Type | Defaults | Description |
| --- | --- | --- | --- | --- |
| `--force` or `-f` | ❌ | Boolean | False | If true, overwrite the Starwhale Runtime or version with the same name or version id. |

## swcli runtime remove {#remove}

```bash
swcli [GLOBAL OPTIONS] runtime remove [OPTIONS] <RUNTIME>
```

`runtime remove` removes the specified Starwhale Runtime or version.

`RUNTIME` is a [Runtime URI](../../swcli/uri.md#model-dataset-runtime). If the version part of the URI is omitted, all versions are removed.

Removed Starwhale Runtimes or versions can be recovered by `swcli runtime recover` before garbage collection. Use the `-- force` option to persistently remove a Starwhale Runtime or version.

Removed Starwhale Runtimes or versions can be listed by `swcli runtime list --show-removed`.

| Option | Required | Type | Defaults | Description |
| --- | --- | --- | --- | --- |
| `--force` or `-f` | ❌ | Boolean | False | If true, persistently delete the Starwhale Runtime or version. It can not be recovered. |

## swcli runtime tag {#tag}

```bash
swcli [GLOBAL OPTIONS] runtime tag [OPTIONS] <RUNTIME> [TAGS]...
```

`runtime tag` attaches a tag to a specified Starwhale Runtime version. The tag can be used in a runtime URI instead of the version id.

`RUNTIME` is a [Runtime URI](../../swcli/uri.md#model-dataset-runtime).

Each runtime version can have any number of tags， but duplicated tag names are not allowed in the same runtime.

**`runtime tag` only works for the [standalone instance](../../instances/standalone/index.md).**

| Option | Required | Type | Defaults | Description |
| --- | --- | --- | --- | --- |
| `--remove` or `-r` | ❌ | Boolean | False | remove the tag if true |
| `--quiet` or `-q` | ❌ | Boolean | False | ignore errors, for example, removing tags that do not exist. |
