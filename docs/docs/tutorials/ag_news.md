---
title: Text Classification on AG News dataset
---

This example illustrates how to evaluate a pre-trained text classification model on Starwhale in 6 steps.

1. Create a Runtime
2. Train the model
3. Implement the dataset slicing method
4. Implement the inference method and evaluation metrics computing method
5. Build Runtime, Dataset, and Model
6. Run the evaluation job and see the metrics

## Prerequisites

* Assume that you have Python3.7 or above installed.
* Clone the Starwhale repo

    ```bash
    git clone https://github.com/star-whale/starwhale.git
    cd starwhale/example/text_cls_AG_NEWS
    ```

> :bulb: If you are from the mainland of China, we strongly recommend you use a proxy.

* Install [Starwhale](../quickstart/standalone.md)

## Create a Runtime

```bash
$ swcli runtime create . --name pytorch_text -m venv --python=3.9 --force --base-image ghcr.io/star-whale/starwhale:0.2.0-alpha.12
🚧 start to create the runtime environment...
🍰 run the following command in your shell 🍰
        source ~/code/starwhale/example/text_cls_AG_NEWS/venv/bin/activate
👏 python runtime environment is ready to use 🎉
$ source ~/code/starwhale/example/text_cls_AG_NEWS/venv/bin/activate
(pytorch_text) $  python3 -m pip install -r requirements.txt
```

> :bulb: make sure python3.9-venv is installed if you choose --python=3.9
> :bulb: `python3 -m pip install` is recommended over `pip install`

## Train the model

> The training code in this repo is copied from [Pytorch Tutorial](https://pytorch.org/tutorials/beginner/text_sentiment_ngrams_tutorial.html). However, the dataset part is modified to understand better how Starwhale works.

```console
(pytorch_text) $ mkdir models
(pytorch_text) $ cd code
(pytorch_text) $ python train.py --device cpu --save-model-path  ../models/model.i --dictionary ../models/vocab.i --num-epochs 5
```

You will get the logs as below:

```console
| epoch   1 |   500/ 7125 batches | accuracy    0.385
| epoch   1 |  1000/ 7125 batches | accuracy    0.660
| epoch   1 |  1500/ 7125 batches | accuracy    0.771
| epoch   1 |  2000/ 7125 batches | accuracy    0.813
| epoch   1 |  2500/ 7125 batches | accuracy    0.842
| epoch   1 |  3000/ 7125 batches | accuracy    0.860
| epoch   1 |  3500/ 7125 batches | accuracy    0.852
| epoch   1 |  4000/ 7125 batches | accuracy    0.869
| epoch   1 |  4500/ 7125 batches | accuracy    0.875
| epoch   1 |  5000/ 7125 batches | accuracy    0.876
| epoch   1 |  5500/ 7125 batches | accuracy    0.884
| epoch   1 |  6000/ 7125 batches | accuracy    0.881
| epoch   1 |  6500/ 7125 batches | accuracy    0.883
| epoch   1 |  7000/ 7125 batches | accuracy    0.894
-----------------------------------------------------------
| end of epoch   1 | time: 28.79s | valid accuracy    0.896
-----------------------------------------------------------
| epoch   2 |   500/ 7125 batches | accuracy    0.922
| epoch   2 |  1000/ 7125 batches | accuracy    0.915
......
......
| epoch   5 |  6000/ 7125 batches | accuracy    0.918
| epoch   5 |  6500/ 7125 batches | accuracy    0.925
| epoch   5 |  7000/ 7125 batches | accuracy    0.922
-----------------------------------------------------------
| end of epoch   5 | time: 27.34s | valid accuracy    0.900
-----------------------------------------------------------
Checking the results of test dataset.
test accuracy    0.877
Saving model to ../models/model.i
Save vocab to ../models/vocab.i
```

Great! Now you have your model trained and saved. You can see it in the `models` directory.

```console
(pytorch_text) $ ls ../models
model.i  vocab.i
```

## Slice the test dataset using the Starwhale protocol

In the training section, we use a dataset called [AG_NEWS](https://paperswithcode.com/dataset/ag-news).

```console
(pytorch_text) $ ls ../data
test.csv  train.csv
```

The test part of the dataset is a file called `test.csv`, which contains 7,600 lines of texts and labels.

Before version `0.2.x`, Starwhale sliced the dataset into chunks where the batched texts and labels reside. You must tell Starwhale how to yield batches of byte arrays from each dataset file.

In this example, we will read texts and labels in batch and convert them into byte arrays.

```python
def yield_data(path, label=False):
    data = ag_news.load_ag_data(path)
    idx = 0
    data_size = len(data)
    while True:
        last_idx = idx
        idx += 1
        if idx > data_size:
            break
        data_batch = [lbl if label else txt for lbl, txt in
                      data[last_idx:idx]]
        join = "#@#@#@#".join(data_batch)
        yield join.encode()

class AGNEWSSlicer(BuildExecutor):

    def iter_data_slice(self, path: str):
        yield from yield_data(path)

    def iter_label_slice(self, path: str):
        yield from yield_data(path, True)


```

You need to extend the abstract class `BuildExecutor`, so Starwhale can use it. The `path` argument is a file that matches `data_filter` or `label_filter` in `${code_base}/example/text_cls_AG_NEWS/dataset.yaml`. The filters used in this example are `test.csv`.

## Implement the inference method and evaluation metrics computing method

The inference method is called `ppl,` and the evaluation metrics computing method is called `cmp`.
Here is the code snap from `ppl.py`, which implements both methods. You need to extend the abstract class `PipelineHandler` so you can receive the byte arrays, which you transformed in the last step.

```python
class TextClassificationHandler(PipelineHandler):

    def __init__(self, device="cpu") -> None:
        super().__init__(merge_label=True, ignore_error=True)
        self.device = torch.device(device)

    @torch.no_grad()
    def ppl(self, data, **kw):
        _model, vocab, tokenizer = self._load_model(self.device)
        texts = data.decode().split('#@#@#@#')
        return list(map(lambda text: predict.predict(text, _model, vocab, tokenizer, 2), texts)), None

    def handle_label(self, label, **kw):
        labels = label.decode().split('#@#@#@#')
        return[int(label) for label in labels]

    @multi_classification(
        confusion_matrix_normalize="all",
        show_hamming_loss=True,
        show_cohen_kappa_score=True,
        show_roc_auc=False,
        all_labels=[i for i in range(1, 5)],
    )
    def cmp(self, _data_loader):
        _result, _label = [], []
        for _data in _data_loader:
            print(_data)
            _label.extend([int(l) for l in _data["label"]])
            _result.extend([int(r) for r in _data["result"]])
        return _label, _result

    def _load_model(self, device):
        model_path = _ROOT_DIR + "/models/model.i"
        _model = model.TextClassificationModel(1308713, 32, 4).to(device)
        _model.load_state_dict(torch.load(model_path))
        _model.eval()
        vocab_path = _ROOT_DIR + "/models/vocab.i"
        dictionary = torch.load(vocab_path)
        tokenizer = get_tokenizer("basic_english")
        return _model, dictionary, tokenizer
```

### Implement ppl

Starwhale will feed the byte arrays of one batch to the `ppl` method and put the output of `ppl` into an `inference_result` dict, which looks like:

```json
{"result":[{resultObj1},{resultObj2}],"label":[{labelObj1},{labelObj2}]}
```

Starwhale will automatically add the result of `ppl` to `inference_result.result` and the result of `handle_label` to `inference_result.label`.

The `inference_result` is used in the argument of `cmp` named `_data_loader`.

### Implement cmp

`_data_loader` is an iterator for `result` and `label`. For a multiple classification problem, it is pretty easy to implement the `cmp` method by annotating your `cmp` method with the `multi_classification` annotation and coping the lines inside it. Because AG_NEWS has only 4 labels, `all_labels` is set to `[i for i in range(1, 5)]`

If you need to show `roc` and `auc`, you will also need to supply `_pr` in your `ppl` method.

By now, we have finished all the coding parts. Then let's begin the command line part.

## Build Runtime, Model, and Dataset

### Build Runtime

```console
(pytorch_text) $ cd ..
(pytorch_text) $ swcli runtime build .
🚧 start to build runtime bundle...
👷 uri:local/project/self/runtime/pytorch_text
🐦 runtime will ignore pypi editable package
🆕 version mnqtgyjrhezd
📁 workdir: ~/.cache/starwhale/self/workdir/runtime/pytorch_text/mn/mnqtgyjrhezdmodbmu4tezrsgvvdgmq
💫 python3.9.5@venv, os(Linux), include-editable(False), try to export environment...
🌈 runtime docker image: ghcr.io/star-whale/starwhale:0.2.0-alpha.  🌈
🦋 .swrt bundle:~/.cache/starwhale/self/runtime/pytorch_text/mn/mnqtgyjrhezdmodbmu4tezrsgvvdgmq.swrt
```

### Build Dataset

#### Write the yaml file

Here is some descriptive information needed for Starwhale to build a Starwhale Dataset(SWDS). A yaml file describes the information as below:

```yaml
name: AG_NEWS

data_dir: data
data_filter: "test.csv"
label_filter: "test.csv"

process: code.data_slicer:AGNEWSSlicer
pip_req: requirements.txt

desc: AG_NEWS data and label test dataset
tag:
 - bin

attr:
  alignment_size: 4k
  volume_size: 2M
```

Most of the fields are self-explained. The `process` descriptor is the entry point of the data split method. The `data_filter` is for searching files containing data named like `test.csv` recursively under `data_dir`. Then Starwhale will use the files found as the input for `process`.

After creating the yaml file under `${code_base}/example/text_cls_AG_NEWS/`, we are ready.

```console
$ swcli dataset build .
🚧 start to build dataset bundle...
👷 uri:local/project/self/dataset/ag_news
🆕 version gaygeyrsmi2w
📁 swds workdir: ~/.cache/starwhale/self/dataset/ag_news/ga/gaygeyrsmi2wcmrsmuydgy3enrzdm5i.swds
👍 try to copy source code files...
👻 import code.data_slicer:AGNEWSSlicer@~/code/starwhale/example/text_cls_AG_NEWS to make swds...
cleanup done.
finish gen swds @ ~/.cache/starwhale/self/dataset/ag_news/ga/gaygeyrsmi2wcmrsmuydgy3enrzdm5i.swds/data
🤖 calculate signature...
🌺 congratulation! you can run  swcli dataset info ag_news/version/gaygeyrsmi2wcmrsmuydgy3enrzdm5i
  8 out of 8 steps finished ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━ 100% 0:00:00 0:00:00
```

There is one more step left.

### Build Model

Here is some descriptive information for Starwhale to build a Starwhale Model Package(SWMP). A yaml file describes the information as below:

```yaml
version: 1.0
name: text_cls

config:
  - config/hyperparam.json

run:
  ppl: code.ppl:TextClassificationHandler
  pip_req: requirements.txt
  exclude_pkg_data:
    - venv
    - .git
    - .history
    - .vscode

desc: TextClassification by PyTorch

tag:
  - TextClassification
```

Most of the fields are self-explained. The `ppl` descriptor is the entry point of the inference and cmp method.
After creating the yaml file under `${code_base}/example/text_cls_AG_NEWS/`, we are ready.

```console
(pytorch_text) $ swcli model build .
🚧 start to build model bundle...
👷 uri:local/project/self/model/text_cls
🆕 version mq2tmmlfgmzd
📁 workdir: ~/.cache/starwhale/self/workdir/model/text_cls/mq/mq2tmmlfgmzdqztfgbqtmntfg53dk4a
👍 try to copy source code files...
🦋 .swmp bundle:~/.cache/starwhale/self/model/text_cls/mq/mq2tmmlfgmzdqztfgbqtmntfg53dk4a.swmp
```

Here we are. We have finished all the complex parts.

## Run the evaluation job and see the metrics

We have two ways to evaluate our models:

* Evaluate model on a standalone instance
* Evaluate model on a cloud instance

### Evaluate the model on your local standalone instance

#### Create a job

```console
$ swcli -vvv job create self --model text_cls/version/latest --dataset ag_news/version/latest --runtime pytorch_text/version/latest --docker-verbose
2022-06-09 19:09:45.085 | 0:00:01.898337 | DEBUG    | starwhale.utils.debug:init_logger:42 - verbosity: 3, log level: DEBUG
⠋ eval run in local... ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━   0% -:--:-- 0:00:002022-06-09 19:09:45.090 | 0:00:01.903792 | INFO     | starwhale.core.job.executor:_gen_version:113 - [step:version]create eval job version...
2022-06-09 19:09:45.093 | 0:00:01.906845 | INFO     | starwhale.core.job.executor:_gen_version:118 - [step:version]eval job version is mfrdcyrxhftdgzlgmqytsmbsnvztiyq
2022-06-09 19:09:45.093 | 0:00:01.907084 | INFO     | starwhale.core.job.executor:_prepare_workdir:129 - [step:prepare]create eval workdir...
2022-06-09 19:09:45.095 | 0:00:01.908800 | INFO     | starwhale.core.job.executor:_prepare_workdir:151 - [step:prepare]eval workdir: ~/.cache/starwhale/self/job/mf/mfrdcyrxhftdgzlgmqytsmbsnvztiyq
😹 ~/.cache/starwhale/self/workdir/model/text_cls/gz/gzstmmztmyytmztfgbqtmntfn43gqoi existed, skip extract model bundle
😹 ~/.cache/starwhale/self/workdir/runtime/pytorch_text/hb/hbtdqnztmy2dky3cmqzdazjymn4dqzi existed, skip extract model bundle
😹 local_fuse.json existed, skip render
🔍 ~/.cache/starwhale/self/dataset/ag_news/me/me3tkzdgga4tgmrsmuydgy3ehfshu6i.swds/local_fuse.json
⠋ eval run in local... ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━   0% -:--:-- 0:00:002022-06-09 19:09:45.106 | 0:00:01.919282 | DEBUG    | starwhale.core.job.executor:_gen_swds_fuse_json:166 - [gen fuse input.json]~/.cache/starwhale/self/dataset/ag_news/me/me3tkzdgga4tgmrsmuydgy3ehfshu6i.swds/local_fuse.json
2022-06-09 19:09:45.106 | 0:00:01.919864 | INFO     | starwhale.core.job.executor:_do_run_cmd:217 - [run ppl] docker run command output...

🐘 ppl docker cmd
docker run --net=host --rm --name mfrdcyrxhftdgzlgmqytsmbsnvztiyq-ppl -v ~/.cache/starwhale/self/job/mf/mfrdcyrxhftdgzlgmqytsmbsnvztiyq/ppl:/opt/starwhale -v ~/.cache/starwhale/self/workdir/model/text_cls/gz/gzstmmztmyytmztfgbqtmntfn43gqoi/src:/opt/starwhale/swmp/src -v ~/.cache/starwhale/self/workdir/model/text_cls/gz/gzstmmztmyytmztfgbqtmntfn43gqoi/model.yaml:/opt/starwhale/swmp/model.yaml -v ~/.cache/starwhale/self/workdir/runtime/pytorch_text/hb/hbtdqnztmy2dky3cmqzdazjymn4dqzi/dep:/opt/starwhale/swmp/dep -v ~/.cache/starwhale/self/workdir/runtime/pytorch_text/hb/hbtdqnztmy2dky3cmqzdazjymn4dqzi/_manifest.yaml:/opt/starwhale/swmp/_manifest.yaml -v ~/.cache/starwhale/self/dataset:/opt/starwhale/dataset -v ~/.cache/starwhale-pip:/root/.cache/pip -e DEBUG=1 ghcr.io/star-whale/starwhale:latest ppl

🐟 eval run:ppl dir @ ~/.cache/starwhale/self/job/mf/mfrdcyrxhftdgzlgmqytsmbsnvztiyq/ppl
⠋ eval run in local... ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━   0% -:--:-- 0:00:002022-06-09 19:09:45.116 | 0:00:01.929534 | DEBUG    | starwhale.utils.process:log_check_call:20 - cmd: 'docker pull ghcr.io/star-whale/starwhale:latest'
......logs omitted......
......logs omitted......
⠼ run ppl... ━━━━━━━━━━╸━━━━━━━━━━━━━━━━━━━━━━━━━━━━━  26% 0:00:01 0:08:252022-06-09 19:18:11.233 | 0:08:28.046963 | DEBUG    | starwhale.utils.process:log_check_call:25 - 2022-06-09 11:18:11.232 | 0:04:30.197662 | INFO     | starwhale.api._impl.model:_starwhale_internal_run_ppl:268 - [115] data handle -> success
2022-06-09 19:18:11.234 | 0:08:28.047501 | DEBUG    | starwhale.utils.process:log_check_call:25 - 2022-06-09 11:18:11.233 | 0:04:30.198691 | INFO     | starwhale.api._impl.model:_starwhale_internal_run_ppl:229 - [116]data-label loaded, data size:14.73KB, label size:505.00B ,batch:64
⠦ run ppl... ━━━━━━━━━━╸━━━━━━━━━━━━━━━━━━━━━━━━━━━━━  26% 0:00:01 0:08:262022-06-09 19:18:12.375 | 0:08:29.188626 | DEBUG    | starwhale.utils.process:log_check_call:25 - 2022-06-09 11:18:12.374 | 0:04:31.339085 | INFO     | starwhale.api._impl.model:_starwhale_internal_run_ppl:268 - [116] data handle -> success
2022-06-09 19:18:12.377 | 0:08:29.190733 | DEBUG    | starwhale.utils.process:log_check_call:25 - 2022-06-09 11:18:12.377 | 0:04:31.341953 | INFO     | starwhale.api._impl.model:_starwhale_internal_run_ppl:229 - [117]data-label loaded, data size:15.06KB, label size:505.00B ,batch:64
⠏ run ppl... ━━━━━━━━━━╸━━━━━━━━━━━━━━━━━━━━━━━━━━━━━  26% 0:00:01 0:08:272022-06-09 19:18:13.661 | 0:08:30.474491 | DEBUG    | starwhale.utils.process:log_check_call:25 - 2022-06-09 11:18:13.660 | 0:04:32.625002 | INFO     | starwhale.api._impl.model:_starwhale_internal_run_ppl:268 - [117] data handle -> success
2022-06-09 19:18:13.662 | 0:08:30.475502 | DEBUG    | starwhale.utils.process:log_check_call:25 - 2022-06-09 11:18:13.661 | 0:04:32.626688 | INFO     | starwhale.api._impl.model:_wrapper:209 - finish.
2022-06-09 19:18:13.663 | 0:08:30.477178 | DEBUG    | starwhale.utils.process:log_check_call:25 - 2022-06-09 11:18:13.663 | 0:04:32.628394 | Level 20 | rich.console:_check_buffer:1951 - 👏 finish run ppl: PipelineHandler status@/opt/starwhale/status, log@/opt/starwhale/log, result@/opt/starwhale/result
2022-06-09 19:18:13.853 | 0:08:30.666685 | DEBUG    | starwhale.utils.process:log_check_call:25 - 👏 finish run ppl: PipelineHandler status@/opt/starwhale/status, log@/opt/starwhale/log, result@/opt/starwhale/result
2022-06-09 19:18:14.033 | 0:08:30.846635 | DEBUG    | starwhale.utils.process:log_check_call:25 -
2022-06-09 19:18:14.034 | 0:08:30.848083 | INFO     | starwhale.core.job.executor:_do_run_cmd:217 - [run cmp] docker run command output...

🐘 cmp docker cmd
docker run --net=host --rm --name mfrdcyrxhftdgzlgmqytsmbsnvztiyq-cmp -v ~/.cache/starwhale/self/job/mf/mfrdcyrxhftdgzlgmqytsmbsnvztiyq/cmp:/opt/starwhale -v ~/.cache/starwhale/self/workdir/model/text_cls/gz/gzstmmztmyytmztfgbqtmntfn43gqoi/src:/opt/starwhale/swmp/src -v ~/.cache/starwhale/self/workdir/model/text_cls/gz/gzstmmztmyytmztfgbqtmntfn43gqoi/model.yaml:/opt/starwhale/swmp/model.yaml -v ~/.cache/starwhale/self/workdir/runtime/pytorch_text/hb/hbtdqnztmy2dky3cmqzdazjymn4dqzi/dep:/opt/starwhale/swmp/dep -v ~/.cache/starwhale/self/workdir/runtime/pytorch_text/hb/hbtdqnztmy2dky3cmqzdazjymn4dqzi/_manifest.yaml:/opt/starwhale/swmp/_manifest.yaml -v ~/.cache/starwhale/self/job/mf/mfrdcyrxhftdgzlgmqytsmbsnvztiyq/ppl/result:/opt/starwhale/ppl_result -v ~/.cache/starwhale-pip:/root/.cache/pip -e DEBUG=1 ghcr.io/star-whale/starwhale:latest cmp

🐟 eval run:cmp dir @ ~/.cache/starwhale/self/job/mf/mfrdcyrxhftdgzlgmqytsmbsnvztiyq/cmp
⠙ run cmp... ━━━━━━━━━━━━━━━━━━━━━━━━━╺━━━━━━━━━━━━━━  63% -:--:-- 0:08:282022-06-09 19:18:14.043 | 0:08:30.856355 | DEBUG    | starwhale.utils.process:log_check_call:20 - cmd: 'docker pull ghcr.io/star-whale/starwhale:latest'
......logs omitted......
......logs omitted......
2022-06-09 19:18:23.015 | 0:08:39.828563 | DEBUG    | starwhale.utils.process:log_check_call:25 - 2022-06-09 11:18:23.013 | 0:00:01.444597 | INFO     | starwhale.api._impl.model:_wrapper:209 - finish.
⠼ run cmp... ━━━━━━━━━━━━━━━━━━━━━━━━━╺━━━━━━━━━━━━━━  63% -:--:-- 0:08:372022-06-09 19:18:23.159 | 0:08:39.972502 | DEBUG    | starwhale.utils.process:log_check_call:25 - 2022-06-09 11:18:23.014 | 0:00:01.445667 | Level 20 | rich.console:_check_buffer:1951 - 👏 finish run cmp: PipelineHandler status@/opt/starwhale/status, log@/opt/starwhale/log, result@/opt/starwhale/result
2022-06-09 19:18:23.159 | 0:08:39.972795 | DEBUG    | starwhale.utils.process:log_check_call:25 - {'index': 116, 'result': [2, 3, 1, 4, 2, 4, 3, 3, 2, 2, 2, 2, 1, 1, 3, 4, 1, 3, 2, 1, 3, 2, 1, 2, 3, 3, 3, 3, 3, 4, 2, 3, 4, 2, 1, 1, 1, 1, 3, 3, 3, 3, 3, 1, 3, 1, 4, 2, 4, 1, 1, 1, 1, 1, 4, 1, 3, 1, 1, 1, 3, 4, 2, 2], 'pr': None, 'batch': 64, 'label': [2, 3, 4, 4, 2, 4, 3, 3, 2, 2, 2, 2, 1, 4, 3, 4, 4, 3, 2, 1, 3, 2, 3, 2, 3, 3, 3, 3, 3, 4, 2, 3, 4, 2, 1, 1, 1, 4, 3, 3, 3, 3, 4, 1, 3, 4, 3, 1, 4, 1, 1, 2, 1, 1, 4, 2, 3, 4, 1, 1, 2, 4, 2, 2]}
2022-06-09 19:18:23.314 | 0:08:40.128161 | DEBUG    | starwhale.utils.process:log_check_call:25 - {'index': 117, 'result': [3, 2, 1, 4, 1, 4, 4, 1, 1, 4, 1, 4, 2, 3, 1, 3, 4, 1, 2, 3, 3, 1, 1, 3, 1, 4, 4, 1, 1, 2, 4, 1, 4, 4, 3, 3, 3, 2, 1, 4, 2, 1, 3, 2, 2, 1, 1, 3, 1, 3, 1, 4, 1, 4, 3, 1, 1, 1, 4, 3, 3, 3, 1, 2], 'pr': None, 'batch': 64, 'label': [3, 2, 1, 4, 3, 4, 4, 1, 3, 4, 4, 4, 2, 3, 1, 3, 4, 1, 2, 3, 3, 1, 1, 3, 1, 4, 4, 3, 1, 2, 4, 1, 4, 4, 4, 3, 3, 2, 1, 4, 2, 1, 3, 2, 2, 1, 1, 3, 1, 3, 1, 1, 1, 4, 4, 3, 1, 1, 4, 3, 3, 3, 2, 2]}
  7 out of 7 steps finished ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━ 100% 0:00:00 0:08:38
👏 success to create job(project id: self)
```

#### See the metrics

```console
(pytorch_text) $ swcli job info mfrdcyrxhftdgzlgmqytsmbsnvztiyq
{
    'created_at': '2022-06-09 19:09:45 CST',
    'datasets': [
        'local/project/self/dataset/ag_news/version/latest'
    ],
    'desc': None,
    'finished_at': '2022-06-09 19:18:23 CST',
    'model': 'local/project/self/model/text_cls/version/latest',
    'name': None,
    'phase': 'all',
    'runtime': 'local/project/self/runtime/pytorch_text/version/latest',
    'status': 'success',
    'version': 'mfrdcyrxhftdgzlgmqytsmbsnvztiyq'
}

🌵 ppl: ~/.cache/starwhale/self/job/mf/mfrdcyrxhftdgzlgmqytsmbsnvztiyq/ppl
🐫 cmp: ~/.cache/starwhale/self/job/mf/mfrdcyrxhftdgzlgmqytsmbsnvztiyq/cmp

Summary
├── accuracy: 0.8059                                                                                   Label   Precision   Recall   F1-score   Support
├── macro avg                                                                                         ───────────────────────────────────────────────────
│   ├── precision: 0.8427                                                                              1       0.6313      0.9487   0.7581     3780.0000
│   ├── recall: 0.8059                                                                                 2       0.9610      0.8110   0.8797     3768.0000
│   ├── f1-score: 0.8098                                                                               3       0.8554      0.7834   0.8178     3776.0000
│   └── support: 15104.0000                                                                            4       0.9232      0.6804   0.7834     3780.0000
├── weighted avg
│   ├── precision: 0.8426
│   ├── recall: 0.8059
│   ├── f1-score: 0.8097
│   └── support: 15104.0000
├── hamming_loss: 0.1941
└── cohen_kappa_score: 0.7412

  Label   TP      TN     FP     FN                                                                                                            1        2        3        4
 ────────────────────────────────────                                                                                                    ───────────────────────────────────────
  1       9230    2094   194    3586                                                                                                      1   0.2374   0.0048   0.0061   0.0020
  2       11212   124    712    3056                                                                                                      2   0.0446   0.2023   0.0021   0.0004
  3       10828   500    818    2958                                                                                                      3   0.0413   0.0011   0.1958   0.0118
  4       11110   214    1208   2572                                                                                                      4   0.0527   0.0024   0.0249   0.1703
```

> :bulb: Docker is required to run as a demon service on the machine

Congratulations, we have finished the whole example! From now on, we can update the training method, get a new model, build a new SWMP, and evaluate our model from time to time.

### Evaluate model on a cloud instance

* **Log in to one cloud instance**

```console
(pytorch_text) $ swcli instance login http://console.pre.intra.starwhale.ai --username starwhale --password abcd1234 --alias pre-k8s
‍🍳 login http://console.pre.intra.starwhale.ai successfully!
```

* **Copy the model we built before to the cloud instance**

```console
(pytorch_text) $ swcli model copy text_cls/version/gzstmmztmyyt cloud://pre-k8s/project/starwhale
🚧 start to copy local/project/self/model/text_cls/version/gzstmmztmyyt -> http://console.pre.intra.starwhale.ai/project/1...
  🎳 upload gzstmmztmyytmztfgbqtmntfn43gqoi.swmp ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━ 100% 0:00:16 196.7 MB 10.6 MB/s
👏 copy done.
```

* **Copy the dataset we built before to the cloud instance**

```console
(pytorch_text) $ swcli dataset copy ag_news/version/me3tkzdgga4t cloud://pre-k8s/project/starwhale
    🚧 start to copy local/project/self/dataset/ag_news/version/me3tkzdgga4t -> http://console.pre.intra.starwhale.ai/project/1...
    ⬆ _manifest.yaml         ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━ 100% 0:00:00 1.2 kB   ?
⬆ data_ubyte_0.swds_bin  ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━ 100% 0:00:00 2.0 MB   ?
⬆ index.jsonl            ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━ 100% 0:00:00 20.9 kB  ?
⬆ label_ubyte_0.swds_bin ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━ 100% 0:00:00 479.6 kB ?
⬆ archive.swds_meta      ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━ 100% 0:00:00 20.5 kB  ?
👏 copy done
```

* **Copy the runtime we built before to the cloud instance**

```console
(pytorch_text) $ swcli runtime copy pytorch_text/version/hbtdqnztmy2d cloud://pre-k8s/project/starwhale
🚧 start to copy local/project/self/runtime/pytorch_text/version/hbtdqnztmy2d -> http://console.pre.intra.starwhale.ai/project/1...
  🎳 upload hbtdqnztmy2dky3cmqzdazjymn4dqzi.swrt ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━ 100% 0:00:00 20.5 kB ?
👏 copy done.
```

* **Go to the console and create one job**

![img.png](../img/create_job_ag_news.png)
