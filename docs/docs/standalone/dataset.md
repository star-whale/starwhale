---
title: Starwhale Dataset
---

## dataset.yaml Definition

|Field|Description|Required|Default Value|Type|Example|
|-----|-----------|--------|-------------|-------|
|`version`|starwhale api version, today only support 1.0|❌|`1.0`|String|`1.0`|
|`name`|starwhale dataset name|✅||String|`mnist`|
|`data_dir`|data directory|✅||String|`data`|
|`data_filer`|the filter for data files, support regular expression|✅||string|`t10k-image*`|
|`label_filer`|the filter for label files, support regular expression|✅||string|`t10k-label*`|
|`process`|the class import path which is inherited by `starwhale.api.dataset.BuildExecutor` class. The format is {module path}:{class name}|✅||String|`mnist.process:DataSetProcessExecutor`|
|`desc`|description|❌|""|String|`This is a mnist dataset.`|
|`attr.batch_size`|data batch size|❌|`50`|Integer|`50`|
|`attr.alignment_size`|every section data alignment size|❌|`4k`|String|`4k`|
|`attr.volume_size`|data volume size|❌|`64M`|String|`2M`|

Example:

```yaml
name: mnist

data_dir: data
data_filter: "t10k-image*"
label_filter: "t10k-label*"
process: mnist.process:DataSetProcessExecutor

desc: MNIST data and label test dataset
attr:
  batch_size: 50
  alignment_size: 4k
  volume_size: 2M
```
