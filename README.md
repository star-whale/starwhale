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
  - 🚝 **Model Training(WIP)**: Use Starwhale Python SDK to record experiment meta, metric, log, and artifact.
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
    python3 -m pip install starwhale
    ```

- 🍵 **STEP2**: Downloading the MNIST example

    > To save time in the example downloading, we skip git-lfs and other commits info.

    ```bash
    GIT_LFS_SKIP_SMUDGE=1 git clone https://github.com/star-whale/starwhale.git --depth 1
    cd starwhale
    ```

- ☕ **STEP3**: Building a runtime

    > When you first build runtime, creating an isolated python environment and downloading python dependencies will take a lot of time. The command execution time is related to the network environment of the machine and the number of packages in the runtime.yaml. Using the befitting pypi mirror and cache config in the `~/.pip/pip.conf` file is a recommended practice.
    >
    > For users in the mainland of China, the following conf file is an option:
    >
    > ```conf
    > [global]
    > cache-dir = ~/.cache/pip
    > index-url = https://mirrors.aliyun.com/pypi/simple/
    > extra-index-url = https://pypi.doubanio.com/simple
    > ```

    ```bash
    swcli runtime build example/runtime/pytorch
    swcli runtime list
    swcli runtime info pytorch/version/latest
    ```

- 🍞 **STEP4**: Building a model

  - Download pre-trained model file:

    ```bash
    cd example/mnist
    make download-model
    # For users in the mainland of China, please add `CN=1` environment for make command:
    # CN=1 make download-model
    cd -
    ```

  - [Code Example]Write some code with Starwhale Python SDK. Complete code is [here](https://github.com/star-whale/starwhale/blob/main/example/mnist/mnist/evaluator.py).

    ```python
    import typing as t
    import torch
    from starwhale import Image, PipelineHandler, multi_classification

    class MNISTInference(PipelineHandler):
            def __init__(self) -> None:
                super().__init__()
                self.device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
                self.model = self._load_model(self.device)

            def ppl(self, data: t.Dict[str, t.Any], **kw: t.Any) -> t.Tuple[float, t.List[float]]:
                data_tensor = self._pre(data["img"])
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
                self, ppl_result
            ) -> t.Tuple[t.List[int], t.List[int], t.List[t.List[float]]]:
                result, label, pr = [], [], []
                for _data in ppl_result:
                    label.append(_data["ds_data"]["label"])
                    result.append(_data["result"][0])
                    pr.append(_data["result"][1])
                return label, result, pr

        def _pre(self, input:bytes):
            """write some mnist preprocessing code"""

        def _post(self, input:bytes):
            """write some mnist post-processing code"""

        def _load_model():
            """load your pre trained model"""
    ```

  - [Code Example]Define `model.yaml`.

    ```yaml
    name: mnist
    run:
        handler: mnist.evaluator:MNISTInference
    ```

  - Run one command to build the model.

    ```bash
    swcli model build example/mnist --runtime pytorch/version/latest
    swcli model list
    swcli model info mnist/version/latest
    ```

- 🍺 **STEP5**: Building a dataset

  - Download MNIST RAW data files.

    ```bash
    cd example/mnist
    make download-data
    # For users in the mainland of China, please add `CN=1` environment for make command:
    # CN=1 make download-data
    cd -
    ```

  - [Code Example]Write some code with Starwhale Python SDK. Full code is [here](https://github.com/star-whale/starwhale/blob/main/example/mnist/mnist/dataset.py).

    ```python
    import struct
    from pathlib import Path
    from starwhale import GrayscaleImage

    def iter_swds_bin_item():
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
                yield {
                    "img": GrayscaleImage(
                        _data,
                        display_name=f"{i}",
                        shape=(height, width, 1),
                    ),
                    "label": _label,
                }
    ```

  - Run one command to build the dataset.

    ```bash
    swcli dataset build example/mnist --handler mnist.dataset:iter_swds_bin_item --runtime pytorch/version/latest
    swcli dataset info mnist/version/latest
    swcli dataset head mnist/version/latest
    ```

  Starwhale also supports build dataset with pure python sdk. You can try it in [Google Colab](https://colab.research.google.com/github/star-whale/starwhale/blob/main/example/notebooks/dataset-sdk.ipynb).

- 🍖 **STEP6**: Running an evaluation job

    ```bash
    swcli eval run --model mnist/version/latest --dataset mnist/version/latest --runtime pytorch/version/latest
    swcli eval list
    swcli eval info $(swcli eval list | grep mnist | grep success | awk '{print $1}' | head -n 1)
    ```

**👏 Now, you have completed the fundamental steps for Starwhale standalone. Let's go ahead and finish the tutorial on the on-premises instance.**

## MNIST Quick Tour for on-premises instance

![Create Job Workflow](docs/docs/img/console-create-job.gif)

- 🍰 **STEP1**: Install minikube and helm

  - [Minikube](https://minikube.sigs.k8s.io/docs/start/) 1.25+
  - [Helm](https://helm.sh/docs/intro/install/) 3.2.0+

- 🍵 **STEP2**: Start minikube

    ```bash
    minikube start
    ```

    > For users in the mainland of China, please add some external parameters. The following command was well tested; you may also try another kubernetes version.
    >
    > ```bash
    > minikube start --image-mirror-country=cn --kubernetes-version=1.25.3
    > ```

    If there is no kubectl bin in your machine, you may use `minikube kubectl` or `alias kubectl="minikube kubectl --"` alias command.

- 🍵 **STEP3**: Installing Starwhale

    ```bash
    helm repo add starwhale https://star-whale.github.io/charts
    helm repo update
    helm pull starwhale/starwhale --untar --untardir ./charts

    helm upgrade --install starwhale ./charts/starwhale -n starwhale --create-namespace -f ./charts/starwhale/values.minikube.global.yaml
    ```

    > For users in the mainland of China, use `values.minikube.global.yaml`.

    ```bash
    helm upgrade --install starwhale ./charts/starwhale -n starwhale --create-namespace -f ./charts/starwhale/values.minikube.cn.yaml
    ```

    After the installation is successful, the following prompt message appears:

    ```bash
    Release "starwhale" has been upgraded. Happy Helming!
    NAME: starwhale
    LAST DEPLOYED: Tue Feb 14 16:25:03 2023
    NAMESPACE: starwhale
    STATUS: deployed
    REVISION: 14
    NOTES:
    ******************************************
    Chart Name: starwhale
    Chart Version: 0.1.0
    App Version: latest
    Starwhale Image:
        - server: ghcr.io/star-whale/server:latest
    Runtime default Image:
    - runtime image: ghcr.io/star-whale/starwhale:latest

    ******************************************
    Web Visit:

    Port Forward Visit:
    - starwhale controller:
        - run: kubectl port-forward --namespace starwhale svc/controller 8082:8082
        - visit: http://localhost:8082
    - minio admin:
        - run: kubectl port-forward --namespace starwhale svc/minio 9001:9001
        - visit: http://localhost:9001
    - mysql:
        - run: kubectl port-forward --namespace starwhale svc/mysql 3306:3306
        - visit: mysql -h 127.0.0.1 -P 3306 -ustarwhale -pstarwhale

    ******************************************
    Login Info:
    - starwhale: u:starwhale, p:abcd1234
    - minio admin: u:minioadmin, p:minioadmin

    *_* Enjoy to use Starwhale Platform. *_*
    ```

    Then keep checking the minikube service status until all deployments are running.

    ```bash
    kubectl get deployments -n starwhale
    ```

    | NAME | READY | UP-TO-DATE| AVAILABLE | AGE |
    |------|-------|--------|----------|-----|
    |controller|1/1|1|1|5m|
    |minio|1/1|1|1|5m|
    |mysql|1/1|1|1|5m|

    Make the Starwhale controller accessible locally with the following command:

    ```bash
    kubectl port-forward --namespace starwhale svc/controller 8082:8082
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

        ![Console Artifacts](docs/docs/img/console-artifacts.gif)
        </details>
    3. Create and view an evaluation job
        <details>
            <summary>Show create job screenshot</summary>

        ![Console Create Job](docs/docs/img/console-create-job.gif)
        </details>

**👏 Congratulations! You have completed the evaluation process for a model.**

## Documentation, Community, and Support

- Visit [Starwhale HomePage](https://starwhale.ai).
- More information in the [official documentation](https://doc.starwhale.ai).
- For general questions and support, join the [Slack](https://starwhale.slack.com/).
- For bug reports and feature requests, please use [Github Issue](https://github.com/star-whale/starwhale/issues).
- To get community updates, follow [@starwhaleai](https://twitter.com/starwhaleai) on Twitter.
- For Starwhale artifacts, please visit:

  - Python Package on [Pypi](https://pypi.org/project/starwhale/).
  - Helm Charts on [Artifacthub](https://artifacthub.io/packages/helm/starwhale/starwhale).
  - Docker Images on [Docker Hub](https://hub.docker.com/u/starwhaleai), [Github Packages](https://github.com/orgs/star-whale/packages) and [Starwhale Registry](https://docker-registry.starwhale.cn/).

- Additionally, you can always find us at *developer@starwhale.ai*.

## Contributing

🌼👏**PRs are always welcomed** 👍🍺. See [Contribution to Starwhale](https://doc.starwhale.ai/docs/community/contribute) for more details.

## License

Starwhale is licensed under the [Apache License 2.0](https://github.com/star-whale/starwhale/blob/main/LICENSE).
