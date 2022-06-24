---
title: Project
---

## Usage

```bash
swcli project [OPTIONS] COMMAND [ARGS]...
```

## Summary

- Command for project lifecycle management.
- Project contains models, datasets, runtimes and jobs.
- For standalone instance, `project` command use local disk to store project meta. `self` project will be created and selected automatically.
- For cloud instance, `project` command manage the remote cloud projects with HTTP API.
- **Project URI** in format: `[<Instance URI>/project]<project name>`.

## All Sub-Commands

  |Command|Standalone|Cloud|
  |-------|----------|-----|
  |create|✅|✅|
  |info|✅|✅|
  |list|✅|✅|
  |remove|✅|✅|
  |recover|✅|✅|
  |select|✅|✅|

## Create a project

```bash
swcli project create PROJECT
```

- This command creates a new project.
- `PROJECT` argument is required, which uses `Project URI` format.
- Example:

    ```bash
    ❯ swcli project create myproject
    👏 do successfully
    ❯ swcli project create myproject
    🤿 failed to run, reason:/home/liutianwei/.cache/starwhale/myproject was already existed
    ```

## List projects

```bash
swcli project list [OPTIONS]
```

- This command lists all viewable projects in the instance.
- Options:

    |Option|Alias Option|Required|Type|Default|Description|
    |------|--------|-------|-----------|-----|-----------|
    |`--instance`|`-i`|❌|String|Selected instance|Instance URI|
    |`--page`||❌|Integer|1|Page number for project list|
    |`--size`||❌|Integer|20|Page size for project list|

- Example:

    ```bash
    ❯ swcli project list
    ❯ swcli project list -i pre-k8s
    ❯ swcli project list -i http://console.pre.intra.starwhale.ai
    ```

## Select the default project

```bash
swcli project select PROJECT
```

- `PROJECT` argument is required, it uses `Project URI` format.
- For cloud instances, you should login it at first.
- Example:

    ```bash
    ❯ swcli project select local/project/self
    👏 select instance:local, project:self successfully
    ```

## Remove a project

```bash
swcli project remove PROJECT
```

- This command removes a project, you can run `swcli project recover` to recover the project.
- `PROJECT` argument is required, it uses `Project URI` format.
- Example:

    ```bash
    ❯ swcli project remove myproject
    🐶 remove project myproject, you can recover it, don't panic.
    ```

## Recover a project

```bash
swcli project recover PROJECT
```

- This command recover a removed project.
- `PROJECT` argument is required, it uses `Project URI` format.
- Example:

    ```bash
    ❯ swcli project recover myproject
    👏 recover project myproject
    ```
