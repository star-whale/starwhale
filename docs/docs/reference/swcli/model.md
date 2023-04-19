---
title: swcli model
---

## Overview

```bash
swcli [GLOBAL OPTIONS] model [OPTIONS] <SUBCOMMAND> [ARGS]...
```

The `model` command includes the following subcommands:

* `build`
* `copy`
* `diff`
* `history`
* `info`
* `list`
* `recover`
* `remove`
* `run`
* `tag`

## swcli model build {#build}

```bash
swcli [GLOBAL OPTIONS] model build [OPTIONS] <WORKDIR>
```

`model build` will put the whole `WORKDIR` into the model, except files that match patterns defined in [.swignore](../../swcli/swignore.md).

`model build` will import modules specified by `--module` to generate the required configurations to run the model. If your module depends on third-party libraries, we strongly recommend you use the `--runtime` option; otherwise, you need to ensure that the python environment used by swcli has these libraries installed.

| Option | Required | Type | Defaults | Description |
| --- | --- | --- | --- | --- |
| `--project` or `-p` | ❌ | String | [the default project](../../swcli/uri.md#defaultProject) | the project URI |
| `--module` or `-m`| ❌ | String | | Python modules to be imported during the build process. Starwhale will export model handlers from these modules to the model package. This option supports set multiple times.|
| `--runtime` or `-r` | ❌ | String | | the URI of the [Starwhale Runtime](../../runtime/index.md) to use when running this command. If this option is used, this command will run in an independent python environment specified by the Starwhale Runtime; otherwise, it will run directly in the swcli's current python environment. |
| `--name` or `-n` | ❌ | String | | model package name |
| `--desc` or `-d` | ❌ | String | | model package description |

## swcli model copy {#copy}

```bash
swcli [GLOBAL OPTIONS] model copy [OPTIONS] <SRC> <DEST>
```

`model copy` copies from `SRC` to `DEST`.

`SRC` and `DEST` are both [model URIs](../../swcli/uri.md#model-dataset-runtime).

| Option | Required | Type | Defaults | Description |
| --- | --- | --- | --- | --- |
| `--force` or `-f` | ❌ | Boolean | False | If true, `DEST` will be overwritten if it exists; otherwise, this command displays an error message. |

## swcli model diff {#diff}

```bash
swcli [GLOBAL OPTIONS] model diff [OPTIONS] <MODEL VERSION> <MODEL VERSION>
```

`model diff` compares the difference between two versions of the same model.

`MODEL VERSION` is a [model URI](../../swcli/uri.md#model-dataset-runtime).

| Option | Required | Type | Defaults | Description |
| --- | --- | --- | --- | --- |
| `--show-details` | ❌ | Boolean | False | If true, outputs the detail information. |

## swcli model history {#history}

```bash
swcli [GLOBAL OPTIONS] model history [OPTIONS] <MODEL>
```

`model history` outputs all history versions of the specified Starwhale Model.

`MODEL` is a [model URI](../../swcli/uri.md#model-dataset-runtime).

| Option | Required | Type | Defaults | Description |
| --- | --- | --- | --- | --- |
| `--fullname` | ❌ | Boolean | False | Show the full version name. Only the first 12 characters are shown if this option is false. |

## swcli model info {#info}

```bash
swcli [GLOBAL OPTIONS] model info [OPTIONS] <MODEL>
```

`model info` outputs detailed information about the specified Starwhale Model version.

`MODEL` is a [model URI](../../swcli/uri.md#model-dataset-runtime).

| Option | Required | Type | Defaults | Description |
| --- | --- | --- | --- | --- |
| `--fullname` | ❌ | Boolean | False | Show the full version name. Only the first 12 characters are shown if this option is false. |

## swcli model list {#list}

```bash
swcli [GLOBAL OPTIONS] model list [OPTIONS]
```

`model list` shows all Starwhale Models.

| Option | Required | Type | Defaults | Description |
| --- | --- | --- | --- | --- |
| `--project` | ❌ | String | | The URI of the project to list. Use the [default project](../../swcli/uri.md#defaultProject) if not specified. |
| `--fullname` | ❌ | Boolean | False | Show the full version name. Only the first 12 characters are shown if this option is false. |
| `--show-removed` | ❌ | Boolean | False | If true, include packages that are removed but not garbage collected. |
| `--page` | ❌ | Integer | 1 | The starting page number.  Server and cloud instances only. |
| `--size` | ❌ | Integer | 20 | The number of items in one page. Server and cloud instances only. |
| `--filter` or `-fl` | ❌ | String | | Show only Starwhale Models that match specified filters. This option can be used multiple times in one command. |

| Filter | Type | Description | Example |
| --- | --- | --- | --- |
| `name` | Key-Value | The name prefix of models | `--filter name=mnist` |
| `owner` | Key-Value | The model owner name  | `--filter owner=starwhale` |
| `latest` | Flag | If specified, it shows only the latest version. | `--filter latest` |

## swcli model recover {#recover}

```bash
swcli [GLOBAL OPTIONS] model recover [OPTIONS] <MODEL>
```

`model recover` recovers previously removed Starwhale Models or versions.

`MODEL` is a [model URI](../../swcli/uri.md#model-dataset-runtime). If the version part of the URI is omitted, all removed versions are recovered.

Garbage-collected Starwhale Models or versions can not be recovered, as well as those are removed with the `--force` option.

| Option | Required | Type | Defaults | Description |
| --- | --- | --- | --- | --- |
| `--force` or `-f` | ❌ | Boolean | False | If true, overwrite the Starwhale Model or version with the same name or version id. |

## swcli model remove {#remove}

```bash
swcli [GLOBAL OPTIONS] model remove [OPTIONS] <MODEL>
```

`model remove` removes the specified Starwhale Model or version.

`MODEL` is a [model URI](../../swcli/uri.md#model-dataset-runtime). If the version part of the URI is omitted, all versions are removed.

Removed Starwhale Models or versions can be recovered by `swcli model recover` before garbage collection. Use the `--force` option to persistently remove a Starwhale Model or version.

Removed Starwhale Models or versions can be listed by `swcli model list --show-removed`.

| Option | Required | Type | Defaults | Description |
| --- | --- | --- | --- | --- |
| `--force` or `-f` | ❌ | Boolean | False | If true, persistently delete the Starwhale Model or version. It can not be recovered. |

## swcli model run {#run}

```bash
swcli [GLOBAL OPTIONS] model run [OPTIONS]
```

`model run` executes a model handler. Model run supports two modes to run: [model URI](../../swcli/uri.md#model-dataset-runtime) and local development. Model URI mode needs a pre-built Starwhale Model Package. Local development model only needs the model src dir.

| Option | Required | Type | Defaults | Description |
| --- | --- | --- | --- | --- |
| `--workdir` or `-w` | ❌ | String | | For local development mode, the path of model src dir.|
| `--uri` or `-u` | ❌ | String | | For model URI mode, the string of model uri.|
| `--handler` or `-h` | ❌ | String | | Runnable handler index or name, default is None, will use the first handler |
| `--runtime` | ❌ | String | | the [Starwhale Runtime](../../runtime/index.md) URI to use when running this command. If this option is used, this command will run in an independent python environment specified by the Starwhale Runtime; otherwise, it will run directly in the swcli's current python environment. |
| `--in-container` | ❌ | Boolean | False | Use docker container to run the model. This option is only available for standalone instances. For server and cloud instances, a docker image is always used. If the runtime is a docker image, this option is always implied. |

## swcli model tag {#tag}

```bash
swcli [GLOBAL OPTIONS] model tag [OPTIONS] <MODEL> [TAGS]...
```

`model tag` attaches a tag to a specified Starwhale Model version. The tag can be used in a model URI instead of the version id.

`MODEL` is a [model URI](../../swcli/uri.md#model-dataset-runtime).

Each model version can have any number of tags， but duplicated tag names are not allowed in the same model.

**`model tag` only works for the [standalone instance](../../instances/standalone/index.md).**

| Option | Required | Type | Defaults | Description |
| --- | --- | --- | --- | --- |
| `--remove` or `-r` | ❌ | Boolean | False | remove the tag if true |
| `--quiet` or `-q` | ❌ | Boolean | False | ignore errors, for example, removing tags that do not exist. |
