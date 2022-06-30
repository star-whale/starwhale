---
title: Standalone Quickstart
---

## Installing Starwhale

Starwhale has three types of instances: Standalone, On-Premises, and Cloud Hosted. Starting with the standalone mode is ideal for quickly understanding and mastering Starwhale.
You install Starwhale Standalone by running:

```bash
python3 -m pip install --pre starwhale
```

:::note
The Starwhale client version is currently under alpha preview. Thus `--pre` is required.
:::
:::note
Starwhale standalone requires Python 3.7 or above. Currently, Starwhale only supports Linux and macOS X. Windows is coming soon.
:::

At the installation stage, we strongly recommend you follow the [doc](../standalone/installation.md).

## Downloading Examples

Download Starwhale examples by cloning Starwhale via:

```bash
git clone https://github.com/star-whale/starwhale.git
cd starwhale/example/mnist
```

We will use ML/DL HelloWorld code `MNIST` to start your Starwhale journey. The following steps are all performed in the `starwhale/example/mnist` directory.

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

- Build the Starwhale Runtime bundle.

  ```bash
  swcli runtime build .
  ```

- Check your local Starwhale Runtimes.

  ```bash
  swcli runtime list
  swcli runtime info pytorch-mnist/version/latest
  ```

## Building Model

- Build a Starwhale Model

  ```bash
  swcli model build .
  ```

- Check your local Starwhale Models.

  ```bash
  swcli model list
  swcli model info mnist/version/latest
  ```

## Building Dataset

- Download the MNIST raw data

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

- Check your local Starwhale Dataset.

  ```bash
  swcli dataset list
  swcli dataset info mnist/version/latest
  ```

## Running an Evaluation Job

Run an evaluation job in the currently activated python runtime.

- Create an evaluation job

 ```bash
 swcli -vvv job create --model mnist/version/latest --dataset mnist/version/latest
 ```

- Check the evaluation result

 ```bash
 swcli job list
 swcli job info ${version}
 ```

- [Optional Step] You can also create a job with docker.

 ```bash
 swcli -vvv job create --model mnist/version/latest --runtime pytorch-mnist/version/latest --dataset mnist/version/latest --use-docker
 ```

:::tip
The `job create` command will pull the runtime base image from ghcr.io by default. It may cost a lot of time for users in the mainland of China. `pip install` can be slow too.
:::

  👏 Now, you have completed the basic steps for Starwhale standalone.