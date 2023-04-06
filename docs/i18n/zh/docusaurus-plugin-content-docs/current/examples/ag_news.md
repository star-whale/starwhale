---
title: AG News数据集的文本分类模型评测
---

本例子[参考TorchText](https://pytorch.org/tutorials/beginner/text_sentiment_ngrams_tutorial.html)对[AG News数据集](http://www.di.unipi.it/~gulli/AG_corpus_of_news_articles.html)进行文本分类和模型评测，相关代码的链接：[example/text_cls_AG_NEWS](https://github.com/star-whale/starwhale/tree/main/example/text_cls_AG_NEWS)。

从该例中，我们能实践如下Starwhale功能：

- 如何使用Text类型构建swds数据集。
- 如何在model.yaml中指定多个model并在评测中使用。

## 前置条件

阅读本文前，建议先阅读[Pytorch Runtime构建](../runtime/examples/pytorch.md), [Speech Commands数据集的多分类任务模型评测](speech.md)。

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

- Activate Runtime：在当前shell中激活相应的Python环境

```shell
swcli runtime activate pytorch/version/latest
```

### 数据准备与模型训练

数据准备和模型训练非常容易，只需要三步就能完成操作：下载代码、下载数据、开始训练。

```shell
git clone --depth=1 https://github.com/star-whale/starwhale.git
cd starwhale/example/text_cls_AG_NEWS
make download-data
make train
```

- `make download-data` 命令下载数据的时候，如果遇到网络问题，请合理设置代理。
- `make train` 命令需要在Pytorch Runtime 已经激活的Shell环境中执行，否则可能提示某些Python包Import Error。
- `make train` 过程可能会比较慢，执行时间长短取决于机器配置、GPU资源情况等。
- 命令执行结束后，可以到`data`目录查看原始数据，`models`目录查看已经构建好的模型。
- 可以在train.py或Makefile中对训练过程的一些参数进行调整，比如epoch值, 是否使用gpu等。

## Starwhale的模型评测过程

### 步骤1：构建Starwhale Dataset

```bash
# 根据dataset.yaml构建swds-bin格式in格式的数据集
swcli dataset build .
# 查看最新构建的数据集详情
swcli dataset info ag_news_test/version/latest
```

上面的`build`命令在`starwhale/example/text_cls_AG_NEWS`中执行，也可以在其他目录中执行，但要合理设置 `swcli dataset build`命令的`WORKDIR`参数。

```bash
❯ swcli dataset build .
🚧 start to build dataset bundle...
👷 uri:local/project/self/dataset/ag_news_test
🆕 version gi4gcmtbgm2d
📁 swds workdir: /home/liutianwei/.cache/starwhale/.tmp/tmpwvso4qob
👍 try to copy source code files...
cleanup done.
finish gen swds @ /home/liutianwei/.cache/starwhale/.tmp/tmpwvso4qob/data
🤖 calculate signature with blake2b for 4 files
🌺 congratulation! you can run  swcli dataset info ag_news_test/version/gi4gcmtbgm2dmy3bmnsggzbqmy2dsma
  9 out of 9 steps finished ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━ 100% 0:00:00 0:00:01
finish gen resource @ /home/liutianwei/.cache/starwhale/self/dataset/ag_news_test/gi/gi4gcmtbgm2dmy3bmnsggzbqmy2dsma.swds
```

### 步骤2：Standalone Instance中评测模型

```bash
# 根据model.yaml运行评测任务
swcli model eval . --dataset  ag_news_test/version/latest --runtime pytorch/version/latest
# 展示评测结果
swcli model info ${version}
```

上面的`build`命令在`starwhale/example/text_cls_AG_NEWS`中执行，也可以在其他目录中执行，但要合理设置 `swcli model eval`命令的`WORKDIR`参数。如果不想每次执行`eval`命令都指定`--runtime`参数，则可以先执行`swcli runtime activate pytorch/version/latest`命令激活当前shell环境，或在一个已经激活Pytorch Runtime环境shell中执行评测。

![eval.png](../img/examples/ag_news-eval.png)

### 步骤3：构建Starwhale Model

一般情况下，用户经过多次运行模型评测命令(步骤2)进行调试，得到一个可以在大数据量下运行评测或可发布的模型，就需要执行步骤3，构建一个可分发的Starwhale Model。

```shell
#如果已经激活该runtime环境，则忽略本行命令
swcli runtime activate pytorch/version/latest
#根据model.yaml构建Starwhale Model
swcli model build .
# 查看最新构建的模型信息
swcli model info text_cls/version/latest
```

上面的`build`命令在`starwhale/example/text_cls_AG_NEWS`中执行，也可以在其他目录中执行，但要合理设置 `swcli model build`命令的`WORKDIR`参数。如果执行`swcli model build`命令时，是在非Pytorch Runtime的Shell环境中进行的，则可以追加 `--runtime pytorch/version/latest` 参数，确保构建时使用Pytorch Runtime环境。

### 步骤4：Cloud Instance中评测模型（可选）

在Cloud Instance上运行评测任务，需要将Standalone Instance上构建的Model、Dataset和Runtime发布到相应的Instance上。

```shell
# 登陆相关instance，之后可以用 prod alias访问该instance
swcli instance login --username ${username} --token ${token}  http://${instance-address} --alias prod
# 将本地默认instance改为standalone
swcli instance select local
#上传model到prod instance中name为starwhale的project中
swcli model copy text_cls/version/latest cloud://prod/project/starwhale
#上传dataset到prod instance中name为starwhale的project中
swcli dataset copy ag_news_test/version/latest cloud://prod/project/starwhale
#上传runtime到prod instance中name为starwhale的project中
swcli runtime copy pytorch/version/latest cloud://prod/project/starwhale
```

然后，可以在终端中执行`swcli ui prod`命令，可以拉起浏览器并进入prod instance的web页面中，接着进入相关project，创建评测任务即可。

![ui.gif](../img/examples/ag_news-ui.gif)

## 参考资料

- [Text Classification with the TorchText Library](https://pytorch.org/tutorials/beginner/text_sentiment_ngrams_tutorial.html)
- [AG News数据集](http://www.di.unipi.it/~gulli/AG_corpus_of_news_articles.html)
