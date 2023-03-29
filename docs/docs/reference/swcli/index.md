---
title: Overview
---

## Usage

```bash
swcli [OPTIONS] COMMAND [ARGS]...
```

:::note
`sw` and `starwhale` are aliases for the `swcli` commands.
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

* [`swcli dataset`](./dataset.md)
* [`swcli runtime`](./runtime.md)
* [`swcli model`](./model.md)
* [`swcli project`](./project.md)
* [`swcli instance`](./instance.md)
* [`swcli job`](./job.md)
* [`swcli ui`](./utilities.md#ui)
* [`swcli gc`](./utilities.md#gc)

swcli supports command abbreviation. You can input only the prefix of a command, and swcli will recognize the command. If there is any conflict, an error message will be displayed.

```bash
swcli r -h
swcli ru -h
swcli run -h
swcli runt -h
```

All the above commands are the same as `swcli runtime -h`.
