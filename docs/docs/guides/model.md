---
title: Model
---
## model.yaml Definition

|Field|Description|Required|Default Value|Type|Example|
|-----|-----------|--------|-------------|-------|
|`name`|starwhale model name|✅||String|`mnist`|
|`version`|starwhale api version, today only support 1.0|❌|`1.0`|String|`1.0`|
|`config`|config files|❌|None|List[String]|`- config/hyperparam.json`|
|`desc`|description|❌|""|String|`This is a mnist model evaluation.`|
|`run.ppl`|the class import path which is inherited by `starwhale.api.model.PipelineHandler` class. The format is {module path}:{class name}|✅||String|`mnist.ppl:MNISTInference`|
|`run.pkg_data`|except fo python scripts, model files defined by `model` field and config files defined by `config` field, other type files you want to package into starwhale model bundle file.|❌|None|List[String]|`- *.sh`|
|`envs`|environments|❌|None|List[String]|`- DEBUG=1`|

Example:

```yaml
name: mnist

run:
  ppl: mnist.ppl:MNISTInference
```
