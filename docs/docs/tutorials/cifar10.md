---
title: Image Classification on CIFAR-10
---

This example will illustrate how to evaluate a pre-trained classification model on Starwhale in 5 steps.

1. Train the model
2. Implement the dataset slicing method
3. Implement the inference method and the evaluation metrics computing method
4. Build Dataset and Model
5. Run the evaluation job and see the metrics

## Prerequisites

* Assume that you have Python3.7 or above installed

Clone the Starwhale repo and install the requirements:

```console
git clone https://github.com/star-whale/starwhale.git
cd starwhale/example/cifar10
#create your virtual environment if needed
pip install -r requirements.txt
```

> :bulb: If you are from the mainland of China, we strongly recommend using a proxy.

## Train the model

> The training code in this repo is sourced from [cifar10 tutorial](https://pytorch.org/tutorials/beginner/blitz/cifar10_tutorial.html).

```console
mkdir models
cd code
python3 train.py
```

The training process is relatively slow on your laptop. You can reduce the train epochs in `train.py` to make it faster.

You will get the logs below:

```console
Downloading https://www.cs.toronto.edu/~kriz/cifar-10-python.tar.gz to ../data/cifar-10-python.tar.gz
100.0%
Extracting ../data/cifar-10-python.tar.gz to ../data
[1,  2000] loss: 2.180
[1,  4000] loss: 1.817
......
......
[10, 12000] loss: 0.763
Finished Training
```

Great! Now you have your model trained and saved. You can see it in the `models` directory.

## Slice the test dataset using the Starwhale protocol

In the training section, we got a dataset called [CIFA-10](https://www.cs.toronto.edu/~kriz/cifar.html).

```console
$ cd ../data
$ ls
cifar-10-batches-py  cifar-10-python.tar.gz
$ ls cifar-10-batches-py
batches.meta  data_batch_1  data_batch_2  data_batch_3  data_batch_4  data_batch_5  readme.html  test_batch
```

The test part of the dataset is a single file of size 30MB called `test_batch`, which contains 10,000 images and labels.

Before version `0.1.2b7`, Starwhale sliced the dataset into chunks where the batched images and labels reside. You must tell Starwhale how to yield batches of byte arrays from each dataset file.

In this example, we will `unpickle` the dataset to get the NumPy arrays of each image and a list of labels, then transform them into byte arrays.

```python
class CIFAR10Slicer(BuildExecutor):

    def iter_data_slice(self, path: str):
        content_dict = unpickle(path)
        data_numpy = content_dict.get(b'data')
        idx = 0
        data_size = len(data_numpy)
        while True:
            last_idx = idx
            idx += 1
            if idx > data_size:
                break
            yield data_numpy[last_idx:idx].tobytes()

    def iter_label_slice(self, path: str):
        content_dict = unpickle(path)
        labels_list = content_dict.get(b'labels')
        idx = 0
        data_size = len(labels_list)
        while True:
            last_idx = idx
            idx += 1
            if idx > data_size:
                break
            yield bytes(labels_list[last_idx:idx])
```

You need to extend the abstract class `BuildExecutor`, so Starwhale can use it. The `path` argument is a file that matches `data_filter` or `label_filter` in `${code_base}/example/cifar10/dataset.yaml`. The filters used in this example are  `test_batch`.

## Implement the inference method and evaluation metrics computing method

The inference method is called `ppl,` and the evaluation metrics computing method is called `cmp`.
Here is the code snap from `ppl.py`, which implements both methods. You need to extend the abstract class `PipelineHandler` so you can receive the byte arrays, which you transformed in the last step.

```python
class CIFAR10Inference(PipelineHandler):

    def __init__(self, device="cpu") -> None:
        super().__init__(merge_label=True, ignore_error=True)
        self.device = torch.device(device)
        self.model = self._load_model(self.device)

    def ppl(self, data, **kw):
        data = self._pre(data)
        output = self.model(data)
        return self._post(output)

    def handle_label(self, label, **kw):
        return [int(l) for l in label]

    @multi_classification(
        confusion_matrix_normalize="all",
        show_hamming_loss=True,
        show_cohen_kappa_score=True,
        show_roc_auc=True,
        all_labels=[i for i in range(0, 10)],
    )
    def cmp(self, _data_loader):
        _result, _label, _pr = [], [], []
        for _data in _data_loader:
            _label.extend([int(l) for l in _data["label"]])
            _result.extend([int(l) for l in _data["result"]])
            _pr.extend([l for l in _data["pr"]])
        return _label, _result, _pr

    def _pre(self, input: bytes):
        batch_size = 1
        images = []
        from_buffer = np.frombuffer(input, 'uint8')
        shape = (batch_size, ONE_IMAGE_SIZE)
        batch_numpy_flatten_data = from_buffer.reshape(shape)
        batch_numpy_flatten_data = np.vstack([batch_numpy_flatten_data]).reshape(-1, 3, 32, 32)
        batch_numpy_flatten_data = batch_numpy_flatten_data.transpose((0, 2, 3, 1))
        shape_image = (WIDTH_IMAGE, HEIGHT_IMAGE, CHANNEL_IMAGE)
        for i in range(0, batch_size):
            numpy_flatten_data_i_ = batch_numpy_flatten_data[i]
            _image = Image.fromarray(numpy_flatten_data_i_.reshape(shape_image))
            _image = transforms.Compose(
                [transforms.ToTensor(),
                transforms.Normalize((0.5, 0.5, 0.5), (0.5, 0.5, 0.5))])(_image)
            images.append(_image)
        return torch.stack(images).to(self.device)

    def _post(self, input):
        pred_value = input.argmax(1).flatten().tolist()
        probability_matrix = np.exp(input.tolist()).tolist()
        return pred_value, probability_matrix

    def _load_model(self, device):
        model = Net().to(device)
        model.load_state_dict(torch.load(str(ROOTDIR / "models/cifar_net.pth")))
        model.eval()
        print("load cifar_net model, start to inference...")
        return model
```

### Implement ppl

Starwhale will feed the byte arrays of one batch to the `ppl` method. And take the output of `ppl` into an `inference_result` dict, which looks like

```json
{"result":[{resultObj1},{resultObj2}],"label":[{labelObj1},{labelObj2}]}
```

Now let's look at how `inference_result` is produced using the byte arrays of one batch.

First, we load our model trained before with the `_load_model` method. Then we transform the byte array to a tensor which is the input for the model using `_pre`. After that, we make the inference. At last, we convert the output tensor into labels with the `_post` method.
By the way, we also overwrite the `handle_label` method.

Starwhale will automatically add the result of `ppl` to `inference_result.result` and add the result of `handle_label` to `inference_result.label`.

The `inference_result` is used in the argument of `cmp` named `_data_loader`.

### Implement cmp

`_data_loader` is an iterator for `result` and `label`. For a multiple classification problem, it is pretty easy to implement the `cmp` method by annotating your `cmp` method with the `multi_classification` annotation and coping the lines inside it.

```python
    @multi_classification(
        confusion_matrix_normalize="all",
        show_hamming_loss=True,
        show_cohen_kappa_score=True,
        show_roc_auc=True,
        all_labels=[i for i in range(0, 10)],
    )
    def cmp(self, _data_loader):
        _result, _label, _pr = [], [], []
        for _data in _data_loader:
            _label.extend([int(l) for l in _data["label"]])
            _result.extend([int(l) for l in _data["result"]])
            _pr.extend([l for l in _data["pr"]])
        return _label, _result, _pr
```

If you need to show `roc` and `auc`, you must supply `_pr` in your `ppl` method.

By now, we have finished all the coding parts. Then let's begin the command line part.

## Build Dataset and Model

### Build Dataset

Here is some descriptive information needed for Starwhale to build a Starwhale Dataset(SWDS). A yaml file describes the information as below:

```yaml
name: cifar10
data_dir: data
data_filter: "test_batch"
label_filter: "test_batch"

process: code.data_slicer:CIFAR10Slicer
pip_req: requirements.txt

desc: CIFAR10 data and label test dataset

attr:
    alignment_size: 4k
    volume_size: 2M
```

Most of the fields are self-explained. The `process` descriptor is the entry point of the data split method. The `data_filter` is for searching files containing data named like `test_batch` recursively under `data_dir`. Then Starwhale will use the files found as the input for `process`.

After creating the yaml file under `${code_base}/example/cifar10/`, we are ready.

```console
$ cd ..
$ swcli dataset build .
🆕 swmp version g4ytezbqgfrt
📁 swmp workdir: /home/anda/.cache/starwhale/dataset/cifar10/g4ytezbqgfrtmodche3wcnrupfwta5a
👍 try to copy source code files...
👻 import <code.data_slicer.CIFAR10Slicer object at 0x7faa927a5fa0> to make swds...
cleanup done.
💫 python3.8.13@conda, try to export environment...
🤖 calculate signature...
🌺 congratulation! you can run  swcli dataset info cifar10:g4ytezbqgfrtmodche3wcnrupfwta5a
8 out of 8 steps finished ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━ 100% 0:00:00 0:00:04
```

There is one more step left.

### Build Model

Here is some descriptive information for Starwhale to build a Starwhale Model Package(SWMP). A yaml file describes the information as below:

```yaml
version: 1.0
name: cifar_net

config:
- config/hyperparam.json

run:
ppl: code.ppl:CIFAR10Inference
pip_req: requirements.txt
desc: cifar10 by pytorch

tag:
- multi_classification
```

Most of the fields are self-explained. The `ppl` descriptor is the entry point of the inference and cmp method.
After creating the yaml file under `${code_base}/example/cifar10/`, we are ready.

```console
$ swcli model build . --skip-gen-env
🆕 swmp version hfqtimrxgy4g
📁 swmp workdir: /home/anda/.cache/starwhale/workdir/cifar_net/hfqtimrxgy4gcztfgq4gmzten42gc6a
👍 try to copy source code files...
💫 python3.8.13@conda, try to export environment...
🌺 congratulation! you can run  swcli model info cifar_net:hfqtimrxgy4gcztfgq4gmzten42gc6a
6 out of 6 steps finished ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━ 100% 0:00:00 0:00:03
```

Here we are. We have finished all the complex parts.

## Run the evaluation job and see the metrics

Before we evaluate our model, we should copy the runtime, model, and dataset to the Starwhale instance. Open the console, create one job, and look at the evaluation metrics.

Congratulations, we have finished the whole example! From now on, we can update the training method, get a new model, build a new SWMP and evaluate our model from time to time.
