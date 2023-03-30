---
title: 数据集构建和加载
---

## starwhale.SWDSBinBuildExecutor

提供swds-bin格式的数据集构建类，需要用户实现 `iter_item` 函数，返回一个可迭代的对象，包含data和annotations。Github上的[代码链接](https://github.com/star-whale/starwhale/blob/dc6e6fdeae2f7c5bd0e72ccd8fb50768b1ce0826/client/starwhale/api/_impl/dataset/builder.py#L138)。

```python
class DatasetProcessExecutor(SWDSBinBuildExecutor):
    def iter_item(self) -> t.Generator[t.Tuple[t.Any, t.Any], None, None]:
        ...
```

`iter_item` 返回一个可迭代的对象，通常写法是for循环中，yield data和annotations。对于swds-bin格式的数据集，data一般为 `Audio`，`Image`，`Text`、`GrayscaleImage`和`Binary`。也接受用户yield bytes类型的data，会自动转化成 `Binary` 类型。以[MNIST](https://github.com/star-whale/starwhale/tree/dc6e6fdeae2f7c5bd0e72ccd8fb50768b1ce0826/example/mnist)为例，构建swds的数据集基本代码如下：

```python
import struct
import typing as t
from pathlib import Path

from starwhale import (
    Link,
    GrayscaleImage,
    SWDSBinBuildExecutor,
)

class DatasetProcessExecutor(SWDSBinBuildExecutor):
    def iter_item(self) -> t.Generator[t.Tuple[t.Any, t.Any], None, None]:
        root_dir = Path(__file__).parent.parent / "data"

        with (root_dir / "t10k-images-idx3-ubyte").open("rb") as data_file, (
            root_dir / "t10k-labels-idx1-ubyte"
        ).open("rb") as label_file:
            _, data_number, height, width = struct.unpack(">IIII", data_file.read(16))
            _, label_number = struct.unpack(">II", label_file.read(8))
            print(
                f">data({data_file.name}) split data:{data_number}, label:{label_number} group"
            )
            image_size = height * width

            for i in range(0, min(data_number, label_number)):
                _data = data_file.read(image_size)
                _label = struct.unpack(">B", label_file.read(1))[0]
                yield GrayscaleImage(
                    _data,
                    display_name=f"{i}",
                    shape=(height, width, 1),
                ), {"label": _label}
```

## starwhale.UserRawBuildExecutor

提供remote-link和user-raw格式的数据集构建类，需要用户实现 `iter_item` 函数，返回一个可迭代的对象，包含data和annotations，其中data需要是一个 `starwhale.Link` 类型。Github上的[代码链接](https://github.com/star-whale/starwhale/blob/dc6e6fdeae2f7c5bd0e72ccd8fb50768b1ce0826/client/starwhale/api/_impl/dataset/builder.py#L307)。

```python
class RawDatasetProcessExecutor(UserRawBuildExecutor):
    def iter_item(self) -> t.Generator[t.Tuple[t.Any, t.Any], None, None]:
        ...
```

以[Speech Commands](https://github.com/star-whale/starwhale/tree/main/example/speech_command)为例，构建remote-link的数据集基本代码如下：

```python
import typing as t
from pathlib import Path

from starwhale import (
    Link,
    Audio,
    MIMEType,
    S3LinkAuth,
    UserRawBuildExecutor,
)
class LinkRawDatasetBuildExecutor(UserRawBuildExecutor):

    _auth = S3LinkAuth(
        name="speech", access_key="minioadmin", secret="minioadmin", region="local"
    )
    _addr = "10.131.0.1:9000"
    _bucket = "users"

    def iter_item(self) -> t.Generator[t.Tuple[t.Any, t.Any], None, None]:
        import boto3
        from botocore.client import Config

        s3 = boto3.resource(
            "s3",
            endpoint_url=f"http://{self._addr}",
            aws_access_key_id=self._auth.access_key,
            aws_secret_access_key=self._auth.secret,
            config=Config(signature_version="s3v4"),
            region_name=self._auth.region,
        )

        objects = s3.Bucket(self._bucket).objects.filter(
            Prefix="dataset/SpeechCommands/speech_commands_v0.02"
        )

        for obj in objects:
            path = Path(obj.key)  # type: ignore
            command = path.parent.name
            if (
                command == "_background_noise_"
                or "_nohash_" not in path.name
                or obj.size < 10240
                or not path.name.endswith(".wav")
            ):
                continue

            speaker_id, utterance_num = path.stem.split("_nohash_")
            uri = f"s3://{self._addr}/{self._bucket}/{obj.key.lstrip('/')}"
            data = Link(
                uri,
               self._auth,
                size=obj.size,
                data_type=Audio(
                    display_name=f"{command}/{path.name}",
                    mime_type=MIMEType.WAV,
                    shape=(1,),
                ),
            )
            annotations = {
                "label": command,
                "speaker_id": speaker_id,
                "utterance_num": int(utterance_num),
            }
            yield data, annotations
```

## starwhale.BuildExecutor

`SWDSBinBuildExecutor` 类的别称，同为swds-bin格式的数据集构建类。
