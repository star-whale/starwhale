---
title: Starwhale Client User Guide
---
This guide explains all commands provided by the Starwhale client (swcli).

## Definitions

### Resource URI

Resource URI is widely used in Starwhale client commands. The URI can refer to a resource in the local instance or any other resource in a remote instance. In this way, the Starwhale client can easily manipulate any resource.

### Instance URI

Instance URI can be either:

- A URL in format: `[http(s)://]<hostname or ip>[:<port>]`, which refers to a Starwhale controller. The default scheme is HTTP, and the default port is 7827.
- local, which means the local standalone instance.

:::tip Caveat
"local" is different from "localhost". The former means the local standalone instance without a controller, while the latter implies a controller listening at the default port 7827 on localhost.|
:::

### Project URI

Project URI is in the format `[<Instance URI>/project/]<project name>`. If the instance URI is not specified, use the default instance instead.

### Other Resources URI

- Model: `[<Project URI>/model/]<model name>[/version/<version id>]`
- Dataset: `[<Project URI>/dataset/]<dataset name>[/version/<version id>]`
- Runtime: `[<Project URI>/runtime/]<runtime name>[/version/<version id>]`
- Job: `[<Project URI>/job/]<job id>`

If the project URI is not specified, use the default project.

If the version id is not specified, use the latest version.

### Names

Names mean project names, model names, dataset names, runtime names, and tag names.

Names are case-insensitive.

A name MUST only consist of letters `A-Z a-z`, digits `0-9`, the hyphen character `-`, and the underscore character `_`.

A name should always start with a letter or the `_` character.

The maximum length of a name is 80.

#### Name uniqueness requirement

The resource name should be a unique string within its owner. For example, the project name should be unique in the owner instance, and the model name should be unique in the owner project.

The resource name can not be used by any other resource of the same kind in the owner, including those removed ones. For example, Project "apple" can not have two models named "Alice", even if one of them is already removed.

Different kinds of resources can have the same name. For example, a project and a model can have the same name "Alice".

Resources with different owners can have the same name. For example, a model in project "Apple" and a model in project "Banana" can have the same name "Alice".

Garbage collected resources' names can be reused. For example, after the model with the name "Alice" in project "Apple" is removed and garbage collected, the project can have a new model with the same name "Alice".

## Project management

### Select the default project

This command sets both the default instance and project used by other commands. When a project is selected as the default project, its owner instance is also selected as the default instance.

```console
swcli project select <project uri>
```

### List projects

This command lists all viewable projects in the instance.

```console
swcli project list [OPTIONS] [instance uri]
```

Options:

- -a, --all Include removed projects which have not been garbage collected.

### Remove a project

This command removes a project.

```console
swcli project remove <project uri>
```

### Recover a project

This command recovers a removed project.

```console
swcli project recover <project uri>
```

## Job Management

### List jobs

This command lists all jobs in the project.

```console
swcli job list [project uri]
```

### Get a job's detailed information

This command shows detailed information about a job.

```console
swcli job info <job uri>
```

### Pause a job

This command pauses a running job.

```console
swcli job pause <job uri>
```

### Resume a job

This command resumes a paused job.

```console
swcli job resume <job uri>
```

### Cancel a job

This command cancels a running/paused job.

```console
swcli job cancel <job uri>
```

## Utilities

### Garbage collection

This command purges removed entities in the instance. Purged entities are not recoverable.

```console
swcli gc [instance uri]
```

The garbage collection aims to hold the storage size within an acceptable range. It keeps removing the oldest removed entity until the total storage size is not greater than a predefined threshold.

## Others

### Special rules for versioned resources

Some resources are versioned, like models, datasets, and runtimes. There are some special rules for commands manipulating these resources.

#### Versioned resource creation and update

The command for creation and update has the following pattern:

```console
swcli model/dataset/runtime build <resource uri> [working dir]
```

A new resource with one version is automatically created when the specified resource URI does not exist. Otherwise, a new version of the resource is created and becomes the latest version.

#### Versioned resource removal and recovery

The commands for removal and recovery have the following pattern:

```console
swcli model/dataset/runtime remove/recover <resource uri>
```

Only the specified version will be removed/recovered if the resource URI has the version part. Otherwise, the resource with the whole history will be removed/recovered.

If the resource has only one version and is being removed, the resource is removed as well.
