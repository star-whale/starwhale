---
title: MNIST with Pytorch
---

MNIST is the hello world code for Machine Learning. This document will let you master all core Starwhale concepts and workflows.

## Downloading the MNIST example

```bash
git clone https://github.com/star-whale/starwhale.git
cd starwhale/example/mnist
```

## Creating and building Runtime

```bash
swcli runtime create -n pytorch-mnist -m venv --python 3.9 .
source venv/bin/activate
python3 -m pip install -r requirements.txt
swcli runtime build .
swcli runtime info pytorch-mnist/version/latest
```

## Train the MNIST Model

```bash
make train
```

output: models/mnist_cnn.pt, which is pre-trained model.

## Building Model

- Write some code with Starwhale Python SDK. Full code is [here](https://github.com/star-whale/starwhale/blob/main/example/mnist/mnist/ppl.py).

```python
from starwhale.api.model import PipelineHandler
from starwhale.api.metric import multi_classification

class MNISTInference(PipelineHandler):

    def __init__(self):
        super().__init__(merge_label=True, ignore_error=False)
        self.model = self._load_model()

    def ppl(self, data:bytes, **kw):
        data = self._pre(data)
        output = self.model(data)
        return self._post(output)

    def handle_label(self, label:bytes, **kw):
        return [int(l) for l in label]

    @multi_classification(
        confusion_matrix_normalize="all",
        show_hamming_loss=True,
        show_cohen_kappa_score=True,
        show_roc_auc=True,
        all_labels=[i for i in range(0, 10)],
    )
    def cmp(self, _data_loader:"DataLoader"):
        _result, _label, _pr = [], [], []
        for _data in _data_loader:
            _label.extend([int(l) for l in _data[self._label_field]])
            # unpack data according to the return value of function ppl
            (pred, pr) = _data[self._ppl_data_field]
            _result.extend([int(l) for l in pred])
            _pr.extend([l for l in pr])

    def _pre(self, input:bytes):
        """write some mnist preprocessing code"""

    def _post(self, input:bytes):
        """write some mnist post-processing code"""

    def _load_model():
        """load your pre trained model"""
```

- Define `model.yaml`.

```yaml
name: mnist
config:
- config/hyperparam.json
run:
ppl: mnist.ppl:MNISTInference
```

## Building Dataset

- Write some code with Starwhale Python SDK. Full code is [here](https://github.com/star-whale/starwhale/blob/main/example/mnist/mnist/process.py).

 ```python
  from starwhale.api.dataset import BuildExecutor

  class DataSetProcessExecutor(BuildExecutor):

      def iter_data_slice(self, path: str):
          """read data file, unpack binary data, yield data bytes"""

      def iter_label_slice(self, path: str):
          """read label file, unpack binary data, yield label bytes"""
 ```

- Define `dataset.yaml`.

 ```yaml
  name: mnist
  data_dir: data
  data_filter: "t10k-image*"
  label_filter: "t10k-label*"
  process: mnist.process:DataSetProcessExecutor
  attr:
    alignment_size: 4k
    volume_size: 2M
 ```

- Run one command to build the dataset.

 ```bash
  swcli dataset build .
  swcli dataset info mnist/version/latest
 ```

## Running Standalone Evaluation Job

```bash
swcli -vvv job create --model mnist/version/latest --dataset mnist/version/latest
swcli job list
swcli job info ${version}
```

## Copying Model/Dataset/Runtime into Cloud instance

```bash
swcli model copy mnist/version/latest cloud://pre-k8s/project/1
swcli dataset copy mnist/version/latest cloud://pre-k8s/project/1
swcli runtime copy pytorch-mnist/version/latest cloud://pre-k8s/project/1
```
