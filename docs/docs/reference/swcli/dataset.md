---
title: swcli dataset
---

## Overview

```bash
swcli [GLOBAL OPTIONS] dataset [OPTIONS] <SUBCOMMAND> [ARGS]...
```

The `dataset` command includes the following subcommands:

* `copy`
* `diff`
* `history`
* `info`
* `list`
* `recover`
* `remove`
* `summary`
* `tag`
* `head`

## swcli dataset copy {#copy}

```bash
swcli [GLOBAL OPTIONS] dataset copy [OPTIONS] <SRC> <DEST>
```

`dataset copy` copies from `SRC` to `DEST`.

`SRC` and `DEST` are both [dataset URIs](../../swcli/uri.md#model-dataset-runtime).

| Option | Required | Type | Defaults | Description |
| --- | --- | --- | --- | --- |
| `--force` or `-f` | ❌ | Boolean | False | If true, `DEST` will be overwritten if it exists; otherwise, this command displays an error message. |

## swcli dataset diff {#diff}

```bash
swcli [GLOBAL OPTIONS] dataset diff [OPTIONS] <DATASET VERSION> <DATASET VERSION>
```

`dataset diff` compares the difference between two versions of the same dataset.

`DATASET VERSION` is a [dataset URI](../../swcli/uri.md#model-dataset-runtime).

| Option | Required | Type | Defaults | Description |
| --- | --- | --- | --- | --- |
| `--show-details` | ❌ | Boolean | False | If true, outputs the detail information. |

## swcli dataset history {#history}

```bash
swcli [GLOBAL OPTIONS] dataset history [OPTIONS] <DATASET>
```

`dataset history` outputs all history versions of the specified Starwhale Dataset.

`DATASET` is a [dataset URI](../../swcli/uri.md#model-dataset-runtime).

| Option | Required | Type | Defaults | Description |
| --- | --- | --- | --- | --- |
| `--fullname` | ❌ | Boolean | False | Show the full version name. Only the first 12 characters are shown if this option is false. |

## swcli dataset info {#info}

```bash
swcli [GLOBAL OPTIONS] dataset info [OPTIONS] <DATASET>
```

`dataset info` outputs detailed information about the specified Starwhale Dataset version.

`DATASET` is a [dataset URI](../../swcli/uri.md#model-dataset-runtime).

## swcli dataset list {#list}

```bash
swcli [GLOBAL OPTIONS] dataset list [OPTIONS]
```

`dataset list` shows all Starwhale Datasets.

| Option | Required | Type | Defaults | Description |
| --- | --- | --- | --- | --- |
| `--project` | ❌ | String | | The URI of the project to list. Use the [default project](../../swcli/uri.md#defaultProject) if not specified. |
| `--fullname` | ❌ | Boolean | False | Show the full version name. Only the first 12 characters are shown if this option is false. |
| `--show-removed` or `-sr` | ❌ | Boolean | False | If true, include datasets that are removed but not garbage collected. |
| `--page` | ❌ | Integer | 1 | The starting page number.  Server and cloud instances only. |
| `--size` | ❌ | Integer | 20 | The number of items in one page. Server and cloud instances only. |
| `--filter` or `-fl` | ❌ | String | | Show only Starwhale Datasetes that match specified filters. This option can be used multiple times in one command. |

| Filter | Type | Description | Example |
| --- | --- | --- | --- |
| `name` | Key-Value | The name prefix of datasets | `--filter name=mnist` |
| `owner` | Key-Value | The dataset owner name  | `--filter owner=starwhale` |
| `latest` | Flag | If specified, it shows only the latest version. | `--filter latest` |

## swcli dataset recover {#recover}

```bash
swcli [GLOBAL OPTIONS] dataset recover [OPTIONS] <DATASET>
```

`dataset recover` recovers previously removed Starwhale Datasets or versions.

`DATASET` is a [dataset URI](../../swcli/uri.md#model-dataset-runtime). If the version part of the URI is omitted, all removed versions are recovered.

Garbage-collected Starwhale Datasets or versions can not be recovered, as well as those are removed with the `--force` option.

| Option | Required | Type | Defaults | Description |
| --- | --- | --- | --- | --- |
| `--force` or `-f` | ❌ | Boolean | False | If true, overwrite the Starwhale Dataset or version with the same name or version id. |

## swcli dataset remove {#remove}

```bash
swcli [GLOBAL OPTIONS] dataset remove [OPTIONS] <DATASET>
```

`dataset remove` removes the specified Starwhale Dataset or version.

`DATASET` is a [dataset URI](../../swcli/uri.md#model-dataset-runtime). If the version part of the URI is omitted, all versions are removed.

Removed Starwhale Datasets or versions can be recovered by `swcli dataset recover` before garbage collection. Use the `--force` option to persistently remove a Starwhale Dataset or version.

Removed Starwhale Datasets or versions can be listed by `swcli dataset list --show-removed`.

| Option | Required | Type | Defaults | Description |
| --- | --- | --- | --- | --- |
| `--force` or `-f` | ❌ | Boolean | False | If true, persistently delete the Starwhale Dataset or version. It can not be recovered. |

## swcli dataset tag {#tag}

```bash
swcli [GLOBAL OPTIONS] dataset tag [OPTIONS] <DATASET> [TAGS]...
```

`dataset tag` attaches a tag to a specified Starwhale Dataset version. The tag can be used in a dataset URI instead of the version id.

`DATASET` is a [dataset URI](../../swcli/uri.md#model-dataset-runtime).

Each dataset version can have any number of tags， but duplicated tag names are not allowed in the same dataset.

**`dataset tag` only works for the [standalone instance](../../instances/standalone/index.md).**

| Option | Required | Type | Defaults | Description |
| --- | --- | --- | --- | --- |
| `--remove` or `-r` | ❌ | Boolean | False | remove the tag if true |
| `--quiet` or `-q` | ❌ | Boolean | False | ignore errors, for example, removing tags that do not exist. |
