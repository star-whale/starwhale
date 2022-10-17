---
title: Standalone Quickstart
---

## 1. Installing Starwhale

Starwhale has three types of instances: Standalone, On-Premises, and Cloud Hosted. Starting with the standalone mode is ideal for quickly understanding and mastering Starwhale.
You install Starwhale Standalone by running:

```bash
python3 -m pip install --pre starwhale
```

:::note
The Starwhale client version is currently under alpha preview. Thus `--pre` is required.
:::

:::note
Starwhale standalone requires Python 3.7~3.10. Currently, Starwhale only supports Linux and macOS. Windows is coming soon.
:::

At the installation stage, we strongly recommend you follow the [doc](../guides/install/standalone.md).

## 2. Downloading Examples

Download Starwhale examples by cloning Starwhale via:

```bash
git clone https://github.com/star-whale/starwhale.git
cd starwhale
```

If [git-lfs](https://git-lfs.github.com/) has not been previously installed in the local environment(the command is `git lfs install`), you need to download the trained model file.

```bash
wget https://media.githubusercontent.com/media/star-whale/starwhale/main/example/mnist/models/mnist_cnn.pt -O example/mnist/models/mnist_cnn.pt
```

We will use ML/DL HelloWorld code `MNIST` to start your Starwhale journey. The following steps are all performed in the `starwhale` directory.

![Core Workflow](../img/standalone-core-workflow.gif)

## 3. Building Runtime

Runtime example code are in the `example/runtime/pytorch` directory.

- Build the Starwhale Runtime bundle:

  ```bash
  swcli runtime build .
  ```

- Check your local Starwhale Runtimes:

  ```bash
  swcli runtime list
  swcli runtime info pytorch/version/latest
  ```

## 4. Building Model

Model example code are in the `example/mnist` directory.

- Build a Starwhale Model:

  ```bash
  swcli model build .
  ```

- Check your local Starwhale Models.

  ```bash
  swcli model list
  swcli model info mnist/version/latest
  ```

## 5. Building Dataset

Dataset example code are in the `example/mnist` directory.

- Download the MNIST raw data:

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

- Build a Starwhale Dataset:

  ```bash
  swcli dataset build .
  ```

- Check your local Starwhale Dataset:

  ```bash
  swcli dataset list
  swcli dataset info mnist/version/latest
  ```

## 6. Running an Evaluation Job

- Create an evaluation job

 ```bash
 swcli -vvv eval run --model mnist/version/latest --dataset mnist/version/latest --runtime pytorch/version/latest
 ```

- Check the evaluation result

 ```bash
 swcli eval list
 swcli eval info ${version}
 ```

:::tip
When you first use Runtime in the eval run command which maybe use a lot of time to create isolated python environment, download python dependencies. Use the befitting pypi mirror in the `~/.pip/pip.conf` file is a recommend practice.
:::

  👏 Now, you have completed the basic steps for Starwhale standalone.
