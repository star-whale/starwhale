---
title: Dataset
---

## Usage

```bash
swcli [GLOBAL OPTIONS] dataset [OPTIONS] COMMAND [ARGS]...
```

## Summary

- Commands for the dataset lifecycle management.
- For standalone instances, the `dataset` command uses the local disk to build and store Starwhale Datasets.
- For cloud instances, the `dataset` command manages remote datasets through the HTTP API.
- **Dataset URI** in format: `[<Project URI>/dataset/]<dataset name>[/version/<version id>]`.

## All Sub-Commands

  |Command|Standalone|Cloud|
  |-------|----------|-----|
  |build|✅|❌|
  |copy|✅|✅|
  |diff|✅|❌|
  |history|✅|✅|
  |info|✅|✅|
  |list|✅|✅|
  |recover|✅|✅|
  |remove|✅|✅|
  |summary|✅|✅|
  |tag|✅|❌|

## Build a dataset

```bash
swcli dataset build [OPTIONS] WORKDIR
```

- This command builds a dataset within the specified working directory. The working dir must contain a `dataset.yaml`, which defines the metadata and process handler for dataset building.
- Options:

    |Option|Alias Option|Required|Type|Default|Description|
    |------|--------|-------|-----------|-----|-----------|
    |`--project`|`-p`|❌|String|Selected project|Project URI|
    |`--dataset-yaml`|`-f`|❌|String|dataset.yaml|Dataset yaml filename, the default is ${WORKDIR}/dataset.yaml|

- Example:

    ```bash
    ❯ swcli dataset build .
    🚧 start to build dataset bundle...
    👷 uri:local/project/self/dataset/mnist
    🆕 version gvsgemdbhazw
    📁 swds workdir: /home/liutianwei/.cache/starwhale/self/dataset/mnist/gv/gvsgemdbhazwknrtmftdgyjzoaygynq.swds
    👍 try to copy source code files...
    👻 import mnist.process:DataSetProcessExecutor@/home/liutianwei/code/starwhale/example/mnist to make swds...
    >data(t10k-images-idx3-ubyte) split 200 group
    >label(t10k-labels-idx1-ubyte) split 200 group
    cleanup done.
    finish gen swds @ /home/liutianwei/.cache/starwhale/self/dataset/mnist/gv/gvsgemdbhazwknrtmftdgyjzoaygynq.swds/data
    🤖 calculate signature...
    🌺 congratulation! you can run swcli dataset info mnist/version/gvsgemdbhazwknrtmftdgyjzoaygynq
    8 out of 8 steps finished ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━ 100% 0:00:00 0:00:00
    ```

## List datasets

```bash
swcli dataset list [OPTIONS]
```

- This command lists all datasets in the project.
- Options:

    |Option|Alias Option|Required|Type|Default|Description|
    |------|--------|-------|-----------|-----|-----------|
    |`--project`|`-p`|❌|String|Selected project|Project URI|
    |`--fullname`||❌|Boolean|False|Show fullname of dataset version|
    |`--show-removed`||❌|Boolean|False|Show removed datasets|
    |`--page`||❌|Integer|1|Page number for dataset list|
    |`--size`||❌|Integer|20|Page size for dataset list|

## Remove a dataset

```bash
swcli dataset remove [OPTIONS] DATASET
```

- This command removes a dataset. You can run `swcli dataset recover` to recover the removed datasets.
- `DATASET` argument uses the `Dataset URI` format so that you can remove the whole dataset or a specified-version dataset.
- Support the short version or tag in `Dataset URI` format when you remove a dataset.
- Options:

    |Option|Alias Option|Required|Type|Default|Description|
    |------|--------|-------|-----------|-----|-----------|
    |`--force`|`-f`|❌|Boolean|False|Force to remove dataset|

- Example:

    ```bash
    ❯ swcli dataset remove mnist/version/latest
    continue to remove? [y/N]: y
    👏 do successfully
    ```

## Recover a dataset

```bash
swcli dataset recover [OPTIONS] DATASET
```

- This command recovers a removed dataset. You can run `swcli dataset list --show-removed` to fetch removed datasets.
- `DATASET` argument uses the `Dataset URI` format so that you can recover the whole dataset or a specified-version dataset.
- Only support the full version in `Dataset URI` format when you recover a dataset.
- Options:

    |Option|Alias Option|Required|Type|Default|Description|
    |------|--------|-------|-----------|-----|-----------|
    |`--force`|`-f`|❌|Boolean|False|Force to recover dataset|

- Example:

    ```bash
    ❯ swcli dataset recover mnist/version/gvsgemdbhazwknrtmftdgyjzoaygynq
    👏 do successfully
    ```

## Get dataset info

```bash
swcli dataset info [OPTIONS] DATASET
```

- This command inspects the dataset details.
- The `DATASET` argument uses the `Dataset URI` format so that you can inspect the whole dataset or a specified-version dataset.
- Options:

    |Option|Alias Option|Required|Type|Default|Description|
    |------|--------|-------|-----------|-----|-----------|
    |`--fullname`||❌|Boolean|False|Show version fullname|

## Show dataset history

```bash
swcli dataset history [OPTIONS] DATASET
```

- This command shows a dataset's history and lists all its versions.
- `DATASET` argument uses the `Dataset URI` format.
- Options:

    |Option|Alias Option|Required|Type|Default|Description|
    |------|--------|-------|-----------|-----|-----------|
    |`--fullname`||❌|Boolean|False|Show version fullname|

- Example:

    ```bash
    ❯ swcli dataset history mnist --fullname
    ```

## Manage dataset tags

```bash
swcli dataset tag [OPTIONS] DATASET TAGS
```

- This command adds or removes tags on a specified dataset version.
- `DATASET` argument uses the `Dataset URI` format which must include `/version/{version id}` part.
- You can write one or more `TAG` arguments.
- Options:

    |Option|Alias Option|Required|Type|Default|Description|
    |------|--------|-------|-----------|-----|-----------|
    |`--remove`|`-r`|❌|Boolean|False|Remove tags|
    |`--quiet`|`-q`|❌|Boolean|False|Ignore tag name errors like name duplication, name absence|

- Example:

    ```bash
    ❯ swcli dataset tag mnist/version/hfsdmyrtgzst v1 test
    ```

## Copy a dataset

```bash
swcli dataset copy [OPTIONS] SRC DEST
```

- This command copies a dataset to another place, either locally or remotely.
- `SRC` uses `Dataset URI`, which can locate one existing dataset version in the standalone or cloud instance.
- `DEST` uses `Project URI`, which implies the storage project in the destination instance. If the `DEST` project has already stored the same name and version dataset, you can set the `--force` argument to force update.
- Today, this command supports copy datasets from standalone to cloud and from cloud to standalone. `standalone -> standalone` or `cloud -> cloud` is not supported.
- Options:

    |Option|Alias Option|Required|Type|Default|Description|
    |------|--------|-------|-----------|-----|-----------|
    |`--force`|`-f`|❌|Boolean|False|Force to copy dataset|

- Example: copy a dataset from the local standalone instance to a remote cloud instance(upload)

    ```bash
    ❯ swcli dataset copy mnist/version/latest cloud://pre-k8s/project/1
    🚧 start to copy local/project/self/dataset/mnist/version/latest -> http://console.pre.intra.starwhale.ai/project/1...
    ⬆ _manifest.yaml         ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━ 100% 0:00:00 2.2 kB
    ⬆ data_ubyte_0.swds_bin  ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━ 100% 0:00:00 2.1 MB
    ⬆ data_ubyte_1.swds_bin  ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━ 100% 0:00:00 2.1 MB
    ⬆ data_ubyte_2.swds_bin  ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━ 100% 0:00:00 2.1 MB
    ⬆ data_ubyte_3.swds_bin  ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━ 100% 0:00:01 1.8 MB
    ⬆ index.jsonl            ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━ 100% 0:00:01 35.4 kB
    ⬆ label_ubyte_0.swds_bin ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━ 100% 0:00:01 211.3 kB
    ⬆ label_ubyte_1.swds_bin ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━ 100% 0:00:01 211.3 kB
    ⬆ label_ubyte_2.swds_bin ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━ 100% 0:00:01 211.3 kB
    ⬆ label_ubyte_3.swds_bin ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━ 100% 0:00:02 178.8 kB
    ⬆ archive.swds_meta      ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━ 100% 0:00:02 41.0 kB
    👏 copy done
    ```

- Example: copy a dataset from a remote cloud instance to the local standalone instance(download)

    ```bash
    ❯ swcli dataset copy cloud://pre-k8s/project/1/dataset/mnist/version/gvsgemdbhazwknrtmftdgyjzoaygynq self --force
    🚧 start to copy http://console.pre.intra.starwhale.ai/project/1/dataset/mnist/version/gvsgemdbhazwknrtmftdgyjzoaygynq -> local/project/self...
    ⬇ _manifest.yaml         ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━ 100% 0:00:15 2.2 kB
    ⬇ data_ubyte_0.swds_bin  ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━ 100% 0:00:03 2.1 MB
    ⬇ data_ubyte_1.swds_bin  ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━ 100% 0:00:05 2.1 MB
    ⬇ data_ubyte_2.swds_bin  ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━ 100% 0:00:08 2.1 MB
    ⬇ data_ubyte_3.swds_bin  ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━ 100% 0:00:10 1.8 MB
    ⬇ index.jsonl            ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━ 100% 0:00:10 35.4 kB
    ⬇ label_ubyte_0.swds_bin ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━ 100% 0:00:10 211.3 kB
    ⬇ label_ubyte_1.swds_bin ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━ 100% 0:00:10 211.3 kB
    ⬇ label_ubyte_2.swds_bin ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━ 100% 0:00:10 211.3 kB
    ⬇ label_ubyte_3.swds_bin ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━ 100% 0:00:10 178.8 kB
    ⬇ archive.swds_meta      ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━ 100% 0:00:10 41.0 kB
    👏 copy done
    ```
