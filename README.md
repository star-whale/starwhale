<p align="center">
    <img src="https://github.com/star-whale/starwhale/raw/main/docs/static/img/starwhale.png" width="600" style="max-width: 600px;">
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

<a href="https://pypi.org/project/starwhale/">
    <img alt="PyPI - Python Version" src="https://img.shields.io/pypi/pyversions/starwhale">
</a>

<a href="https://github.com/star-whale/starwhale/actions/workflows/client.yaml">
    <img src="https://github.com/star-whale/starwhale/actions/workflows/client.yaml/badge.svg">
</a>

<a href="https://github.com/star-whale/starwhale/actions/workflows/server-ut-report.yml">
    <img src="https://github.com/star-whale/starwhale/actions/workflows/server-ut-report.yml/badge.svg">
</a>

<a href="https://github.com/star-whale/starwhale/actions/workflows/console.yml">
    <img src="https://github.com/star-whale/starwhale/actions/workflows/console.yml/badge.svg">
</a>

<a href='https://app.codecov.io/gh/star-whale/starwhale'>
    <img alt="Codecov" src="https://img.shields.io/codecov/c/github/star-whale/starwhale?flag=controller&label=Java%20Cov">
</a>

<a href="https://app.codecov.io/gh/star-whale/starwhale">
    <img alt="Codecov" src="https://img.shields.io/codecov/c/github/star-whale/starwhale?flag=standalone&label=Python%20cov">
</a>

<a href='https://artifacthub.io/packages/helm/starwhale/starwhale'>
    <img src='https://img.shields.io/endpoint?url=https://artifacthub.io/badge/repository/starwhale' alt='Artifact Hub'/>
</a>

<a href="https://github.com/star-whale/starwhale/actions/workflows/e2e-test.yml">
    <img src='https://github.com/star-whale/starwhale/actions/workflows/e2e-test.yml/badge.svg' alt='Starwhale E2E Test'>
</a>

</p>

## What is Starwhale

Starwhale is an MLOps platform. It provides **Instance**, **Project**, **Runtime**, **Model**, and **Dataset**.

- **Instance**: Each installation of Starwhale is called an instance.
  - 👻 **Standalone Instance**: The simplest form that requires only the Starwhale Client(`swcli`). `swcli` is written by pure python3.
  - 🎍 **On-Premises Instance**: Cloud form, we call it **private cloud instance**. Kubernetes and BareMetal both meet the basic environmental requirements.
  - ☁️ **Cloud Hosted Instance**: Cloud form, we call it **public cloud instance**. Starwhale team maintains the web service.

  **Starwhale tries to keep concepts consistent across different types of instances. In this way, people can easily exchange data and migrate between them.**

- **Project**: The basic unit for organizing different resources.

- **ML Basic Elements**: The Machine Learning/Deep Learning running environments or artifacts. Starwhale empowers the ML/DL essential elements with packaging, versioning, reproducibility, and shareability.
  - 🐌 **Runtime**: Software dependencies description to "run" a model, which includes python libraries, native libraries, native binaries, etc.
  - 🐇 **Model**: The standard model format used in model delivery.
  - 🐫 **Dataset**: A unified description of how the data and labels are stored and organized. Starwhale datasets can be loaded efficiently.

- **Running Fundamentals**: Starwhale uses **Job**, **Step**, and **Task** to execute ML/DL actions like model training， evaluation, and serving. Starwhale's **Controller-Agents** structure scales out easily.
  - 🥕 **Job**: A set of programs to do specific work. Each job consists of one or more steps.
  - 🌵 **Step**: Represents distinct stages of the work. Each step consists of one or more tasks.
  - 🥑 **Task**: Operation entity. Tasks are in some specific steps.

- **Scenarios**: Starwhale provides the best practice and out-of-the-box for different ML/DL scenarios.
  - 🚝 **Model Training(TBD)**: Use Starwhale Python SDK to record experiment meta, metric, log, and artifact.
  - 🛥️ **Model Evaluation**: `PipelineHandler` and some report decorators can give you complete, helpful, and user-friendly evaluation reports with only a few lines of codes.
  - 🛫 **Model Serving(TBD)**: Starwhale Model can be deployed as a web service or stream service in production with deployment capability, observability, and scalability. Data scientists do not need to write ML/DL irrelevant codes.

## MNIST Quick Tour for the standalone instance

### Use Notebook

- You can try it in [Google Colab](https://colab.research.google.com/github/star-whale/starwhale/blob/main/example/mnist/notebook.ipynb)
- Or run [example/mnist/notebook.ipynb](example/mnist/notebook.ipynb) locally using [vscode](https://code.visualstudio.com/) or [jupyterlab](https://github.com/jupyterlab/jupyterlab)

### Use your own python env

![Core Job Workflow](docs/docs/img/standalone-core-workflow.gif)

- 🍰 **STEP1**: Installing Starwhale

    ```bash
    python3 -m pip install --pre starwhale
    ```

- 🍵 **STEP2**: Downloading the MNIST example

    ```bash
    git clone https://github.com/star-whale/starwhale.git
    ```

- ☕ **STEP3**: Building a runtime

    ```bash
    cd example/runtime/pytorch
    swcli runtime build .
    swcli runtime list
    swcli runtime info pytorch/version/latest
    ```

- 🍞 **STEP4**: Building a model

  - Enter `example/mnist` directory:

   ```bash
   cd ../../mnist
   ```

  - Write some code with Starwhale Python SDK. Complete code is [here](https://github.com/star-whale/starwhale/blob/main/example/mnist/mnist/evaluator.py).

   ```python
   import typing as t
   import torch
   from starwhale import Image, PipelineHandler, PPLResultIterator, multi_classification

   class MNISTInference(PipelineHandler):
        def __init__(self) -> None:
            super().__init__()
            self.device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
            self.model = self._load_model(self.device)

        def ppl(self, img: Image, **kw: t.Any) -> t.Tuple[t.List[int], t.List[float]]:
            data_tensor = self._pre(img)
            output = self.model(data_tensor)
            return self._post(output)

        @multi_classification(
            confusion_matrix_normalize="all",
            show_hamming_loss=True,
            show_cohen_kappa_score=True,
            show_roc_auc=True,
            all_labels=[i for i in range(0, 10)],
        )
        def cmp(
            self, ppl_result: PPLResultIterator
        ) -> t.Tuple[t.List[int], t.List[int], t.List[t.List[float]]]:
            result, label, pr = [], [], []
            for _data in ppl_result:
                label.append(_data["annotations"]["label"])
                result.extend(_data["result"][0])
                pr.extend(_data["result"][1])
            return label, result, pr

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
  model:
    - models/mnist_cnn.pt
  config:
    - config/hyperparam.json
  run:
    handler: mnist.evaluator:MNISTInference
  ```

  - Run one command to build the model.

   ```bash
    swcli model build .
    swcli model info mnist/version/latest
   ```

- 🍺 **STEP5**: Building a dataset

  - Download MNIST RAW data files.

   ```bash
    mkdir -p data && cd data
    wget http://yann.lecun.com/exdb/mnist/t10k-images-idx3-ubyte.gz
    wget http://yann.lecun.com/exdb/mnist/t10k-labels-idx1-ubyte.gz
    gzip -d *.gz
    cd ..
    ls -lah data/*
   ```

  - Write some code with Starwhale Python SDK. Full code is [here](https://github.com/star-whale/starwhale/blob/main/example/mnist/mnist/dataset.py).

   ```python
    import struct
    import typing as t
    from pathlib import Path
    from starwhale import BuildExecutor

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

  - Define `dataset.yaml`.

   ```yaml
    name: mnist
    handler: mnist.dataset:DatasetProcessExecutor
    attr:
      alignment_size: 1k
      volume_size: 4M
      data_mime_type: "x/grayscale"
   ```

  - Run one command to build the dataset.

   ```bash
    swcli dataset build .
    swcli dataset info mnist/version/latest
   ```

- 🍖 **STEP6**: Running an evaluation job

   ```bash
    swcli -vvv eval run --model mnist/version/latest --dataset mnist/version/latest --runtime pytorch/version/latest
    swcli eval list
    swcli eval info ${version}
   ```

👏 Now, you have completed the fundamental steps for Starwhale standalone.

Let's go ahead and finish the tutorial on the on-premises instance.

## MNIST Quick Tour for on-premises instance

![Create Job Workflow](docs/docs/img/console-create-job.gif)

- 🍰 **STEP1**: Install minikube and helm

  - [Minikube](https://minikube.sigs.k8s.io/docs/start/) 1.25+
  - [Helm](https://helm.sh/docs/intro/install/) 3.2.0+

- 🍵 **STEP2**: Start minikube

    ```bash
    minikube start
    ```

    > For users in the mainland of China, please add these startup parameters：`--image-mirror-country=cn --image-repository=registry.cn-hangzhou.aliyuncs.com/google_containers`. If there is no kubectl bin in your machine, you may use `minikube kubectl` or `alias kubectl="minikube kubectl --"` alias command.

- 🍵 **STEP3**: Installing Starwhale

    ```bash
    helm repo add starwhale https://star-whale.github.io/charts
    helm repo update
    helm install --devel my-starwhale starwhale/starwhale -n starwhale --create-namespace --set minikube.enabled=true
    ```

    After the installation is successful, the following prompt message appears:

    ```bash
    NAME: my-starwhale
    LAST DEPLOYED: Thu Jun 23 14:48:02 2022
    NAMESPACE: starwhale
    STATUS: deployed
    REVISION: 1
    NOTES:
    ******************************************
    Chart Name: starwhale
    Chart Version: 0.3.0
    App Version: 0.3.0
    ...

    Port Forward Visit:
    - starwhale controller:
        - run: kubectl port-forward --namespace starwhale svc/my-starwhale-controller 8082:8082
        - visit: http://localhost:8082
    - minio admin:
        - run: kubectl port-forward --namespace starwhale svc/my-starwhale-minio 9001:9001
        - visit: http://localhost:9001
    - mysql:
        - run: kubectl port-forward --namespace starwhale svc/my-starwhale-mysql 3306:3306
        - visit: mysql -h 127.0.0.1 -P 3306 -ustarwhale -pstarwhale

    ******************************************
    Login Info:
    - starwhale: u:starwhale, p:abcd1234
    - minio admin: u:minioadmin, p:minioadmin

    *_* Enjoy using Starwhale. *_*
    ```

    Then keep checking the minikube service status until all pods are running.

    ```bash
    kubectl get pods -n starwhale
    ```

    | NAME | READY | STATUS | RESTARTS | AGE |
    |:-----|-------|--------|----------|-----|
    |my-starwhale-controller-7d864558bc-vxvb8|1/1|Running|0|1m
    |my-starwhale-minio-7d45db75f6-7wq9b|1/1|Running|0|2m
    |my-starwhale-mysql-0|1/1|Running|0|2m

    Make the Starwhale controller accessible locally with the following command:

    ```bash
    kubectl port-forward --namespace starwhale svc/my-starwhale-controller 8082:8082
    ```

- ☕ **STEP4**: Upload the artifacts to the cloud instance

    > **pre-prepared artifacts**
    > Before starting this tutorial, the following three artifacts should already exist on your machine：
    >
    > - a starwhale model named mnist
    > - a starwhale dataset named mnist
    > - a starwhale runtime named pytorch
    >
    > The above three artifacts are what we built on our machine using starwhale.

    1. Use swcli to operate the remote server
        First, log in to the server:

        ```bash
        swcli instance login --username starwhale --password abcd1234 --alias dev http://localhost:8082
        ```

    2. Start copying the model, dataset, and runtime that we constructed earlier:

        ```bash
        swcli model copy mnist/version/latest dev/project/starwhale
        swcli dataset copy mnist/version/latest dev/project/starwhale
        swcli runtime copy pytorch/version/latest dev/project/starwhale
        ```

- 🍞 **STEP5**: Use the web UI to run an evaluation

    1. Log in Starwhale instance: let's use the username(starwhale) and password(abcd1234) to open the server web UI(<http://localhost:8082/>).

    2. Then, we will see the project named 'project_for_mnist' that we created earlier with swcli. Click the project name, you will see the model, runtime, and dataset uploaded in the previous step.
        <details>
            <summary>Show the uploaded artifacts screenshots</summary>

        ![console-artifacts.gif](../img/console-artifacts.gif)
        </details>
    3. Create and view an evaluation job
        <details>
            <summary>Show create job screenshot</summary>

        ![console-create-job.gif](../img/console-create-job.gif)
        </details>

**Congratulations! You have completed the evaluation process for a model.**

## Documentation, Community, and Support

- Visit [Starwhale HomePage](https://starwhale.ai).
- More information in the [official documentation](https://doc.starwhale.ai).
- For general questions and support, join the [Slack](https://starwhale.slack.com/).
- For bug reports and feature requests, please use [Github Issue](https://github.com/star-whale/starwhale/issues).
- To get community updates, follow [@starwhaleai](https://twitter.com/starwhaleai) on Twitter.
- For Starwhale artifacts, please visit:

  - Python Package on [Pypi](https://pypi.org/project/starwhale/).
  - Helm Charts on [Artifacthub](https://artifacthub.io/packages/helm/starwhale/starwhale).
  - Docker Images on [Docker Hub](https://hub.docker.com/u/starwhaleai) and [ghcr.io](https://github.com/orgs/star-whale/packages).

- Additionally, you can always find us at *developer@starwhale.ai*.

## Contributing

🌼👏**PRs are always welcomed** 👍🍺.

## License

Starwhale is licensed under the [Apache License 2.0](https://github.com/star-whale/starwhale/blob/main/LICENSE).
