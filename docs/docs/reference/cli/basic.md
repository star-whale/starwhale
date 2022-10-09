---
title: Overview
---

## Usage

```bash
swcli [OPTIONS] COMMAND [ARGS]...
```

:::note
`sw`, `starwhale` are aliases for the `swcli` commands.
:::

## Options

|Option|Description|
|------|-----------|
|`--version`|Show the Starwhale version|
|`-v` or `--verbose`|Show verbose log, support multi counts for `-v` args. More `-v` args, more logs.|
|`--help`|Show help message.|

## Commands

|Command|Description|
|-------|-----------|
|dataset|Dataset management, build/info/list/copy/tag/render-fuse...|
|runtime|Runtime management, create/build/copy/activate/restore...|
|model|Model management, build/copy/ppl/cmp/eval/extract...|
|project|Project management, for standalone and cloud instances|
|instance|Instance management, login and select standalone or cloud instance|
|job|Job management, create/list/info/compare evaluation job|
|ui|Open instance web ui|
|gc|Standalone garbage collection|
