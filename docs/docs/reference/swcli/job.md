---
title: swcli job
---

## Overview

```bash
swcli [GLOBAL OPTIONS] job [OPTIONS] <SUBCOMMAND> [ARGS]...
```

The `job` command includes the following subcommands:

* `cancel`
* `info`
* `list`
* `pause`
* `recover`
* `remove`
* `resume`

## swcli job cancel {#cancel}

```bash
swcli [GLOBAL OPTIONS] job cancel [OPTIONS] <JOB>
```

`job cancel` stops the specified job.

`JOB` is a [job URI](../../swcli/uri.md#job).

| Option | Required | Type | Defaults | Description |
| --- | --- | --- | --- | --- |
| `--force` or `-f` | ❌ | Boolean | False | If true, kill the Starwhale Job by force. |

## swcli job info {#info}

```bash
swcli [GLOBAL OPTIONS] job info [OPTIONS] <JOB>
```

`job info` outputs detailed information about the specified Starwhale Job.

`JOB` is a [job URI](../../swcli/uri.md#job).

| Option | Required | Type | Defaults | Description |
| --- | --- | --- | --- | --- |
| `--page` | ❌ | Integer | 1 | The starting page number.  Server and cloud instances only. |
| `--size` | ❌ | Integer | 20 | The number of items in one page. Server and cloud instances only. |

## swcli job list {#list}

```bash
swcli [GLOBAL OPTIONS] job list [OPTIONS]
```

`job list` shows all Starwhale Jobs.

| Option | Required | Type | Defaults | Description |
| --- | --- | --- | --- | --- |
| `--project` | ❌ | String | | The URI of the project to list. Use the [default project](../../swcli/uri.md#defaultProject) if not specified. |
| `--show-removed` or `-sr` | ❌ | Boolean | False | If true, include packages that are removed but not garbage collected. |
| `--page` | ❌ | Integer | 1 | The starting page number.  Server and cloud instances only. |
| `--size` | ❌ | Integer | 20 | The number of items in one page. Server and cloud instances only. |

## swcli job pause {#pause}

```bash
swcli [GLOBAL OPTIONS] job pause [OPTIONS] <JOB>
```

`job pause` pauses the specified job. Paused jobs can be resumed by `job resume`.

`JOB` is a [job URI](../../swcli/uri.md#job).

From Starwhale's perspective, `pause` is almost the same as `cancel`, except that the job reuses the old Job id when resumed. It is job developer's responsibility to save all data periodically and load them when resumed. The job id is usually used as a key of the checkpoint.

| Option | Required | Type | Defaults | Description |
| --- | --- | --- | --- | --- |
| `--force` or `-f` | ❌ | Boolean | False | If true, kill the Starwhale Job by force. |

## swcli job resume {#resume}

```bash
swcli [GLOBAL OPTIONS] job resume [OPTIONS] <JOB>
```

`job resume` resumes the specified job.

`JOB` is a [job URI](../../swcli/uri.md#job).
