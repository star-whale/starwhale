---
title: Overview
---

## Concepts

The Starwhale standalone instance includes almost all **Starwhale Concepts and workflows**. Starwhale keeps concepts consistent across different types of instances. So you can easily exchange data and migrate your work between them.

- For **ML Basic Elements**:

  - **Starwhale Runtime**: Starwhale standalone defines a new environment bundle format with `runtime.yaml`, which includes python libs, native libs, and native binaries. You can use **Starwhale Runtime** to create a shareable and system-independent running environment.
  - **Starwhale Model**: **Starwhale Model** is not only the Machine Learning or Deep Learning trained model files. It is described by `model.yaml`, and includes trained model files, pipeline code, configurations, hyperparameter files, and other related files. **Starwhale Model** is a bundle file with the `.swmp` extension.
  - **Starwhale Dataset**: You can use `dataset.yaml` and a few lines of python code to create a **Starwhale Dataset** from your data files with the help of Starwhale SDK. **Starwhale Dataset** is a unified description of how the data and labels are stored and organized. **Starwhale Dataset** makes data loading efficient and easy to use.

- For **Instances Operations**:

  - **Exchange Data**: the **ML Basic Elements** can be exchanged between different Starwhale instances by the `copy` command.
  - **Unified Terminal View**: `swcli` is a developer-friendly command-line tool that provides a unified operation terminal view between standalone instance and cloud instance. You can use `swcli` to manage multi instances.

- For **ML Workflows**:

  - **Model Evaluation**: Starwhale standalone provides the out-of-the-box model evaluation toolkit that includes Python SDK, analysis report decorators, and other commands. You can finish the whole evaluation workflow of MNIST in ten minutes.
  - Model training and serving components are still in development.

## Starwhale Resource URI

**Resource URI is widely used in Starwhale client commands. The URI can refer to a resource in the local instance or any other resource in a remote instance. In this way, the Starwhale client can easily manipulate any resource.**

### Instance URI

Instance URI can be either:

- `local`: standalone instance.
- `[http(s)://]<hostname or ip>[:<port>]`: cloud instance with HTTP address.
- `[cloud://]<cloud alias>`: cloud instance with an alias name, which can be configured in the instance login phase.

:::caution
"local" is different from "localhost". The former means the local standalone instance without a controller, while the latter implies a controller listening at the default port 7827 on localhost.|
:::

Example:

```bash
# login http://console.pre.intra.starwhale.ai instance, the alias is pre-k8s
swcli instance login --username starwhale --password abcd1234 http://console.pre.intra.starwhale.ai --alias pre-k8s
# copy model from the local instance, default project into cloud instance, instance field uses the alias name: pre-k8s.
swcli model copy mnist/version/latest cloud://pre-k8s/project/1
# copy runtime into cloud instance: localhost:8081
swcli runtime copy pytorch/version/v1.0 http://localhost:8081/project/myproject
```

### Project URI

Project URI is in the format `[<Instance URI>/project/]<project name>`. If the instance URI is not specified, use the default instance instead.

Example:

```bash
swcli project select self   # select self project in the current instance
swcli project info local/project/self  # inspect self project info in the local instance
```

### Model/Dataset/Runtime URI

- Model URI: `[<Project URI>/model/]<model name>[/version/<version id|tag>]`.
- Dataset URI: `[<Project URI>/dataset/]<dataset name>[/version/<version id|tag>]`.
- Runtime URI: `[<Project URI>/runtime/]<runtime name>[/version/<version id|tag>]`.
- `swcli` supports short version which is at least five characters, but `recover` command must use full version.
- If the project URI is not specified, use the default project.
- The version id field also supports tag.

Example:

```bash
swcli model info mnist/version/hbtdenjxgm4ggnrtmftdgyjzm43tioi  # inspect model info, model name: mnist, version:hbtdenjxgm4ggnrtmftdgyjzm43tioi
swcli model remove mnist/version/hbtdenj  # short version
swcli model info mnist  # inspect mnist model info
swcli job create --model mnist/version/latest --runtime pytorch-mnist/version/latest --dataset mnist/version/latest
```

### Job URI

- format: `[<Project URI>/job/]<job id>`.
- If the project URI is not specified, use the default project.

Example:

```bash
swcli job info mezdayjzge3w   # Inspect mezdayjzge3w version in default instance and default project
swcli job info local/project/self/job/mezday # Inspect the local instance, self project, with short job version:mezday
```

## Names Limitation

Names mean project names, model names, dataset names, runtime names, and tag names.

- Names are case-insensitive.
- A name MUST only consist of letters `A-Z a-z`, digits `0-9`, the hyphen character `-`, the dot character `.`, and the underscore character `_`.
- A name should always start with a letter or the `_` character.
- The maximum length of a name is 80.

### Names uniqueness requirement

The resource name should be a unique string within its owner. For example, the project name should be unique in the owner instance, and the model name should be unique in the owner project.

The resource name can not be used by any other resource of the same kind in the owner, including those removed ones. For example, Project "apple" can not have two models named "Alice", even if one of them is already removed.

Different kinds of resources can have the same name. For example, a project and a model can have the same name "Alice".

Resources with different owners can have the same name. For example, a model in project "Apple" and a model in project "Banana" can have the same name "Alice".

Garbage collected resources' names can be reused. For example, after the model with the name "Alice" in project "Apple" is removed and garbage collected, the project can have a new model with the same name "Alice".

## Standalone Guide Keywords

The key words `MUST`, `MUST NOT`, `REQUIRED`,`SHALL`, `SHALL NOT`, `SHOULD`, `SHOULD NOT`, `RECOMMENDED`, `MAY` and `OPTIONAL` in the Starwhale standalone guide documents are to be interpreted as described in [RFC 2119](https://datatracker.ietf.org/doc/html/rfc2119).
