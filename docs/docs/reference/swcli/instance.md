---
title: swcli instance
---

## Overview

```bash
swcli [GLOBAL OPTIONS] instance [OPTIONS] <SUBCOMMAND> [ARGS]
```

The `instance` command includes the following subcommands:

* `info`
* `list`
* `login`
* `logout`
* `use`

## swcli instance info {#info}

```bash
swcli [GLOBAL OPTIONS] instance info [OPTIONS] <INSTANCE>
```

`instance info` outputs detailed information about the specified Starwhale Instance.

`INSTANCE` is an [instance URI](../../swcli/uri.md#instance).

## swcli instance list {#list}

```bash
swcli [GLOBAL OPTIONS] instance list [OPTIONS]
```

`instance list` shows all Starwhale Instances.

## swcli instance login {#login}

```bash
swcli [GLOBAL OPTIONS] instance login [OPTIONS] <INSTANCE>
```

`instance login` connects to a Server/Cloud instance and makes the specified instance [default](../../swcli/uri.md#defaultInstance).

`INSTANCE` is an [instance URI](../../swcli/uri.md#instance).

| Option | Required | Type | Defaults | Description |
| --- | --- | --- | --- | --- |
| `--username` | ❌ | String | | The login username. |
| `--password` | ❌ | String | | The login password. |
| `--token` | ❌ | String | | The login token. |
| `--alias` | ✅ | String | | The alias of the instance. You can use it anywhere that requires an instance URI. |

`--username` and `--password` can not be used together with `--token`.

## swcli instance logout {#logout}

```bash
swcli [GLOBAL OPTIONS] instance logout [INSTANCE]
```

`instance logout` disconnects from the Server/Cloud instance, and clears information stored in the local storage.

`INSTANCE` is an [instance URI](../../swcli/uri.md#instance). If it is omiited, the [default instance](../../swcli/uri.md#defaultInstance) is used instead.

## swcli instance use {#use}

```bash
swcli [GLOBAL OPTIONS] instance use <INSTANCE>
```

`instance use` make the specified instance [default](../../swcli/uri.md#defaultInstance).

`INSTANCE` is an [instance URI](../../swcli/uri.md#instance).
