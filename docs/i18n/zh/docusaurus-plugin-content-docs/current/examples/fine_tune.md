---
title: 基于resnet的微调全流程
---
本例子[参考D2L-CV Fine Tuning](https://d2l.ai/chapter_computer-vision/fine-tuning.html)对[hotdog](http://d2l-data.s3-accelerate.amazonaws.com/)进行微调及模型评测，相关代码的链接：[example/imageNet-ft](https://github.com/star-whale/starwhale/tree/main/example/imageNet-ft)。

从该示例中，我们将基于starwhale完成如下过程：

- 使用Image类型构建微调及测试数据集
- 编写微调过程，并构建新的模型包
- 编写评测过程

## 前置条件

阅读本文前，建议先阅读[Pytorch Runtime构建](../runtime/examples/pytorch.md), [Speech Commands数据集的多分类任务模型评测](speech.md)。

### 基础环境

- Python版本: 3.7 ~ 3.10。
- OS环境: Linux或macOS(仅运行Standalone)。
- Starwhale Client 完成安装，且版本不早于0.3.0。
- [可选]Starwhale Controller 完成安装，且版本不早于0.3.0，如果只希望在Standalone Instance中进行评测，可以忽略该步骤。
- Runtime: [Pytorch Runtime Example](https://github.com/star-whale/starwhale/tree/main/example/runtime/pytorch)

### Starwhale Runtime构建与激活

- 准备Runtime：下载或重新构建，如何Standalone Instance中已经有该Runtime，则忽略该步骤。

```shell
# 根据本示例提供的runtime配置在本地构建一个新的Runtime
git clone --depth=1 https://github.com/star-whale/starwhale.git
cd starwhale/example/imageNet-ft
swcli runtime build .
```

- Restore Runtime：本地复原Runtime环境，并在当前shell中激活相应的Python环境

```shell
swcli runtime restore image-net-for-finetune/version/latest
swcli runtime activate --uri image-net-for-finetune/version/latest
```

### 数据准备与预训练模型下载

数据准备和模型训练非常容易，只需要两步就能完成操作：下载数据、下载模型。

```shell
make download-data
make download-model
```

- `make download-data` 命令下载数据的时候，如果遇到网络问题，请合理设置代理。
- `make download-model` 命令执行结束后，可以到`data`目录查看原始数据，`models`目录查看已经构建好的模型。

## Starwhale的模型微调过程

### 步骤1：构建Starwhale Dataset

使用starwhale sdk编写数据集构建脚本：

```python
def build_ds(ds_uri: str):
    """
    build by sdk
    :param ds_uri: cloud://server/project/starwhale/dataset/hotdog_test
    """
    ds = dataset(ds_uri, create="empty")
    for idx, label in enumerate(_LABEL_NAMES):
        path = ROOT_DIR / "data" / tag / label
        for _fn in os.listdir(path):
            _f = path / _fn
            with open(_f, mode="rb") as image_file:
                ds.append({
                    "img": Image(fp=image_file.read(), display_name=_fn, mime_type=MIMEType.PNG),
                    "label": label,
                })
    ds.commit()
    ds.close()
```

```bash
# 根据sdk构建所需数据集
python3 imagenet/dataset_build.py train
python3 imagenet/dataset_build.py test
# 查看最新构建的数据集详情
swcli dataset info hotdog_train/version/latest
swcli dataset info hotdog_test/version/latest
```

### 步骤2：Standalone Instance中评测模型

#### 编写微调过程

使用starwhale提供的@pass_context（传递必要的运行信息，如dataset uri等）和@fine-tune（标明为一个可执行器）进行标记。接下来展示微调脚本的流程，详细代码请参看[example/imageNet-ft](https://github.com/star-whale/starwhale/tree/main/example/imageNet-ft/imagenet/pipeline.py)

```python
@pass_context
@experiment.fine_tune()
def fine_tune(context: Context,
              learning_rate=5e-5,
              batch_size=128,
              num_epochs=10,
              param_group=True,
              ):
    # init
    finetune_net = ResNet(BasicBlock, [2, 2, 2, 2])
    # load from pretrained model
    finetune_net.load_state_dict(
        torch.load(str(ROOTDIR / "models" / "resnet18.pth"))  # type: ignore
    )
    # reset to 2 class
    finetune_net.fc = nn.Linear(finetune_net.fc.in_features, 2)
    nn.init.xavier_uniform_(finetune_net.fc.weight)

    ...

    # use starwhale dataset
    train_dataset = dataset(context.dataset_uris[0], readonly=True, create="forbid")
    train_iter = data.DataLoader(train_dataset.to_pytorch(transform=train_augs), batch_size=batch_size)

    ...
    # fine tuning
    for epoch in range(num_epochs):
        for i, (features, labels) in enumerate(train_iter):
            l, acc = train_batch(
                net, features, labels, loss, trainer, devices)
            ...

    # save and build new model
    torch.save(finetune_net.state_dict(), ROOTDIR / "models" / "resnet-ft.pth")

    model.build(workdir=ROOTDIR, name="imageNet-for-hotdog", evaluation_handler=ImageNetEvaluation)
```

#### 基于新模型对数据进行评测

```bash
# 查看新模型是否创建成功
swcli model info imageNet-for-hotdog/version/latest
# 使用新模型对数据进行评测
swcli eval run eval --model imageNet-for-hotdog/version/latest --dataset hotdog_test/version/latest
# 查看评测结果
swcli eval info {eval version}
```

### 步骤3：Cloud Instance中评测模型

在fine tune功能界面中创建相关任务即可.
