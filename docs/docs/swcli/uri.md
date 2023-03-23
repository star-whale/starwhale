---
title: Starwhale Resources URI
---

:::tip
**Resource URI is widely used in Starwhale client commands. The URI can refer to a resource in the local instance or any other resource in a remote instance. In this way, the Starwhale client can easily manipulate any resource.**
:::

![concepts-org.jpg](../img/concepts-org.jpg)

## Instance URI

Instance URI can be either:

- `local`: standalone instance.
- `[http(s)://]<hostname or ip>[:<port>]`: cloud instance with HTTP address.
- `[cloud://]<cloud alias>`: cloud instance with an alias name, which can be configured in the instance login phase.

:::caution
"local" is different from "localhost". The former means the local standalone instance without a controller, while the latter implies a controller listening at the default port 8082 on the localhost.|
:::

Example:

```bash
# log in Starwhale Cloud, the alias is swcloud
swcli instance login --username <your account name> --password <your password> https://cloud.starwhale.ai --alias swcloud
# copy a model from the local instance to the cloud instance
swcli model copy mnist/version/latest swcloud/project/<your account name>/demo
# copy a runtime to a Starwhale Server instance: http://localhost:8081
swcli runtime copy pytorch/version/v1 http://localhost:8081/project/<your account name>/demo
```

## Project URI

Project URI is in the format `[<Instance URI>/project/]<project name>`. If the instance URI is not specified, use the current instance instead.

Example:

```bash
swcli project select self   # select self project in the current instance
swcli project info local/project/self  # inspect self project info in the local instance
```

## Model/Dataset/Runtime URI

- Model URI: `[<Project URI>/model/]<model name>[/version/<version id|tag>]`.
- Dataset URI: `[<Project URI>/dataset/]<dataset name>[/version/<version id|tag>]`.
- Runtime URI: `[<Project URI>/runtime/]<runtime name>[/version/<version id|tag>]`.
- `swcli` supports human-friendly short version id. You can just type the first few characters of the verison id, provided that it is at least four characters long and unambiguous. However, the `recover` command must use the full version id.
- If the project URI is not specified, the default project will be used.
- You can always use version tag instead of version id.

Example:

```bash
swcli model info mnist/version/hbtdenjxgm4ggnrtmftdgyjzm43tioi  # inspect model info, model name: mnist, version:hbtdenjxgm4ggnrtmftdgyjzm43tioi
swcli model remove mnist/version/hbtdenj  # short version
swcli model info mnist  # inspect mnist model info
swcli job create --model mnist/version/latest --runtime pytorch-mnist/version/latest --dataset mnist/version/latest # use version tag
```

## Evaluation URI

- format: `[<Project URI>/eval/]<evaluation id>`.
- If the project URI is not specified, the default project will be used.

Example:

```bash
swcli eval info mezdayjzge3w   # Inspect mezdayjzge3w version in default instance and default project
swcli eval info local/project/self/eval/mezday # Inspect the local instance, self project, with short evaluation id:mezday
```
