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

Starwhale is a MLOps platform. It provides **Instance**, **Project**, **Runtime**, **Model** and **Dataset** core concepts.

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
    git clone https://github.com/star-whale/starwhale.git
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
               _label.extend([int(l) for l in _data[self._label_field]])
               # unpack data according to the return value of function ppl
               (pred, pr) = _data[self._ppl_data_field]
               _result.extend([int(l) for l in pred])
               _pr.extend([l for l in pr])

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

Let's go ahead and finish tutorial on the on-premises instance.

## MNIST Quick Tour for on-premises instance
![Create Job Workflow](docs/docs/img/create-job-workflow.gif)

🍰 **STEP1**: installing minikube and helm
- Minikube 1.25+
- Helm 3.2.0+

There are standard installation tutorials for [minikube](https://minikube.sigs.k8s.io/docs/start/) and [helm](https://helm.sh/docs/intro/install/) here.


🍵 **STEP2**: Start minikube
```bash
minikube start
alias kubectl="minikube kubectl --"
```
For users in the mainland of China, please add these startup parameters：--image-mirror-country='cn' --image-repository=registry.cn-hangzhou.aliyuncs.com/google_containers

**Installation process**

```bash
helm repo add starwhale https://star-whale.github.io/charts
helm repo update
helm install my-starwhale starwhale/starwhale --version 0.2.0 -n starwhale --create-namespace --set minikube.enabled=true
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
Chart Version: 0.2.0
App Version: 0.2.0
...

Port Forward Visist:
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
watch -n 1 kubectl get pods -n starwhale
```

| NAME | READY | STATUS | RESTARTS | AGE |
|:-----|-------|--------|----------|-----|
|my-starwhale-agent-cpu-64699|2/2|Running|0|1m
|my-starwhale-controller-7d864558bc-vxvb8|1/1|Running|0|1m
|my-starwhale-minio-7d45db75f6-7wq9b|1/1|Running|0|2m
|my-starwhale-mysql-0|1/1|Running|0|2m

Make the Starwhale controller accessible locally with the following command:
```bash
kubectl port-forward --namespace starwhale svc/my-starwhale-controller 8082:8082
```

☕ **STEP3**: Upload resources to the server
> **pre-prepared resources**
> Before starting this tutorial, the following three resources should already exist on your machine：
> - a starwhale model named mnist
> - a starwhale dataset named mnist
> - a starwhale runtime named pytorch-mnist
> 
> The above three resources are what we built on our machine using starwhale.
1. Use swcli to operate the remote server
    First, log in to the server:
    ```bash
    swcli instance login --username starwhale --password abcd1234 --alias server http://localhost:8082
    ```

    Use the server instance as the default:
    ```bash
    swcli instance select server
    ```

2. Create a project named 'project_for_mnist' and make it default:

    ```bash
    swcli project create project_for_mnist
    swcli project select project_for_mnist
    ```

3. Start copying the model, dataset, and runtime that we constructed earlier:

    ```bash
    swcli model copy local/project/self/model/mnist/version/latest http://localhost:8082/
    swcli dataset copy local/project/self/dataset/mnist/version/latest http://localhost:8082/
    swcli runtime copy local/project/self/runtime/pytorch-mnist/version/latest http://localhost:8082/
    ```
🍞 **STEP4**: Use the web UI to run an evaluation
1. login
Ok, let's use the username(starwhale) and password(abcd1234) to open the server web UI(http://localhost:8082/). 

2. Then, we will see the project named 'project_for_mnist' that we created earlier with swcli.
![project list](docs/docs/img/ui-list-project.jpg)
Click the project name, you will see the model, runtime, and dataset uploaded in the previous step.
![model list](docs/docs/img/ui-list-model.jpg)
![dataset list](docs/docs/img/ui-list-dataset.jpg)
![runtime list](docs/docs/img/ui-list-runtime.jpg)
1. Create an evaluation job
![Create job](docs/docs/img/ui-create-job.jpg)

4. The job is completed and the results can be viewed
![Show Job Results](docs/docs/img/ui-job-results.jpg)

**Congratulations! You have completed the evaluation process for a model.**

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

🌼👏**PRs are always welcomed** 👍🍺.

## License

Starwhale is Licensed under the [Apache License 2.0](https://github.com/star-whale/starwhale/blob/main/LICENSE).
