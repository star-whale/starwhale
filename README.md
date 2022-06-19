<p align="center">
    <img src="https://raw.githubusercontent.com/star-whale/starwhale/main/docs/static/img/starwhale.png" width="600" style="max-width: 600px;">
</p>

<p align="center">

<a href=https://github.com/ambv/black>
    <img src="https://img.shields.io/badge/code%20style-black-000000.svg">
</a>

<a href="https://starwhale.slack.com">
    <img src="https://img.shields.io/static/v1.svg?label=chat&message=on%20slack&color=27b1ff&style=flat">
</a>

<a href="https://pypi.org/project/starwhale/">
    <img src="https://img.shields.io/pypi/v/starwhale?style=flat">
</a>

<a href="https://github.com/star-whale/starwhale/blob/main/LICENSE">
    <img src="https://img.shields.io/github/license/star-whale/starwhale?style=flat">
</a>

<a href="https://github.com/star-whale/starwhale/actions/workflows/client.yaml">
    <img src="https://github.com/star-whale/starwhale/actions/workflows/client.yaml/badge.svg">
</a>

<a href="https://github.com/star-whale/starwhale/actions/workflows/server-build.yml">
    <img src="https://github.com/star-whale/starwhale/actions/workflows/server-build.yml/badge.svg">
</a>

<a href="https://github.com/star-whale/starwhale/actions/workflows/console.yml">
    <img src="https://github.com/star-whale/starwhale/actions/workflows/console.yml/badge.svg">
</a>

<a href='https://coveralls.io/github/star-whale/starwhale?branch=main'>
    <img src='https://coveralls.io/repos/github/star-whale/starwhale/badge.svg?branch=main' alt='Coverage Status' />
</a>

<a href='https://artifacthub.io/packages/helm/starwhale/starwhale'>
    <img src='https://img.shields.io/endpoint?url=https://artifacthub.io/badge/repository/starwhale' alt='Artifact Hub'/>
</a>

</p>

## What is Starwhale

Starwhale is a mlops platform. It provides **Instance**, **Project**, **Runtime**, **Model** and **Dataset** core concepts.

- **Instance**: Starwhale platform delivery method.
  - 👻 **Standalone Instance**: The simplest form that requires only the Starwhale Client(`swcli`). `swcli` is written by pure python3.
  - 🎍 **On-Premises Instance**: Cloud form, we call it **private cloud instance**. Kubernetes and BareMetal both meet the basic environmental requirements.
  - ☁️ **Cloud Hosted Instance**: Cloud form, we call it **public cloud instance**. Starwhale team maintains web service.

  **Starwhale tries to keep concepts consistent across different types of instances. In this way, people can easily exchange data and migrate between instances.**

- **Project**: The basic unit for organizing different resources.

- **ML Basic Elements**: The Machine Learning/Deep Learning running environments or artifacts. Starwhale empowers the ML/DL basic elements with packaging, versioning, reproducibility and shareable.
  - 🐌 **Runtime**: Describe software dependencies to "run", which includes python libraries, native libraries, native binaries etc.
  - 🐇 **Model**: Use `model.yaml` to describe how to organize the model arbitrary files.
  - 🐫 **Dataset**: Use `dataset.yaml` to process and package data files, then produce a Starwhale dataset. It is a unified description of how the data and labels are stored and organized. Starwhale dataset can be loaded efficiently.

- **Running Fundamentals**: Starwhale uses **Job**, **Step** and **Task** to execute ML/DL actions like model training， evaluation and serving. Starwhale **Controller-Agents** structure make scale-out easily.
  - 🥕 **Job**: A set of programs to do specific work, each job consists of one or more steps.
  - 🌵 **Step**: Represent distinct stages of the work, each step consists of one or more tasks.
  - 🥑 **Task**: Operation entity, tasks are in some specific steps.

- **Scenarios**: Starwhale provides the best practice and out-of-the-box for different ML/DL scenarios.
  - 🚝 **Model Training**: Use Starwhale Python SDK to record experiment meta, metric, log and artifact.
  - 🛥️ **Model Evaluation**: `PipelineHandler` and some report decorators can help you only write some preprocessing and post-processing code, then get a completed, useful, user-friendly evaluation reports.
  - 🛫 **Model Serving**: Starwhale Model is able to deploy as a web service or stream service in production, which will own deployment capability, observability and scalability directly, data and algorithm engineers should not write the ml/dl irrelevant code.

## MNIST Quick Tour for standalone instance

<a href="https://starwhale.ai"><img src="https://github.com/star-whale/starwhale/blob/main/docs/docs/img/core-workflow.gif?raw=true" width="100%"></a>

- 🍰 **STEP1**: installing starwhale

    ```bash
    python3 -m pip install --pre starwhale
    ```

- 🍵 **STEP2**: downloading mnist example

    ```bash
    git clone git@github.com:star-whale/starwhale.git
    cd starwhale/example/mnist
    ```

- ☕ **STEP3**: building runtime

    ```bash
    swcli runtime create -n pytorch-mnist -m venv --python 3.9 .
    source venv/bin/activate
    python3 -m pip install -r requirements.txt
    swcli runtime build .
    swcli runtime info pytorch-mnist/version/latest
    ```

- 🍞 **STEP4**: building model

  - Write some code with Starwhale Python SDK, full code is [here](https://github.com/star-whale/starwhale/blob/main/example/mnist/mnist/ppl.py).

   ```python
   from starwhale.api.model import PipelineHandler
   from starwhale.api.metric import multi_classification

   class MNISTInference(PipelineHandler):

       def __init__(self):
        super().__init__(merge_label=True, ignore_error=False)
        self.model = self._load_model()

       def ppl(self, data:bytes, batch_size:int, **kw):
           data = self._pre(data, batch_size)
           output = self.model(data)
           return self._post(output)

        def handle_label(self, label:bytes, batch_size:int, **kw):
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
                _label.extend([int(l) for l in _data["label"]])
                _result.extend([int(l) for l in _data["result"]])
                _pr.extend([l for l in _data["pr"]])
                return _label, _result, _pr

        def _pre(self, input:bytes, batch_size:int):
            """write some mnist preprocessing code"""

        def _post(self, input:bytes):
            """write some mnist post-processing code"""

        def _load_model():
            """load your pre trained model"""
   ```

  - Define `model.yaml`.

  ```yaml
  name: mnist
  model:
    - models/mnist_cnn.pt
  config:
    - config/hyperparam.json
  run:
    ppl: mnist.ppl:MNISTInference
  ```

  - Run one command to build model.

   ```bash
    swcli model build .
    swcli model info mnist/version/latest
   ```

- 🍺 **STEP5**: building dataset

  - Download MNIST RAW data files.

   ```bash
    mkdir -p data && cd data
    wget http://yann.lecun.com/exdb/mnist/t10k-images-idx3-ubyte.gz
    wget http://yann.lecun.com/exdb/mnist/t10k-labels-idx1-ubyte.gz
    gzip -d *.gz
    cd ..
    ls -lah data/*
   ```

  - Write some code with Starwhale Python SDK, full code is [here](https://github.com/star-whale/starwhale/blob/main/example/mnist/mnist/process.py).

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
    mode: generate
    data_dir: data
    data_filter: "t10k-image*"
    label_filter: "t10k-label*"
    process: mnist.process:DataSetProcessExecutor
    attr:
      batch_size: 50
      alignment_size: 4k
      volume_size: 2M
   ```

  - Run one command to build dataset.

   ```bash
    swcli dataset build .
    swcli dataset info mnist/version/latest
   ```

- 🍖 **STEP6**: running evaluation job

   ```bash
    swcli -vvv job create --model mnist/version/latest --dataset mnist/version/latest
    swcli job list
    swcli job info ${version}
   ```

👏 Now you have completed the basic step for starwhale standalone.

## Documentation, Community and Support

- Visit [Starwhale HomePage](https://starwhale.ai).
- More information in the [official documentation](https://doc.starwhale.ai).
- For general questions and support, join the [Slack](https://starwhale.slack.com/).
- For bug reports and feature requests, please use [Github Issue](https://github.com/star-whale/starwhale/issues).
- To get community updates, follow [@starwhaleai](https://twitter.com/starwhaleai) on Twitter.
- For starwhale artifacts, please visit:

  - Python Package on [Pypi](https://pypi.org/project/starwhale/).
  - Helm Charts on [Artifacthub](https://artifacthub.io/packages/helm/starwhale/starwhale).
  - Docker Images on [Docker Hub](https://hub.docker.com/u/starwhaleai) and [ghcr.io](https://github.com/orgs/star-whale/packages).

- Additional, you can always finds us at *developer@starwhale.ai*.

## Contributing

🌼👏**PRs are always welcomed** 👍🍺. See more details in [How to contribute starwhale](https://doc.starwhale.ai/contribute).

## License

Starwhale is Licensed under the [Apache License 2.0](https://github.com/star-whale/starwhale/blob/main/LICENSE).
