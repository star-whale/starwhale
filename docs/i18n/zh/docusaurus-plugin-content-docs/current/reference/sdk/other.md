---
title: 其他SDK
---

## starwhale.\__version__

Starwhale SDK和Cli版本，是字符串常量。

```python
>>> from starwhale import __version__
>>> print(__version__)
0.3.0rc10
```

## starwhale.URI

starwhale uri的类定义，可以将字符串转化成URI对象。Github上的[代码链接](https://github.com/star-whale/starwhale/blob/dc6e6fdeae2f7c5bd0e72ccd8fb50768b1ce0826/client/starwhale/base/uri.py)。

```python
URI(
    raw: str,
    expected_type: str = URIType.UNKNOWN
)
```

|参数|说明|
|---|---|
|`raw`| starwhale uri的字符串 |
|`expected_type`| 可以对有歧义的uri字符串强制指定为某种类型 |

```python
>>> dataset_uri = URI("mnist/version/latest", expected_type=URIType.DATASET)
>>> model_uri = URI("mnist/version/latest", expected_type=URIType.MODEL)
>>> runtime_uri = URI("mnist/version/latest", expected_type=URIType.RUNTIME)
>>> dataset_uri = URI("dataset/mnist/version/latest")
```

上面例子中，uri的原始字符串都是 `mnist/version/latest`，这是一个有歧义的URI，但当指定了 `expected_type` 参数后，可以明确指定为预期的URI。

## starwhale.URIType

描述 `starwhale.URI` 类型，Github上的[代码链接](https://github.com/star-whale/starwhale/blob/dc6e6fdeae2f7c5bd0e72ccd8fb50768b1ce0826/client/starwhale/base/type.py)。

```python
class URIType:
    INSTANCE = "instance"
    PROJECT = "project"
    MODEL = "model"
    DATASET = "dataset"
    RUNTIME = "runtime"
    EVALUATION = "evaluation"
    UNKNOWN = "unknown"
```
