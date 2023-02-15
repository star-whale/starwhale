---
title: 数据类型
---

## 1. starwhale.COCOObjectAnnotation

提供COCO类型的定义，Github上的[代码链接](https://github.com/star-whale/starwhale/blob/dc6e6fdeae2f7c5bd0e72ccd8fb50768b1ce0826/client/starwhale/core/dataset/type.py#L403)。

```python
COCOObjectAnnotation(
    id: int,
    image_id: int,
    category_id: int,
    segmentation: Union[t.List, t.Dict],
    area: Union[float, int],
    bbox: Union[BoundingBox, t.List[float]],
    iscrowd: int,
)
```

|参数|说明|
|---|---|
|`id`|object id，一般为全局object的递增id|
|`image_id`|image id，一般为图片id|
|`category_id`|category id，一般为目标检测中类别的id|
|`segmentation`|物体轮廓表示，Polygon(多边形的点)或RLE格式|
|`area`|object面积|
|`bbox`|表示bounding box，可以为BoundingBox类型或float的列表|
|`iscrowd`|0表示是一个单独的object，1表示两个没有分开的object|

### 1.1 使用示例

```python
def _make_coco_annotations(
    self, mask_fpath: Path, image_id: int
) -> t.List[COCOObjectAnnotation]:
    mask_img = PILImage.open(str(mask_fpath))

    mask = np.array(mask_img)
    object_ids = np.unique(mask)[1:]
    binary_mask = mask == object_ids[:, None, None]
    # TODO: tune permute without pytorch
    binary_mask_tensor = torch.as_tensor(binary_mask, dtype=torch.uint8)
    binary_mask_tensor = (
        binary_mask_tensor.permute(0, 2, 1).contiguous().permute(0, 2, 1)
    )

    coco_annotations = []
    for i in range(0, len(object_ids)):
        _pos = np.where(binary_mask[i])
        _xmin, _ymin = float(np.min(_pos[1])), float(np.min(_pos[0]))
        _xmax, _ymax = float(np.max(_pos[1])), float(np.max(_pos[0]))
        _bbox = BoundingBox(
            x=_xmin, y=_ymin, width=_xmax - _xmin, height=_ymax - _ymin
        )

        rle: t.Dict = coco_mask.encode(binary_mask_tensor[i].numpy())  # type: ignore
        rle["counts"] = rle["counts"].decode("utf-8")

        coco_annotations.append(
            COCOObjectAnnotation(
                id=self.object_id,
                image_id=image_id,
                category_id=1,  # PennFudan Dataset only has one class-PASPersonStanding
                segmentation=rle,
                area=_bbox.width * _bbox.height,
                bbox=_bbox,
                iscrowd=0,  # suppose all instances are not crowd
            )
        )
        self.object_id += 1

    return coco_annotations
```

## 2. starwhale.GrayscaleImage

提供灰度图类型，比如MNIST中数字手写体图片，是 `Image` 类型的一个特例。Github上的[代码链接](https://github.com/star-whale/starwhale/blob/dc6e6fdeae2f7c5bd0e72ccd8fb50768b1ce0826/client/starwhale/core/dataset/type.py#L301)。

```python
GrayscaleImage(
    fp: _TArtifactFP = "",
    display_name: str = "",
    shape: Optional[_TShape] = None,
    as_mask: bool = False,
    mask_uri: str = "",
)
```

|参数|说明|
|---|---|
|`fp`|图片的路径、IO对象或文件内容的bytes|
|`display_name`|Dataset Viewer上展示的名字|
|`shape`|图片的Width和Height，channel默认为1|
|`as_mask`|是否作为Mask图片|
|`mask_uri`|Mask原图的URI|

### 2.1 使用示例

```python
for i in range(0, min(data_number, label_number)):
    _data = data_file.read(image_size)
    _label = struct.unpack(">B", label_file.read(1))[0]
    yield GrayscaleImage(
        _data,
        display_name=f"{i}",
        shape=(height, width, 1),
    ), {"label": _label}
```

### 2.2 函数

#### 2.2.1 to_types

```python
to_bytes(encoding: str= "utf-8") -> bytes
```

#### 2.2.2 carry_raw_data

```python
carry_raw_data() -> GrayscaleImage
```

#### 2.2.3 astype

```python
astype() -> Dict[str, t.Any]
```

## 3. starwhale.BoundingBox

提供边界框类型，目前为 `LTWH` 格式，即 `left_x`, `top_y`, `width` 和 `height`。Github上的[代码链接](https://github.com/star-whale/starwhale/blob/dc6e6fdeae2f7c5bd0e72ccd8fb50768b1ce0826/client/starwhale/core/dataset/type.py#L363)。

```python
BoundingBox(
    x: float,
    y: float,
    width: float,
    height: float
)
```

|参数|说明|
|---|---|
|`x`|left_x的坐标|
|`y`|top_y的坐标|
|`width`|图片的宽度|
|`height`|图片的高度|

## 4. starwhale.ClassLabel

描述label的数量和类型，Github上的[代码链接](https://github.com/star-whale/starwhale/blob/dc6e6fdeae2f7c5bd0e72ccd8fb50768b1ce0826/client/starwhale/core/dataset/type.py#L344)。

```python
ClassLabel(
     names: List[Union[int, float, str]]
)
```

## 5. starwhale.Image

图片类型，Github上的[代码链接](https://github.com/star-whale/starwhale/blob/dc6e6fdeae2f7c5bd0e72ccd8fb50768b1ce0826/client/starwhale/core/dataset/type.py#L267)。

```python
Image(
    fp: _TArtifactFP = "",
    display_name: str = "",
    shape: Optional[_TShape] = None,
    mime_type: Optional[MIMEType] = None,
    as_mask: bool = False,
    mask_uri: str = "",
)
```

|参数|说明|
|---|---|
|`fp`|图片的路径、IO对象或文件内容的bytes|
|`display_name`|Dataset Viewer上展示的名字|
|`shape`|图片的Width、Height和channel|
|`mime_type`|MIMEType支持的类型|
|`as_mask`|是否作为Mask图片|
|`mask_uri`|Mask原图的URI|

### 5.1 使用示例

```python
import io
import typing as t
import pickle
from PIL import Image as PILImage
from starwhale import Image, MIMEType

def _iter_item(paths: t.List[Path]) -> t.Generator[t.Tuple[t.Any, t.Dict], None, None]:
    for path in paths:
        with path.open("rb") as f:
            content = pickle.load(f, encoding="bytes")
            for data, label, filename in zip(
                content[b"data"], content[b"labels"], content[b"filenames"]
            ):
                annotations = {
                    "label": label,
                    "label_display_name": dataset_meta["label_names"][label],
                }

                image_array = data.reshape(3, 32, 32).transpose(1, 2, 0)
                image_bytes = io.BytesIO()
                PILImage.fromarray(image_array).save(image_bytes, format="PNG")

                yield Image(
                    fp=image_bytes.getvalue(),
                    display_name=filename.decode(),
                    shape=image_array.shape,
                    mime_type=MIMEType.PNG,
                ), annotations


```

### 5.2 函数

#### 5.2.1 to_types

```python
to_bytes(encoding: str= "utf-8") -> bytes
```

#### 5.2.2 carry_raw_data

```python
carry_raw_data() -> GrayscaleImage
```

#### 5.2.3 astype

```python
astype() -> Dict[str, t.Any]
```

## 6. starwhale.Audio

音频类型，Github上的[代码链接](https://github.com/star-whale/starwhale/blob/dc6e6fdeae2f7c5bd0e72ccd8fb50768b1ce0826/client/starwhale/core/dataset/type.py#L324)。

```python
Audio(
    fp: _TArtifactFP = "",
    display_name: str = "",
    shape: Optional[_TShape] = None,
    mime_type: Optional[MIMEType] = None,
)
```

|参数|说明|
|---|---|
|`fp`|图片的路径、IO对象或文件内容的bytes|
|`display_name`|Dataset Viewer上展示的名字|
|`shape`|图片的Width、Height和channel|
|`mime_type`|MIMEType支持的类型|

### 6.1 使用示例

```python
import typing as t
from starwhale import Audio

def iter_item() -> t.Generator[t.Tuple[t.Any, t.Any], None, None]:
    for path in validation_ds_paths:
        with path.open() as f:
            for item in f.readlines():
                item = item.strip()
                if not item:
                    continue

                data_path = dataset_dir / item
                data = Audio(
                    data_path, display_name=item, shape=(1,), mime_type=MIMEType.WAV
                )

                speaker_id, utterance_num = data_path.stem.split("_nohash_")
                annotations = {
                    "label": data_path.parent.name,
                    "speaker_id": speaker_id,
                    "utterance_num": int(utterance_num),
                }
                yield data, annotations
```

### 6.2 函数

#### 6.2.1 to_types

```python
to_bytes(encoding: str= "utf-8") -> bytes
```

#### 6.2.2 carry_raw_data

```python
carry_raw_data() -> GrayscaleImage
```

#### 6.2.3 astype

```python
astype() -> Dict[str, t.Any]
```

## 7. starwhale.Text

文本类型，默认为 `utf-8` 格式。Github上的[代码链接](https://github.com/star-whale/starwhale/blob/dc6e6fdeae2f7c5bd0e72ccd8fb50768b1ce0826/client/starwhale/core/dataset/type.py#L380)。

```python
Text(
    content: str,
    encoding: str = "utf-8",
)
```

|参数|说明|
|---|---|
|`content`|text内容|
|`encoding`|text的编码格式|

### 7.1 使用示例

```python
import typing as t
from pathlib import Path
from starwhale import Text

def iter_item(self) -> t.Generator[t.Tuple[t.Any, t.Any], None, None]:
    root_dir = Path(__file__).parent.parent / "data"

    with (root_dir / "fra-test.txt").open("r") as f:
        for line in f.readlines():
            line = line.strip()
            if not line or line.startswith("CC-BY"):
                continue

            _data, _label, *_ = line.split("\t")
            data = Text(_data, encoding="utf-8")
            annotations = {"label": _label}
            yield data, annotations
```

### 7.2 函数

#### 7.2.1 to_types

```python
to_bytes(encoding: str= "utf-8") -> bytes
```

#### 7.2.2 carry_raw_data

```python
carry_raw_data() -> GrayscaleImage
```

#### 7.2.3 astype

```python
astype() -> Dict[str, t.Any]
```

#### 7.2.4 to_str

```python
to_str() -> str
```

## 8. starwhale.Binary

二进制类型，用bytes存储，Github上的[代码链接](https://github.com/star-whale/starwhale/blob/dc6e6fdeae2f7c5bd0e72ccd8fb50768b1ce0826/client/starwhale/core/dataset/type.py#L258)。

```python
Binary(
    fp: _TArtifactFP = "",
    mime_type: MIMEType = MIMEType.UNDEFINED,
)
```

|参数|说明|
|---|---|
|`fp`|路径、IO对象或文件内容的bytes|
|`mime_type`|MIMEType支持的类型|

### 8.1 函数

#### 8.1.1 to_types

```python
to_bytes(encoding: str= "utf-8") -> bytes
```

#### 8.1.2 carry_raw_data

```python
carry_raw_data() -> GrayscaleImage
```

#### 8.1.3 astype

```python
astype() -> Dict[str, t.Any]
```

## 9. starwhale.Link

Link类型，用来制作 `remote-link` 和 `user-raw` 类型的数据集。Github上的[代码链接](https://github.com/star-whale/starwhale/blob/dc6e6fdeae2f7c5bd0e72ccd8fb50768b1ce0826/client/starwhale/core/dataset/type.py#L432)。

```python
Link(
    uri: str,
    auth: Optional[LinkAuth] = DefaultS3LinkAuth,
    offset: int = 0,
    size: int = -1,
    data_type: Optional[BaseArtifact] = None,
    with_local_fs_data: bool = False,
)
```

|参数|说明|
|---|---|
|`uri`|原始数据的uri地址，目前支持localFS和S3两种协议|
|`auth`|Link Auth信息|
|`offset`|数据相对uri指向的文件偏移量|
|`size`|数据大小|
|`data_type`|Link指向的实际数据类型，目前支持 `Binary`, `Image`, `Text`, `Audio` 四种类型|
|`with_local_fs_data`|是否包含本地文件系统中的数据，用于表示user-raw格式的数据|

### 9.1 使用示例

```python
import typing as t
import struct
from pathlib import Path

from starwhale import Link

def iter_item() -> t.Generator[t.Tuple[t.Any, t.Any], None, None]:
    root_dir = Path(__file__).parent.parent / "data"
    data_fpath = root_dir / "t10k-images-idx3-ubyte"
    label_fpath = root_dir / "t10k-labels-idx1-ubyte"

    with data_fpath.open("rb") as data_file, label_fpath.open("rb") as label_file:
        _, data_number, height, width = struct.unpack(">IIII", data_file.read(16))
        _, label_number = struct.unpack(">II", label_file.read(8))

        image_size = height * width
        offset = 16

        for i in range(0, min(data_number, label_number)):
            _data = Link(
                uri=str(data_fpath.absolute()),
                offset=offset,
                size=image_size,
                data_type=GrayscaleImage(
                    display_name=f"{i}", shape=(height, width, 1)
                ),
                with_local_fs_data=True,
            )
            _label = struct.unpack(">B", label_file.read(1))[0]
            yield _data, {"label": _label}
            offset += image_size

```

### 9.2 函数

#### 9.2.1 astype

```python
astype() -> Dict[str, t.Any]
```

## starwhale.S3LinkAuth

当数据存储在基于S3协议的对象存储上时，该类型负责描述授权、密钥信息。Github上的[代码链接](https://github.com/star-whale/starwhale/blob/dc6e6fdeae2f7c5bd0e72ccd8fb50768b1ce0826/client/starwhale/core/dataset/type.py#L52)。

```python
S3LinkAuth(
    name: str = "",
    access_key: str = "",
    secret: str = "",
    endpoint: str = "",
    region: str = "local",
)
```

|参数|说明|
|---|---|
|`name`|Auth的名称|
|`access_key`|S3连接中的access_key|
|`secret`|S3连接中的secret|
|`endpoint`|S3连接中的endpoint地址|
|`region`|bucket所在的S3 region，默认为local|

### 9.3 使用示例

```python
import struct
import typing as t
from pathlib import Path

from starwhale import (
    Link,
    S3LinkAuth,
    GrayscaleImage,
    UserRawBuildExecutor,
)
class LinkRawDatasetProcessExecutor(UserRawBuildExecutor):
    _auth = S3LinkAuth(name="mnist", access_key="minioadmin", secret="minioadmin")
    _endpoint = "10.131.0.1:9000"
    _bucket = "users"

    def iter_item(self) -> t.Generator[t.Tuple[t.Any, t.Any], None, None]:
        root_dir = Path(__file__).parent.parent / "data"

        with (root_dir / "t10k-labels-idx1-ubyte").open("rb") as label_file:
            _, label_number = struct.unpack(">II", label_file.read(8))

            offset = 16
            image_size = 28 * 28

            uri = f"s3://{self._endpoint}/{self._bucket}/dataset/mnist/t10k-images-idx3-ubyte"
            for i in range(label_number):
                _data = Link(
                    f"{uri}",
                    self._auth,
                    offset=offset,
                    size=image_size,
                    data_type=GrayscaleImage(display_name=f"{i}", shape=(28, 28, 1)),
                )
                _label = struct.unpack(">B", label_file.read(1))[0]
                yield _data, {"label": _label}
                offset += image_size
```

## 10. starwhale.LocalFSLinkAuth

描述数据存储在本地文件系统上，Github上的[代码链接](https://github.com/star-whale/starwhale/blob/dc6e6fdeae2f7c5bd0e72ccd8fb50768b1ce0826/client/starwhale/core/dataset/type.py#L151)。

```python
LocalFSLinkAuth = partial(LinkAuth, ltype=LinkType.LocalFS)
```

## 11. starwhale.DefaultS3LinkAuth

使用默认值初始化 `S3LinkAuth` 类型后得到的变量, Github上的[代码链接](https://github.com/star-whale/starwhale/blob/dc6e6fdeae2f7c5bd0e72ccd8fb50768b1ce0826/client/starwhale/core/dataset/type.py#L152)。

```python
DefaultS3LinkAuth = S3LinkAuth()
```

## 12. starwhale.MIMEType

描述Starwhale支持的多媒体类型，用Python Enum类型实现，用在 `Image`、`Video` 等类型的mime_type 属性上，能更好的进行Dataset Viewer。Github上的[代码链接](https://github.com/star-whale/starwhale/blob/dc6e6fdeae2f7c5bd0e72ccd8fb50768b1ce0826/client/starwhale/core/dataset/type.py#L106)。

```python
class MIMEType(Enum):
    PNG = "image/png"
    JPEG = "image/jpeg"
    WEBP = "image/webp"
    SVG = "image/svg+xml"
    GIF = "image/gif"
    APNG = "image/apng"
    AVIF = "image/avif"
    MP4 = "video/mp4"
    AVI = "video/avi"
    WAV = "audio/wav"
    MP3 = "audio/mp3"
    PLAIN = "text/plain"
    CSV = "text/csv"
    HTML = "text/html"
    GRAYSCALE = "x/grayscale"
    UNDEFINED = "x/undefined"
```

## 13. starwhale.LinkType

描述Starwhale支持的remote-link类型，用Python Enum类型实现，目前支持 `LocalFS` 和 `S3` 两种类型。Github上的[代码链接](https://github.com/star-whale/starwhale/blob/dc6e6fdeae2f7c5bd0e72ccd8fb50768b1ce0826/client/starwhale/core/dataset/type.py#L23)。

```python
class LinkType(Enum):
    LocalFS = "local_fs"
    S3 = "s3"
    UNDEFINED = "undefined"
```

## 14. starwhale.BoundingBox3D

提供在二维界面上绘制3D边界框的能力，需要前后两个边界框的`BoundingBox`信息`bbox_a`, `bbox_b`。Github上的[代码链接](https://github.com/star-whale/starwhale/blob/4d240d0c8ec1e7d7c98746ebbd814d2647fb16af/client/starwhale/core/dataset/type.py#L551)。

```python
BoundingBox3D(
    bbox_a: BoundingBox,
    bbox_b: BoundingBox,
)

```

|参数| 说明                |
|---|-------------------|
|`bbox_a`| 在二维UI上，3D框靠近用户的一面 |
|`bbox_b`| 在二维UI上，3D框远离用户的一面        |

## 15. starwhale.NumpyBinary

在构建`Dataset`的时候，用户可以使`NumpyBinary`来存储`ndarray`，以提高starwhale的存储效率。`fp`为`ndarray`在本地的存储路径，或者`bytes`，`dtype`是`numpy`的`dtype`，`shape`为`ndarray`的形状，`link`可以作为`fp`的备选输入。Github上的[代码链接](https://github.com/star-whale/starwhale/blob/02ed82a406ef403416a6faf67f41341e68c38acd/client/starwhale/core/dataset/type.py#L326)。

```python
NumpyBinary(
   self,
   fp: _TArtifactFP,
   dtype: t.Type,
   shape: _TShape,
   link: t.Optional[Link] = None,
)
```
