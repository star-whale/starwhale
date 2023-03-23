---
title: MNIST 数字手写体识别模型评测
---

本例子[参考TorchServing MNIST Example](https://github.com/pytorch/serve/tree/master/examples/image_classifier/mnist)对[MNIST Dataset](http://yann.lecun.com/exdb/mnist/)数字手写体识别和模型评测，相关代码的链接：[example/mnist](https://github.com/star-whale/starwhale/tree/main/example/mnist)。

从该例中，我们能实践如下Starwhale功能：

- 如何使用Image类型构建swds和user-raw格式的数据集。
- 如何制作Link类型的数据集。
- 如何使用 `starwhale.multi_classification` 自动处理多分类问题。

## 前置条件

阅读本文前，建议先阅读[Pytorch Runtime构建](../runtime/examples/pytorch.md)。

### 基础环境

- Python版本: 3.7 ~ 3.10。
- OS环境: Linux或macOS(仅运行Standalone)。
- Starwhale Client 完成安装，且版本不早于0.3.0。
- [可选]Starwhale Controller 完成安装，且版本不早于0.3.0，如果只希望在Standalone Instance中进行评测，可以忽略该步骤。
- Runtime: [Pytorch Runtime Example](https://github.com/star-whale/starwhale/tree/main/example/runtime/pytorch)

### Starwhale Runtime激活

本例可以使用Starwhale提供的[Pytorch Runtime例子](https://github.com/star-whale/starwhale/tree/main/example/runtime/pytorch)作为Starwhale Runtime，不需要额外编写Runtime配置。模型训练和评测都可以使用该Runtime。

- 准备Runtime：下载或重新构建，如何Standalone Instance中已经有该Runtime，则忽略该步骤。

```shell
# 下载Starwhale Cloud已经构建好的Pytorch Runtime
swcli runtime copy https://cloud.starwhale.ai/project/demo/runtime/pytorch/version/latest self

# 或根据pytorch runtime example在本地构建一个新的Runtime
git clone --depth=1 https://github.com/star-whale/starwhale.git
cd starwhale/example/runtime/pytorch
swcli runtime build .
```

- Restore Runtime：本地复原Runtime环境，并在当前shell中激活相应的Python环境

```shell
swcli runtime restore pytorch/version/latest
swcli runtime activate --uri pytorch/version/latest
```

### 数据准备与模型训练

数据准备和模型训练非常容易，只需要两步就能完成操作：下载代码、开始训练。

```shell
git clone --depth=1 https://github.com/star-whale/starwhale.git
cd starwhale/example/mnist
make train
```

- `make train` 命令需要在Pytorch Runtime 已经激活的Shell环境中执行，否则可能提示某些Python包Import Error。
- `make train` 命令会自动下载数据，如果遇到网络问题，请合理设置代理。
- `make train` 过程可能会比较慢，执行时间长短取决于网络速度、机器配置、GPU资源情况等。
- 命令执行结束后，可以到`data`目录查看原始数据，`models`目录查看已经构建好的模型。
- 可以在train.py中对训练过程的一些参数进行调整，比如epoch值等。

## Starwhale的模型评测过程

### 步骤1：构建Starwhale Dataset

```bash
# 根据dataset.yaml构建swds-bin格式in格式的数据集
swcli dataset build .
# 查看最新构建的数据集详情
swcli dataset info mnist/version/latest
```

上面的`build`命令在`starwhale/example/mnist`中执行，也可以在其他目录中执行，但要合理设置 `swcli dataset build`命令的`WORKDIR`参数。

### 步骤2：Standalone Instance中评测模型

```bash
#如果已经激活该runtime环境，则忽略本行命令
swcli runtime activate --uri pytorch/version/latest
# 根据model.yaml运行评测任务
swcli model eval . --dataset mnist/version/latest
# 展示评测结果
swcli model info ${version}
```

上面的`build`命令在`starwhale/example/mnist`中执行，也可以在其他目录中执行，但要合理设置 `swcli model eval`命令的`WORKDIR`参数。如果不想每次执行`eval`命令都指定`--runtime`参数，则可以先执行`swcli runtime activate --uri pytorch/version/latest`命令激活当前shell环境，或在一个已经激活Pytorch Runtime环境shell中执行评测。

![mnist-eval.png](../img/examples/mnist-eval.png)

### 步骤3：构建Starwhale Model

一般情况下，用户经过多次运行模型评测命令(步骤2)进行调试，得到一个可以在大数据量下运行评测或可发布的模型，就需要执行步骤3，构建一个可分发的Starwhale Model。

```bash
#根据model.yaml构建Starwhale Model
swcli model build .
# 查看最新构建的模型信息
swcli model info mnist/version/latest
```

### 步骤4：Cloud Instance中评测模型（可选）

在Cloud Instance上运行评测任务，需要将Standalone Instance上构建的Model、Dataset和Runtime发布到相应的Instance上。

```bash
# 登陆相关instance，之后可以用 prod alias访问该instance
swcli instance login --username ${username} --token ${token}  http://${instance-address} --alias prod
# 将本地默认instance改为standalone
swcli instance select local
#上传model到prod instance中name为starwhale的project中
swcli model copy mnist/version/latest cloud://prod/project/starwhale
#上传dataset到prod instance中name为starwhale的project中
swcli dataset copy mnist/version/latest cloud://prod/project/starwhale
#上传runtime到prod instance中name为starwhale的project中
swcli runtime copy pytorch/version/latest cloud://prod/project/starwhale
```

然后，可以在终端中执行`swcli ui prod`命令，可以拉起浏览器并进入prod instance的web页面中，接着进入相关project，创建评测任务即可。

## 深入理解MNIST例子

此部分从代码和配置层面，详细介绍如何对MNIST进行Starwhale模型评测，如果只是希望简单复现评测，可以跳过此章节。

### 代码结构说明

```bash
|-- .swignore                 # .swignore文件，可以在model/dataset 构建时排除默写目录或文件
|-- Makefile                  # 目前仅包含train命令。
|-- dataset.yaml              # Starwhale Dataset构建的描述性文件，也是Dataset的起点。
|-- model.yaml                # Starwhale Model构建和运行的描述性文件，也是Model的起点。
|-- notebook.ipynb            # notebook文件，可以在jupyter notebook或google colab上使用。
|-- requirements-sw-lock.txt  # Python依赖
|-- config                    # config 目录，可以存放相关配置
|-- data                      # clone代码仓库的初始状态并没有此目录，make train命令自动创建。
   |-- MNIST                  # make train命令执行的时候，会在这个目录中自动下载数据原始文件。
|-- models                    # 模型存储目录
   |-- mnist_cnn.pth          # make train命令执行完成后，会生成mnist_cnn.pth文件，即模型文件。
|-- mnist                     # 源代码目录，包含若干Python代码文件。
   |-- __init__.py            # model.yaml和dataset.yaml中handler字段会通过python module方式描述入口点，故需要mnist目录是一个Python Module，需要写明__init__.py文件。
   |-- dataset.py             # 描述Starwhale Dataset如何构建。
   |-- evaluator.py           # 描述如何使用Starwhale的PipelineHandler快速完成ppl和cmp阶段的定义，进而完成Model评测任务。
   |-- model.py               # 描述模型结构。
   |-- train.py               # 描述训练过程，便于进行例子复现。目前Starwhale没有提供Model Training的相关支持，此文件也不是Starwhale Model Evaluation所必须的。
```

`mnist/dataset.py`、`mnist/evaluator.py`、`dataset.yaml` 和 `model.yaml` 这四个文件是关键代码和配置。

### swds-bin格式的数据集构建

```python
from starwhale import GrayscaleImage, SWDSBinBuildExecutor

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

dataset.yaml中handler指向 `mnist.dataset:DatasetProcessExecutor`，执行 `swcli dataset build` 命令后会构建swds-bin格式的数据集，该格式是Starwhale提供的一种二进制数据集格式。上例中对原始MNIST文件进行读取然后通过yield方式输出data和annotations字段。data使用了 `starwhale.GrayscaleImage` 类型，是专门针对灰度图提供的一种数据类型，Cloud Instance的Web Dataset Viewer能自动展示该类型数据。

![mnist-viewer.gif](../img/examples/mnist-viewer.gif)

### user-raw格式的数据集构建

```python
from starwhale import Link, S3LinkAuth, GrayscaleImage, UserRawBuildExecutor

class RawDatasetProcessExecutor(UserRawBuildExecutor):
    def iter_item(self) -> t.Generator[t.Tuple[t.Any, t.Any], None, None]:
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

当用户构建数据集的类继承 `starwhale.UserRawBuildExecutor` 后，可以制作user-raw格式的数据集。此种格式，不会改变原始数据格式，只是在外部增加索引关系，数据类型用Link来表示，Link中的data_type类型为 `GrayscaleImage`，Cloud Instance的Web Dataset Viewer支持这种格式的可视化。当使用 `swcli dataset extract` 命令解压这种格式的数据集后，能看到原始的数据。

### remote-link格式的数据集构建

```python
from starwhale import Link, S3LinkAuth, GrayscaleImage, UserRawBuildExecutor

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

最后一种数据格式称之为remote-link格式，顾名思义，表示的是一种远程link方式构建的数据集，即数据存在在其他介质上，目前支持存在在Local FS和S3协议的对象存储两种方式。构建出来的Starwhale数据集仅存储数据映射关系，不会存储原始数据，适用于原始数据比较大不放面搬迁的场景。

需要注意的是，由于访问Remote的Minio数据，需要连接密钥信息，管理员需要在server的system setting中配置必要的秘钥信息。

## 参考资料

- [Digit recognition model with MNIST dataset](https://github.com/pytorch/serve/tree/master/examples/image_classifier/mnist)
- [MNIST Dataset](http://yann.lecun.com/exdb/mnist/)
