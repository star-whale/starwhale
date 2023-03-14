---
title: CIFAR10数据集的简单图形识别模型评测
---

本例子[参考Pytorch 训练分类器](https://pytorch.org/tutorials/beginner/blitz/cifar10_tutorial.html)对[cifar10数据集](https://www.cs.toronto.edu/~kriz/cifar.html)进行图片分类和模型评测，相关代码的链接：[example/cifar10](https://github.com/star-whale/starwhale/tree/main/example/cifar10)。

从该例中，我们能实践如下Starwhale功能：

- 如何使用Image类型构建swds数据集。
- 如果使用 `starwhale.multi_classification` 修饰器来简化多分类问题cmp部分的编写。

## 1.前置条件

阅读本文前，建议先阅读[Pytorch Runtime构建](../runtime/examples/pytorch.md), [Speech Commands数据集的多分类任务模型评测](speech.md)。

### 1.1 基础环境

- Python版本: 3.7 ~ 3.10。
- OS环境: Linux或macOS(仅运行Standalone)。
- Starwhale Client 完成安装，且版本不早于0.3.0。
- [可选]Starwhale Controller 完成安装，且版本不早于0.3.0，如果只希望在Standalone Instance中进行评测，可以忽略该步骤。
- Runtime: [Pytorch Runtime Example](https://github.com/star-whale/starwhale/tree/main/example/runtime/pytorch)

### 1.2 Starwhale Runtime激活

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

### 1.3 数据准备与模型训练

数据准备和模型训练非常容易，只需要两步就能完成操作：下载代码、开始训练。

```shell
git clone --depth=1 https://github.com/star-whale/starwhale.git
cd starwhale/example/cifar10
make train
```

- `make train` 命令需要在Pytorch Runtime 已经激活的Shell环境中执行，否则可能提示某些Python包Import Error。
- `make train` 命令会自动下载数据，如果遇到网络问题，请合理设置代理。
- `make train` 过程可能会比较慢，执行时间长短取决于网络速度、机器配置、GPU资源情况等。
- 命令执行结束后，可以到`data`目录查看原始数据，`models`目录查看已经构建好的模型。
- 可以在train.py中对训练过程的一些参数进行调整，比如epoch值等。

## 2.Starwhale的模型评测过程

### 步骤1：构建Starwhale Dataset

```bash
#如果已经激活该runtime环境，则忽略本行命令
swcli runtime activate --uri pytorch/version/latest
# 根据dataset.yaml构建swds-bin格式in格式in格式in格式in格式的数据集
swcli dataset build .
# 查看最新构建的数据集详情
swcli dataset info cifar10-test/version/latest
```

上面的`build`命令在`starwhale/example/cifar10`中执行，也可以在其他目录中执行，但要合理设置 `swcli dataset build`命令的`WORKDIR`参数。如果不想每次开启新的shell时执行`build`命令都先激活runtime环境，可以执行 `build` 命令的时候指定 `--runtime` 参数。构建数据集的时候，本例提供了将原始数据集Python格式转变为 `starwhale.Image` 的方式，核心代码如下：

```python
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

当分发数据集到Cloud Instance后，可以Web界面中使用Dataset Viewer观察数据集：

![dataset-viewer.gif](../img/examples/cifar10-dataset.gif)

### 步骤2：Standalone Instance中评测模型

```bash
#如果已经激活该runtime环境，则忽略本行命令
swcli runtime activate --uri pytorch/version/latest
# 根据model.yaml运行评测任务
swcli model eval . --dataset  cifar10-test/version/latest
# 展示评测结果
swcli model info ${version}
```

上面的`build`命令在`starwhale/example/cifar10`中执行，也可以在其他目录中执行，但要合理设置 `swcli model eval`命令的`WORKDIR`参数。如果不想每次执行`eval`命令都指定`--runtime`参数，则可以先执行`swcli runtime activate --uri pytorch/version/latest`命令激活当前shell环境，或在一个已经激活Pytorch Runtime环境shell中执行评测。

### 步骤3：构建Starwhale Model

一般情况下，用户经过多次运行模型评测命令(步骤2)进行调试，得到一个可以在大数据量下运行评测或可发布的模型，就需要执行步骤3，构建一个可分发的Starwhale Model。

```shell
#根据model.yaml构建Starwhale Model
swcli model build . --uri pytorch/version/latest
# 查看最新构建的模型信息
swcli model info cifar_net/version/latest
```

### 步骤4：Cloud Instance中评测模型（可选）

在Cloud Instance上运行评测任务，需要将Standalone Instance上构建的Model、Dataset和Runtime发布到相应的Instance上。

```shell
# 登陆相关instance，之后可以用 prod alias访问该instance
swcli instance login --username ${username} --token ${token}  http://${instance-address} --alias prod
# 将本地默认instance改为standalone
swcli instance select local
#上传model到prod instance中name为starwhale的project中
swcli model copy cifar_net/version/latest cloud://prod/project/starwhale
#上传dataset到prod instance中name为starwhale的project中
swcli dataset copy cifar10-test/version/latest cloud://prod/project/starwhale
#上传runtime到prod instance中name为starwhale的project中
swcli runtime copy pytorch/version/latest cloud://prod/project/starwhale
```

然后，可以在终端中执行`swcli ui prod`命令，可以拉起浏览器并进入prod instance的web页面中，接着进入相关project，创建评测任务即可。

## 3.参考资料

- [Training a classifier](https://pytorch.org/tutorials/beginner/blitz/cifar10_tutorial.html)
- [cifar10 数据集](https://www.cs.toronto.edu/~kriz/cifar.html)
