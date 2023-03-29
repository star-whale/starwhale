---
title: Commands for Starwhale Model
---

## Overview

```bash
swcli [GLOBAL OPTIONS] model [OPTIONS] <SUBCOMMAND> [ARGS]...
```

The model command is used for managing Starwhale Models, including build, list, copy, etc.

To reference a model in SWCLI, you can use model URI, whose format is `[<Project URI>/model/]<model name>[/version/<version id>]`.

The `model` command includes the following subcommands:

| Subcommmand | Standalone | Cloud |
| --- | ---------- | ----- |
| `build` | ✅ | ❌ |
| `copy` or `cp` | ✅ | ✅ |
| `eval` | ✅ | ❌ |
| `history` | ✅ | ✅ |
| `info` | ✅ | ✅ |
| `list` or `ls` | ✅ | ✅ |
| `recover` | ✅ | ✅ |
| `remove` or `rm` | ✅ | ✅ |
| `tag` | ✅ | ❌ |
| `diff` | ✅ | ✅ |

## swcli model build {#build}

```bash
swcli model build [OPTIONS] <WORKDIR>
```

The `model build` command will put the whole `WORKDIR` into the model, except files that match patterns defined in [.swignore](../../swcli/swignore.md).

`model build` will read model.yaml and import the module specified in `run.handler` to generate the required configuration to run the model. So if your module depends on third-party libraries, we strongly recommend you use the `--runtime` option; otherwise, you need to ensure that the python runtime used by swcli has these libraries installed.

**`model build` only works for the [standalone instance](../../instances/standalone/index.md).**

| Option | Required | Type | Defaults | Description |
| ------ | ------- | ----------- | ----- | ----------- |
| `-p` or `--project` | ❌ | String | [the default project](../../swcli/uri.md#defaultProject) | the project URI |
| `-f` or `--model-yaml` | ❌ | String | ${workdir}/model.yaml | Path to model.yaml |
| `--runtime` | ❌ | String | | the URI of the [Starwhale Runtime](../../runtime/index.md) to use when running this command. If this option is used, this command will run in an independent python environment specified by the Starwhale Runtime; otherwise, it will run directly in the swcli's current python environment. |

### model.yaml

The YAML describes the required information to create a Starwhale Model.

| Field | Description | Required | Type | Default |
| --- | --- | ------- | --- | ----- |
| name | Starwhale Model name | ✅ | String | |
| version | The syntax version of model.yaml. Only 1.0 is supported. | ❌ | String | 1.0 |
| desc | The description of the model. | ❌ | String | "" |
| run.handler | The entry point for model evaluation. The format is "{module path}:{class name}". The path is relative to the WORKDIR parameter specified in the command. | ✅ | String | |
| run.envs | The environment variables to inject when running the model.The format is {name}={value} | ❌ | List[String] | |

For more information about `run.handler` and `run.envs`, see [Starwhale Evaluation](../../evaluation/index.md).

Example:

```yaml
name: demo
version: 1.0
desc: hello world
run:
  handler: model:ExampleHandler # the ExampleHandler class or function defined in model.py
  envs:
    - a=1
    - b=2
```

## swcli model copy {#copy}

```bash
swcli model copy [OPTIONS] <SRC> <DEST>
```

The `model copy` copies from `SRC` to `DEST`. `SRC` and `DEST` are both model URIs.

| Option | Required | Type | Defaults | Description |
| ------ | ------- | ----------- | ----- | ----------- |
|`--force` or `-f`| ❌ | Boolean | False | If true, `DEST` will be overwritten if it exists; otherwise, this command displays an error message. |

## swcli model eval {#eval}

```bash
swcli model eval [OPTIONS] <MODEL>
```

The `model eval` command will run a model evaluation. `MODEL` can be either a model URI or a local directory that contains the model.yaml. SWCLI will create a temporary Starwhale Model and run the evaluation in the latter case.

**`model eval` only works for the [standalone instance](../../instances/standalone/index.md).**

| Option | Required | Type | Defaults | Description |
| ------ | ------- | ----------- | ----- | ----------- |
| `--runtime` | ❌ | String | | the [Starwhale Runtime](../../runtime/index.md) URI to use when running this command. If this option is used, this command will run in an independent python environment specified by the Starwhale Runtime; otherwise, it will run directly in the swcli's current python environment. |
| `--dataset` | ✅ | String | | The URI of dataset used to run the evaluation. |
| `--use-docker` | ❌ | Boolean | False | Use docker to run the evaluation. This option is only available for standalone instances. For server and cloud instances, a docker image is always used. |
| `--image` | ❌ | Boolean | False | The docker image to run the evaluation. Only available when `--use-docker` is specified. |

![model-eval.gif](../../img/model-eval.gif)

## swcli model history {#history}

```bash
swcli model history [OPTIONS] <MODEL>
```

The `model history` command outputs all history versions of the specified Starwhale Model.

| Option | Required | Type | Defaults | Description |
| ------ | ------- | ----------- | ----- | ----------- |
| `--fullname` | ❌ | Boolean | False | Show the full version name. Only the first 12 characters are shown if this option is false. |

## swcli model info {#info}

```bash
swcli model info [OPTIONS] <MODEL>
```

The `model info` command outputs detail information about a specified Starwhale Model version.

| Option | Required | Type | Defaults | Description |
| ------ | ------- | ----------- | ----- | ----------- |
| `--fullname` | ❌ | Boolean | False | Show the full version name. Only the first 12 characters are shown if this option is false. |

## swcli model list {#list}

```bash
swcli model list [OPTIONS]
```

The `model list` command displays all Starwhale Models.

| Option | Required | Type | Defaults | Description |
| ------ | ------- | ----------- | ----- | ----------- |
| `--project` | ❌ | String | | The URI of the project to list. Use the [default project](../../swcli/uri.md#defaultProject) if not specified. |
| `--fullname` | ❌ | Boolean | False | Show the full version name. Only the first 12 characters are shown if this option is false. |
|`--show-removed` or `-sr` | ❌ | Boolean | False | If true, include packages that are removed but not garbage collected. |
| `--page` | ❌ | Integer | 1 | The start page number.  Server and cloud instances only. |
| `--size` | ❌ | Integer | 20 | The number of items in one page. Server and cloud instances only. |
| `--filter` or `-fl` | ❌ | String | | Show only Starwhale Models that match specified filters. This option can be used multiple times in one command. |

| Filter | Type | Description | Example |
| ---- | ------- | ----------- | ---- |
| `name` | Key-Value | The name prefix of models | `--filter name=mnist` |
| `owner` | Key-Value | The model owner name  | `--filter owner=starwhale` |
| `latest` | Flag | If specified, it shows only the latest version. | `--filter latest` |

## swcli model remove {#remove}

```bash
swcli model remove [OPTIONS] <MODEL>
```

The `model remove` command can remove the specified Starwhale Model or version.

If the version part of the MODEL argument is omitted, all versions are removed.

Removed Starwhale Models or versions can be recovered by `swcli model recover` before garbage collection. To persistently remove a Starwhale Model or version, you can use the `--force` option.

Removed Starwhale Models or versions can be listed by `swcli model list --show-removed`.

| Option | Required | Type | Defaults | Description |
| ------ | ------- | ----------- | ----- | ----------- |
| `--force` or `-f` | ❌ | Boolean | False | If true, persistently delete the Starwhale Model or version. It can not be recovered. |

## swcli model recover {#recover}

```bash
swcli model recover [OPTIONS] <MODEL>
```

The `model recover` command can recover previously removed Starwhale Models or versions.

If the version part of the MODEL argument is omitted, all removed versions are recovered.

Garbage-collected Starwhale Models or versions can not be recovered, as well as those are removed with the `--force` option.

| Option | Required | Type | Defaults | Description |
| ------ | ------- | ----------- | ----- | ----------- |
| `--force` or `-f` | ❌ | Boolean | False | If true, overwrite the Starwhale Model or version with the same name or version id. |

## swcli model tag {#tag}

```bash
swcli model tag [OPTIONS] <MODEL> [TAGS]...
```

The `model tag` command attaches a tag to a specified Starwhale Modle version. The tag can be used in a model URI instead of the version id.

Each model version can have any number of tags， but duplicated tag names are not allowed in the same model.

**`model tag` only works for the [standalone instance](../../instances/standalone/index.md).**

| Option | Required | Type | Defaults | Description |
| ------ | ------- | ----------- | ----- | ----------- |
| `--remove` or `-r` | ❌ | Boolean | False | remove the tag if true |
| `--quiet` or `-q` | ❌ | Boolean | False | ignore errors, for example, removing tags that do not exist. |

## swcli model diff {#diff}

```bash
swcli model diff [OPTIONS] <MODEL VERSION> <MODEL VERSION>
```

The `model diff` command compares the difference between two versions of the same model.

| Option | Required | Type | Defaults | Description |
| ------ | ------- | ----------- | ----- | ----------- |
| `--show-details` | ❌ | Boolean | False | If true, outputs the detail information. |

![model-diff.png](../../img/model-diff.png)
