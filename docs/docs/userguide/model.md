---
title: Starwhale Model User Guide
---

A Starwhale Model is a standard format for packaging machine learning models that can be used for various purposes, like model finetuning, model evaluation, and online serving. A Starwhale Model contains the model file, inference codes, configuration files, and any other files required to run the model.

For more information about the packaging format, see [the storage format](#format).

## Create a Starwhale Model

There are two ways to create a Starwhale Model: by [SWCLI](../reference/swcli/model.md) or by SDK.

### Create a Starwhale Model by SWCLI

To create a Starwhale Model by SWCLI, you need to define a model.yaml, which describes some required information about the model package, and run the following command:

```bash
swcli model build <path to your model.yaml directory>
```

For more information about the command and model.yaml, see [the SWCLI reference](../reference/swcli/model.md#build)

### Create a Starwhale Model by SDK

## Model Management

### Model Management by SWCLI

| Command | Description |
| ------- | ----------- |
| [`swcli model list`](../reference/swcli/model.md#list) | List all Starwhale Models in a project |
| [`swcli model info`](../reference/swcli/model.md#info) | Show detail information about a Starwhale Model |
| [`swcli model copy`](../reference/swcli/model.md#copy) | Copy a Starwhale Model to another location |
| [`swcli model remove`](../reference/swcli/model.md#remove) | Remove a Starwhale Model |
| [`swcli model recover`](../reference/swcli/model.md#recover) | Recover a previously removed Starwhale Model |

### Model Management by WebUI

## Model History

Starwhale Models are versioned. The general rules about versions are described in [Resource versioning in Starwhale](../concepts/versioning.md).

## Model History Management by SWCLI

| Command | Description |
| ------- | ----------- |
| [`swcli model history`](../reference/swcli/model.md#list) | List all versions of a Starwhale Model |
| [`swcli model info`](../reference/swcli/model.md#info) | Show detail information about a Starwhale Model version |
| [`swcli model diff`](../reference/swcli/model.md#diff) | Compare two versions of a Starwhale model |
| [`swcli model copy`](../reference/swcli/model.md#copy) | Copy a Starwhale Model version to a new one |
| [`swcli model remove`](../reference/swcli/model.md#remove) | Remove a Starwhale Model version |
| [`swcli model recover`](../reference/swcli/model.md#recover) | Recover a previously removed Starwhale Model version |

## Model History Management by WebUI

## Model Evaluation

### Model Evaluation by SWCLI

| Command | Description |
| ------- | ----------- |
| [`swcli model eval`](../reference/swcli/model.md#eval) | Create an evaluation with a Starwhale Model |

### Model Evaluation by WebUI

#### Model Online Evaluation by WebUI

## The Storage Format {#format}

The Starwhale Model is a tarball file that contains the source directory.
