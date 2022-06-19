---
title: Standalone Quickstart
---

## Installing Starwhale

Starwhale has three types of instances: Standalone, On-Premises, Cloud Hosted. Starting with standalone mode is ideal for understanding and mastering Starwhale.
You install Starwhale Standalone by running:

```bash
python3 -m pip install --pre starwhale
```

:::note
The Starwhale version is currently under alpha preview, thus `--pre` is required.
:::
:::note
Starwhale standalone requires Python 3.7 or above. Today starwhale only supports Linux platform. Windows and MacOS is coming soon.
:::

At the installation point, we recommended you follow the [doc](../standalone/installation.md).

## Downloading the Example

Download the starwhale example code by cloning Starwhale via:

```bash
git clone git@github.com:star-whale/starwhale.git
cd starwhale/example/mnist
```

We will use ML/DL HelloWorld code `MNIST` to start your starwhale journey. The following steps are all performed in the `starwhale/example/mnist` directory.

![Core Workflow](../img/core-workflow.gif)

## Building Runtime

- Create a python runtime with `Virtualenv` or `Conda`, then activate it.

  ```bash
  swcli runtime create -n pytorch-mnist -m venv --python 3.9 .
  source venv/bin/activate
  ```

- Install your python packages with `pip install`.

  ```bash
  python3 -m pip install -r requirements.txt
  ```

- Build Starwhale Runtime bundle.

  ```bash
  swcli runtime build .
  ```

- List and Info Local Starwhale Runtime.

  ```bash
  swcli runtime list
  swcli runtime info pytorch-mnist/version/latest
  ```

## Building Model

- Build a Starwhale Model

  ```bash
  swcli model build .
  ```

- List and Info Local Starwhale Model.

  ```bash
  swcli model list
  swcli model info mnist/version/latest
  ```

## Building Dataset

- Download MNIST raw data

  ```bash
  mkdir -p data && cd data
  wget http://yann.lecun.com/exdb/mnist/train-images-idx3-ubyte.gz
  wget http://yann.lecun.com/exdb/mnist/train-labels-idx1-ubyte.gz
  wget http://yann.lecun.com/exdb/mnist/t10k-images-idx3-ubyte.gz
  wget http://yann.lecun.com/exdb/mnist/t10k-labels-idx1-ubyte.gz
  gzip -d *.gz
  cd ..
  ls -lah data/*
  ```

- Build a Starwhale Dataset

  ```bash
  swcli dataset build .
  ```

- List and Info Local Starwhale Dataset.

  ```bash
  swcli dataset list
  swcli dataset info mnist/version/latest
  ```

## Running Evaluation Job

Run evaluation job in current activated python runtime.

- Create Evaluation Job

 ```bash
 swcli -vvv job create --model mnist/version/latest --dataset mnist/version/latest
 ```

- Info Evaluation Result

 ```bash
 swcli job list
 swcli job info ${version}
 ```

- [Optional Step]Additional, you can also create a job in docker environment.

 ```bash
 swcli -vvv job create --model mnist/version/latest --runtime pytorch-mnist/version/latest --dataset mnist/version/latest --use-docker
 ```

  :::tip Create job too slow
  Job create command will pull runtime base image from ghcr.io by default, it maybe costs a lot of time, meanwhile, the process of `pip install` is also not fast.
  :::

  👏 Now you have completed the basic step for starwhale standalone.
