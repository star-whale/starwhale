---
title: Overview
---

## Usage

```bash
swcli [OPTIONS] <COMMAND> [ARGS]...
```

:::note
`sw` and `starwhale` are aliases for `swcli`.
:::

## Global Options {#global-option}

| Option | Description |
| ------ | ----------- |
| `--version` | Show the Starwhale Client version |
| `-v` or `--verbose` | Show verbose log, support multi counts for `-v` args. More `-v` args, more logs. |
| `--help` | Show the help message. |

:::caution
Global options must be put immediately after `swcli`, and before any command.
:::

## Commands

* [`swcli instance`](./instance.md)
* [`swcli project`](./project.md)
* [`swcli model`](./model.md)
* [`swcli dataset`](./dataset.md)
* [`swcli runtime`](./runtime.md)
* [`swcli job`](./job.md)
* [`swcli gc`](./utilities.md#gc)
