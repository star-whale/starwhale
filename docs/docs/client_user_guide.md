# Overview

This guide explains all commands provided by the Starwhale client (swcli).

# Definitions

## Resource URI

Resource URI is widely used in Starwhale client commands. The URI can refer to a resource in the local instance or any other resource in a remote instance. In this way, the Starwhale client can manipulate any kind of resource easily.

## Instance URI

Instance URI can be either
- A URL in format: `[http(s)://]<hostname or ip>[:<port>]`, which refers to a Starwhale controller. The default scheme is HTTP, and the default port is 7827.
- local, which means the local standalone instance.

**Caveat**

"local" is different from "localhost". The former one means the local standalone instance without a controller, while the latter one means a controller listening at the default port 7827 on localhost.

## Project URI

Project URI is in format `[<Instance URI>/project/]<project name>`. If the instance URI is not specified, use the default instance instead.

## Other Resources URI

- Model: `[<Project URI>/model/]<model name>[/version/<version id>]`
- Dataset: `[<Project URI>/dataset/]<dataset name>[/version/<version id>]`
- Runtime: `[<Project URI>/runtime/]<runtime name>[/version/<version id>]`
- Job: `[<Project URI>/job/]<job id>`

If the project URI is not specified, use the default project.

If the version id is not specified, use the latest version.

## Names

Names means project names, model names, dataset names, runtime names, and tag names.

Names are case-insensitive.

A name MUST only consists of letters `A-Z a-z`, digits `0-9`, the hyphen character `-`, and the underscore character `_`.

A name should always starts with a letter or the `_` character.

The maximum length of a name is 80.

### Name uniqueness requirement

The resource name should be a unique string within its owner. For example, the project name should be unique in the owner instance, and the model name should be unique in the owner project.

The resource name can not be in use by any other resource of the same kind in the owner, including those removed ones. For example, Project "apple" can not have two models with the name "Alice", even if one of them is already removed.

Different kinds of resources can have the same name. For example, a project and a model can have the same name "Alice". 

Resources with different owners can have the same name. For example, a model in project "Apple" and a model in project "Banana" can have the same name "Alice".

The name previously used by garbage collected datasets can be reused. For example, after the model with the name "Alice" in project "Apple" is removed and garbage collected, the project can have a new model with the same name "Alice".

# Instance management

## Select the default instance

This command sets the default Starwhale instance used by other commands.
```
swcli instance select <instance uri>
```

# Project management

## Select the default project

This command sets both the default instance and project used by other commands. When a project is selected as the default project, its owner instance is also selected as the default instance.
```
swcli project select <project uri>
```

## Create a project

This command creates a new project.

```
swcli project create <project uri>
```

## List projects

This command lists all viewable projects in the instance.

```
swcli project list [OPTIONS] [instance uri]
```

Options:
- -a, --all Include removed projects which have not been garbage collected.

## Remove a project

This command removes a project.

```
swcli project remove <project uri>
```

## Recover a project

This command recovers a removed project.
```
swcli project recover <project uri>
```

# Model management

## Build a model

This command builds a model with the specified working directory. 

```
swcli model build <model uri> [working dir]
```

## List models

This command lists all models in the project.

```
swcli model list [project uri]
```

## Remove a model

This command removes a model.

```
swcli model remove <model uri>
```

## Recover a model

This command recovers a removed model.

```
swcli model recover <model uri>
```

## Evaluate a model

This command creates a new job for model evaluation.

```
swcli model eval [OPTIONS] <model uri> 
```

## Manage model tags

This command adds or removes tags on the specified model.

```
swcli model tag [OPTIONS] <model uri> <tag name>[,<tagname>...]
```

Options:
- -r, --remove If specified, removes the tag names from the model.
- -q, --quiet If specified, ignore tag name errors like name duplication, name absence, etc.

## Copy a model

This command copies a model to another place, either locally or remotely.

```
swcli model copy <source model uri> <destination model uri>
```

The destination model URI should not currently exist and observes the name uniqueness rule.

## Show model history

This command shows the history of a model.
```
swcli model history <model uri>
```

## Revert a model to an old version

This command reverts a model to an old version.

```
swcli model revert <model uri>
```

# Dataset Management

## Build a dataset

This command builds a dataset with the specified working directory.

```
swcli dataset build <dataset uri> [working dir]
```

## List datasets

This command lists all datasets in the project.

```
swcli dataset list [project uri]
```

## Remove a dataset

This command removes a dataset.

```
swcli dataset remove <dataset uri>
```

## Recover a dataset

This command recovers a removed dataset.

```
swcli dataset recover <dataset uri>
```

## Manage dataset tags

This command adds or removes tags on the specified dataset.

```
swcli dataset tag [OPTIONS] <dataset uri> <tag name>[,<tagname>...]
```

Options:
- -r, --remove If specified, removes the tag names from the dataset.
- -q, --quiet If specified, ignore tag name errors like name duplication, name absence, etc.

## Copy a dataset

This command copies a dataset to another place, either locally or remotely.

```
swcli dataset copy <source dataset uri> <destination dataset uri>
```

The destination dataset URI should not currently exist and observes the name uniqueness rule.

## Show dataset history

This command shows the history of a dataset.

```
swcli dataset history <dataset uri>
```

## Revert a dataset to an old version

This command reverts a dataset to an old version.

```
swcli dataset revert <dataset uri>
```

# Runtime management

## Build a runtime

This command builds a runtime with the specified working directory.

```
swcli runtime build <runtime uri> [working dir]
```

## List runtimes

This command lists all runtimes in the project.

```
swcli runtime list [project uri]
```

## Remove a runtime

This command removes a runtime.

```
swcli runtime remove <runtime uri>
```

## Recover a runtime

This command recovers a removed runtime.

```
swcli runtime recover <runtime uri>
```

## Copy a runtime

This command copies a runtime to another place, either locally or remotely.

```
swcli runtime copy <source runtime uri> <destination runtime uri>
```

The destination runtime URI should not currently exist and observes the name uniqueness rule.

## Show runtime history

This command shows the history of a runtime.

```
swcli runtime history <runtime uri>
```

## Revert a runtime to an old version

This command reverts a runtime to an old version.

```
swcli runtime revert <runtime uri>
```

# Job Management

## List jobs

This command lists all jobs in the project.

```
swcli job list [project uri]
```

## Get a job's detailed information

This command shows detailed information about a job.

```
swcli job info <job uri>
```

## Pause a job

This command pauses a running job.

```
swcli job pause <job uri>
```

## Resume a job

This command resumes a paused job.

```
swcli job resume <job uri>
```

## Cancel a job

This command cancels a running/paused job.

```
swcli job cancel <job uri>
```

# Utilities

## Garbage collection

This command purges removed entities in the instance. Purged entities are not recoverable.

```
swcli gc [instance uri]
```

The garbage collection aims to keep the storage size within an acceptable range. It keeps removing the oldest removed entity until the total storage size is not greater than a predefined threshold.

# Others

## Special rules for versioned resources

Some resources are versioned, like models, datasets, and runtimes. There are some special rules for commands manipulating these resources.
Versioned resource creation and update

The command for creation and update has the following pattern:

```
swcli model/dataset/runtime build <resource uri> [working dir]
```

If the specified resource URI does not exist, a new resource with one version is automatically created. Otherwise, a new version of the resource is created and becomes the latest version of the resource.
Versioned resource removal and recovery

The commands for removal and recovery have the following pattern:

```
swcli model/dataset/runtime remove/recover <resource uri>
```

If the resource URI has the version part, only the specified version will be removed/recovered. Otherwise, the resource with the whole history will be removed/recovered.

If the resource has only one version and will be removed, the resource will be removed as well.

