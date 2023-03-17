---
title: Speech Commands 数据集的多分类任务模型评测
---

本例子[参考TorchAudio](https://pytorch.org/tutorials/intermediate/speech_command_classification_with_torchaudio_tutorial.html)对[Speech Commands数据集](https://arxiv.org/abs/1804.03209)进行分类识别和模型评测，相关代码的链接：[example/speech_command](https://github.com/star-whale/starwhale/tree/main/example/speech_command)。

从该例中，我们能实践如下Starwhale功能：

- 如何构建swds-bin格式in格式的Starwhale Dataset。
- 如何构建存储在第三方Minio存储上的remote-link格式的Starwhale Dataset。
- 如何使用TorchAudio完成音频数据的多分类任务。
- 如何使用已经构建好的Starwhale Runtime作为Python运行环境。

## 1.前置条件

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
cd starwhale/example/speech_command
make train
```

- `make train` 命令需要在Pytorch Runtime 已经激活的Shell环境中执行，否则可能提示某些Python包Import Error。
- `make train` 命令会自动下载数据，如果遇到网络问题，请合理设置代理。
- `make train` 过程可能会比较慢，执行时间长短取决于网络速度、机器配置、GPU资源情况等。
- 命令执行结束后，可以到`data`目录查看原始数据，`models`目录查看已经构建好的模型。
- 可以在train.py中对训练过程的一些参数进行调整，比如epoch值等。

![train.png](../img/examples/sc_train.png)

## 2.Starwhale的模型评测过程

### 步骤1：构建Starwhale Dataset

```bash
# 根据dataset.yaml构建swds-bin格式的数据集
swcli dataset build . --runtime pytorch/version/latest
# 查看最新构建的数据集详情
swcli dataset info speech_commands_validation/version/latest
```

- 上面的`build`命令在`starwhale/example/speech_command`中执行，也可以在其他目录中执行，但要合理设置 `swcli dataset build`命令的`WORKDIR`参数。
- 示例中dataset.yaml中`handler`是swds-bin格式数据集的构建，如果想构建remote-link的数据集，可以修改`handler:LinkRawDatasetBuildExecutor`。目前可以简单的在dataset.yaml中调整注释，后续Starwhale会支持多handler数据集的同时构建。
- 如果执行`swcli dataset build`命令时，是在已经激活Pytorch Runtime的Shell环境中进行的，则可以忽略 `--runtime pytorch/version/latest` 参数。

![dataset-build.png](../img/examples/sc_dataset_build.png)
![dataset-info.png](../img/examples/sc_dataset_info.png)

### 步骤2：Standalone Instance中评测模型

```bash
#如果已经激活该runtime环境，则忽略本行命令
swcli runtime activate --uri pytorch/version/latest
# 根据model.yaml运行评测任务
swcli model eval . --dataset  speech_commands_validation/version/latest --runtime pytorch/version/latest
# 展示评测结果
swcli model info ${version}
```

上面的`build`命令在`starwhale/example/speech_command`中执行，也可以在其他目录中执行，但要合理设置 `swcli model eval`命令的`WORKDIR`参数。如果不想每次执行`eval`命令都指定`--runtime`参数，则可以先执行`swcli runtime activate --uri pytorch/version/latest`命令激活当前shell环境，或在一个已经激活Pytorch Runtime环境shell中执行评测。

![eval.png](../img/examples/sc_eval.png)

### 步骤3：构建Starwhale Model

一般情况下，用户经过多次运行模型评测命令(步骤2)进行调试，得到一个可以在大数据量下运行评测或可发布的模型，就需要执行步骤3，构建一个可分发的Starwhale Model。

```bash
#如果已经激活该runtime环境，则忽略本行命令
swcli runtime activate --uri pytorch/version/latest
#根据model.yaml构建Starwhale Model
swcli model build .
# 查看最新构建的模型信息
swcli model info speech_commands_m5/version/latest
```

- 上面的`build`命令在`starwhale/example/speech_command`中执行，也可以在其他目录中执行，但要合理设置 `swcli model build`命令的`WORKDIR`参数。
- 如果执行`swcli model build`命令时，是在非Pytorch Runtime的Shell环境中进行的，则可以追加 `--runtime pytorch/version/latest` 参数，确保构建时使用Pytorch Runtime环境。

![model-build.png](../img/examples/sc_model.png)

### 步骤4：Cloud Instance中评测模型（可选）

在Cloud Instance上运行评测任务，需要将Standalone Instance上构建的Model、Dataset和Runtime发布到相应的Instance上。

```shell
# 登陆相关instance，之后可以用 prod alias访问该instance
swcli instance login --username ${username} --token ${token}  http://${instance-address} --alias prod
# 将本地默认instance改为standalone
swcli instance select local
#上传model到prod instance中name为starwhale的project中
swcli model copy speech_commands_m5/version/latest cloud://prod/project/starwhale
#上传dataset到prod instance中name为starwhale的project中
swcli dataset copy speech_commands_validation/version/latest cloud://prod/project/starwhale
#上传runtime到prod instance中name为starwhale的project中
swcli runtime copy pytorch/version/latest cloud://prod/project/starwhale
```

然后，可以在终端中执行`swcli ui prod`命令，可以拉起浏览器并进入prod instance的web页面中，接着进入相关project，创建评测任务即可。

## 3.深入理解Speech Commands例子

此部分从代码和配置层面详细介绍Speech Commands Dataset是如何进行Starwhale模型评测的，如果只是希望简单复现评测，可以跳过此章节。

### 3.1 代码结构说明

```console
|-- .gitignore              # 目前会ignore models和data目录。
|-- Makefile                # 目前仅包含train命令。
|-- dataset.yaml            # Starwhale Dataset构建的描述性文件，也是Dataset的起点。
|-- model.yaml              # Starwhale Model构建和运行的描述性文件，也是Model的起点。
|-- data                    # clone代码仓库的初始状态并没有此目录，make train命令自动创建。
   |-- SpeechCommands       # make train命令执行的时候，会在这个目录中自动下载数据原始文件，大概3.2G的wav音频文件。
|-- models                  # clone代码仓库的初始状态并没有此目录，make train命令自动创建。
   |-- m5.pth               # make train命令执行完成后，会生成m5.pth文件，即模型文件。
|-- sc                      # 源代码目录，包含若干Python代码文件。
   |-- __init__.py          # model.yaml和dataset.yaml中handler字段会通过python module方式描述入口点，故需要sc目录是一个Python Module，需要写明__init__.py文件。
   |-- dataset.py           # 描述Starwhale Dataset如何构建。
   |-- evaluator.py         # 描述如何使用Starwhale的PipelineHandler快速完成ppl和cmp阶段的定义，进而完成Model评测任务。
   |-- model.py             # 描述模型结构。
   |-- train.py             # 描述训练过程，便于进行例子复现。目前Starwhale没有提供Model Training的相关支持，此文件也不是Starwhale Model Evaluation所必须的。
```

`sc/dataset.py`、`sc/evaluator.py`、`dataset.yaml` 和 `model.yaml` 这四个文件是关键代码和配置。

### 3.2 Dataset的构建

在Starwhale体系中，要进行模型评测，第一步就需要准备评测的数据集。Starwhale提供一种快速构建、使用简单、可分发、有版本控制的数据集机制，用户只需要定义`dataset.yaml`和编写少量代码，就能完成数据集的构建。目前Starwhale Dataset构建三要素：`dataset.yaml` 、`dataset.py` 和 `swcli dataset build` 命令。

- dataset.yaml 定义

    ```yaml
    name: speech_commands_validation                      #数据集名称
    handler: sc.dataset:SWDSBuildExecutor                 #构建数据集时调用的python代码入口点

    desc: SpeechCommands data and label test dataset      #[可选]描述数据集用途
    attr:                                                 #[可选]描述数据集的一些构建参数
    alignment_size: 4k                                    #[可选]默认4k
    volume_size: 64M                                      #[可选]默认64MB
    ```

  - `handler`结构为 ${python module path}:${handler class}。入口点目前只能为Python Class，且需要继承自`starwhale.BuildExecutor`或`starwhale.UserRawBuildExecutor`，并实现`iter_item`方法。
  - `attr.alignment_size`：当构建swds和user raw格式的数据集时，单个数据样本的的padding alignment size，默认为4k。可以认为是为了Page Size等做的padding优化。
  - `attr.volume_size`：当构建swds和user raw格式的数据集时，单个数据文件的最大尺寸，超过该尺寸就会被分割，能够避免单个数据文件过大。
  - 目前dataset.yaml对于数据集构建是必须的。最简单的dataset.yaml只需要包含name和handler两个字段，后续Starwhale也会考虑进一步优化dataset.yaml表示，甚至在某些场景下省略dataset.yaml。

- swds-bin格式的数据集构建代码

    ```python
    import typing as t
    from pathlib import Path
    from starwhale import Audio, MIMEType, BuildExecutor

    dataset_dir = (
        Path(__file__).parent.parent / "data" / "SpeechCommands" / "speech_commands_v0.02"
    )
    validation_ds_paths = [dataset_dir / "validation_list.txt"]

    class SWDSBuildExecutor(BuildExecutor):      #继承BuildExecutor类，构建swds-bin格式的数据集。
        def iter_item(self) -> t.Generator[t.Tuple[t.Any, t.Any], None, None]:  # 实现iter_item方法，返回一个可迭代对象。
            for path in validation_ds_paths:
                with path.open() as f:
                    for item in f.readlines():
                        item = item.strip()
                        if not item:
                            continue

                        data_path = dataset_dir / item
                        data = Audio(    # 构建一个Audio类型的数据
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

  - 继承`BuildExecutor`类可以构建swds-bin格式的Dataset。swds-bin格式是Starwhale Dataset提供的一种二进制格式，会将原始数据变换后生成，包含元数据、类型信息等，能够支持分片、索引和高效加载。
  - 构建数据需要实现`iter_item`方法，该方法返回可迭代的数据，包括data和annotations。
    - data是原始数据，可以使用Starwhale预置的类型来表示，这样便于做Dataset Viewer等。本例子中定义data为Audio类型，当dataset copy到cloud instance后，就能在web ui中可视化整个dataset，包括声音片段的播放等。 ![dataset_viewer.png](../img/examples/sc_dataset_viewer.png)
    - annotations是若干label的描述，用字典表示。

- remote-link格式的数据集构建代码

    ```python
    import typing as t
    from pathlib import Path
    from starwhale import Audio, MIMEType, Link, S3LinkAuth, UserRawBuildExecutor

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

  - 当原始数据文件已经存在第三方存储上，可以使用Starwhale remote-link格式的dataset，能够避免重复拷贝数据，尤其适合数量量比较大或已经有某种存储格式的数据集的场景。remote-link的dataset构建类需要继承 `UserRawBuildExecutor` 类。
  - 本例子中使用boto3库从Minio上遍历相关Prefix路径，迭代返回`Link`类型的数据。需要注意的时，由于访问Remote的Minio数据，需要携带连接密钥信息，Starwhale为保证安全性，需要连接密钥信息，管理员需要在server的system setting中配置必要的秘钥信息。

### 3.3 模型评测代码

Starwhale的模型评测一般分为ppl和cmp两个阶段，用户也可以自定义评测阶段。
    - ppl 阶段：Starwhale Dataset的数据一般会经过前处理、模型推理和后处理，得到可以做统一合并比较的数据。
    - cmp 阶段：收集ppl阶段产生的数据，一般会与label进行对比，得到模型的评测结果。

- model.yaml 定义

    ```yaml
    version: 1.0                          #可选，默认为1.0，表示yaml格式的版本号
    name: speech_commands_m5              #模型包的名称
    run:
      handler: sc.evaluator:M5Inference   #模型评测的Handler

    desc: m5 by pytorch                   #可选，模型评测描述
    ```

  - `run.handler` 结构为 ${python module path}:${handler class}。入口点目前只能为Python Class。如果继承`starwhale.PipelineHandler`类，并实现ppl和cmp两个方法，可以非常容易的完成模型评测过程。用户也可以不继承`starwhale.PipelineHandler`类，而是使用 `starwhale.Step` 等工具函数实现完全自定义的模型评测过程。

- ppl过程

    ```python
    import torch
    from starwhale import Audio, PipelineHandler

    class M5Inference(PipelineHandler):

        @torch.no_grad()
        def ppl(self, audio: Audio, **kw):
            _tensor = self.pre(audio)
            output = self.model(_tensor)
            return self._post(output)
    ```

  - 数据集中的每条数据都会调用一次ppl，如果handler中写的class继承自`PipelineHandler`，那么一定要实现ppl方法。典型的ppl处理过程是前处理->模型推理->后处理，但不用拘泥于这种形式，用户可以自行编写程序。
  - ppl函数的输入参数分为两部分：数据和其他参数。数据部分是制作数据集中 `iter_item` 中 `yield` 的第一个元素，本例子中是一个Starwhale的Audio类型。其他参数部分kw，是一个字典，目前会包含两个参数：`annotations` 和 `index` 。`annotations`是`iter_item` 中 `yield` 的第二个元素，包含若干个数据集中用户自定义的annotation，用字典表示。`index` 类型为float，表示该条数据在整个数据集中的索引编号。
  - ppl函数需要有返回值，但没有类型和形式限制，只要能pickle即可，在cmp中也会自动反序列化成原始的类型。

- cmp过程

    ```python
    from starwhale import multi_classification, PipelineHandler

    class M5Inference(PipelineHandler):

        @multi_classification(
        confusion_matrix_normalize="all",
        show_hamming_loss=True,
        show_cohen_kappa_score=True,
        show_roc_auc=True,
        all_labels=[i for i in range(0, len(ALL_LABELS))],
        )
        def cmp(self, ppl_result):
            result, label, pr = [], [], []
            for _data in ppl_result:
                label.append(ALL_LABELS_MAP[_data["annotations"]["label"]])
                pr.append(_data["result"][1])
                result.append(_data["result"][0][0])
            return label, result, pr
    ```

  - cmp过程一般是整个评测的最后一步，负责将ppl推理结果汇总，并对label进行对比，然后得到各种形态的评测报告。继承`PipelineHandler`的类，需要实现cmp方法。
  - cmp函数的输入参数为`ppl_result`，可以被迭代使用。每个迭代出来的元素是一个dict类型，目前包含 `annotations` , `result` 和 `data_id` 三个元素。`result` 为某条dataset数据的ppl推理结果。
  - 本例是一个multi classification问题，可以直接用 `starwhale.multi_classification` 修饰器，能自动对cmp结果进行进一步分析，并将结果存储在Starwhale的DataStore中，方便后续的可视化展示。由于设置 `show_roc_auc=True` 参数，cmp函数需要返回三个元素：label列表，result列表和probability_matrix列表。需要注意的是，即使是multi classification问题，也不需要强制用 `starwhale.multi_classification` 修饰器，用户完全可以按照自己的需求定制化cmp过程。

## 4.参考资料

- [Speech command classification with TorchAudio](https://pytorch.org/tutorials/intermediate/speech_command_classification_with_torchaudio_tutorial.html)
- [Speech Commands数据集](https://arxiv.org/abs/1804.03209)
