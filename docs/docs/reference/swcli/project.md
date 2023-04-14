---
title: swcli project
---

## Overview

```bash
swcli [GLOBAL OPTIONS] project [OPTIONS] <SUBCOMMAND> [ARGS]...
```

The `project` command includes the following subcommands:

* `create`
* `info`
* `list`
* `recover`
* `remove`
* `use`

## swcli project create {#create}

```bash
swcli [GLOBAL OPTIONS] project create <PROJECT>
```

`project create` creates a new project.

`PROJECT` is a [project URI](../../swcli/uri.md#project).

## swcli project info {#info}

```bash
swcli [GLOBAL OPTIONS] project info [OPTIONS] <PROJECT>
```

`project info` outputs detailed information about the specified Starwhale Project.

`PROJECT` is a [project URI](../../swcli/uri.md#project).

## swcli project list {#list}

```bash
swcli [GLOBAL OPTIONS] project list [OPTIONS]
```

`project list` shows all Starwhale Projects.

| Option | Required | Type | Defaults | Description |
| --- | --- | --- | --- | --- |
| `--instance` | ❌ | String | | The URI of the instance to list. If this option is omitted, use the [default instance](../../swcli/uri.md#defaultInstance). |
| `--show-removed` | ❌ | Boolean | False | If true, include projects that are removed but not garbage collected. |
| `--page` | ❌ | Integer | 1 | The starting page number.  Server and cloud instances only. |
| `--size` | ❌ | Integer | 20 | The number of items in one page. Server and cloud instances only. |

## swcli project recover {#recover}

```bash
swcli [GLOBAL OPTIONS] project recover [OPTIONS] <PROJECT>
```

`project recover` recovers previously removed Starwhale Projects.

`PROJECT` is a [project URI](../../swcli/uri.md#project).

Garbage-collected Starwhale Projects can not be recovered, as well as those are removed with the `--force` option.

## swcli project remove {#remove}

```bash
swcli [GLOBAL OPTIONS] project remove [OPTIONS] <PROJECT>
```

`project remove` removes the specified Starwhale Project.

`PROJECT` is a [project URI](../../swcli/uri.md#project).

Removed Starwhale Projects can be recovered by `swcli project recover` before garbage collection. Use the `--force` option to persistently remove a Starwhale Project.

Removed Starwhale Project can be listed by `swcli project list --show-removed`.

| Option | Required | Type | Defaults | Description |
| --- | --- | --- | --- | --- |
| `--force` or `-f` | ❌ | Boolean | False | If true, persistently delete the Starwhale Project. It can not be recovered. |

## swcli project use {#use}

```bash
swcli [GLOBAL OPTIONS] project use <PROJECT>
```

`project use` make the specified project [default](../../swcli/uri.md#defaultProject). You must [login](#login) at first to use a project on a Server/Cloud instance.
