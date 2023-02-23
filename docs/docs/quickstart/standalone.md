---
title: Standalone Quickstart
---

**This tutorial is also available as a [Colab Notebook](https://colab.research.google.com/github/star-whale/starwhale/blob/main/example/notebooks/quickstart-standalone.ipynb).**

![Core Workflow](../img/standalone-core-workflow.gif)

## 1. Installing Starwhale

Starwhale has three types of instances: Standalone, On-Premises, and Cloud Hosted. Starting with the standalone mode is ideal for quickly understanding and mastering Starwhale.
You install Starwhale Standalone by running:

```bash
python3 -m pip install starwhale
```

:::note
You can install the alpha version by the `--pre` argument.
:::

:::note
Starwhale standalone requires Python 3.7~3.11. Currently, Starwhale only supports Linux and macOS. Windows is coming soon.
:::

At the installation stage, we strongly recommend you follow the [doc](../guides/install/standalone.md).

## 2. Downloading Examples

Download Starwhale examples by cloning Starwhale via:

```bash
GIT_LFS_SKIP_SMUDGE=1 git clone https://github.com/star-whale/starwhale.git --depth 1
cd starwhale
```

To save time in the example downloading, we skip git-lfs and other commits info. We will use ML/DL HelloWorld code `MNIST` to start your Starwhale journey. The following steps are all performed in the `starwhale` directory.

## 3. Building Runtime

Runtime example code are in the `example/runtime/pytorch` directory.

- Build the Starwhale Runtime bundle:.

  :::tip
  When you first build runtime, creating an isolated python environment and downloading python dependencies will take a lot of time. The command execution time is related to the network environment of the machine and the number of packages in the runtime.yaml. Using the befitting pypi mirror and cache config in the `~/.pip/pip.conf` file is a recommended practice.

  For users in the mainland of China, the following conf file is an option:

    ```conf
    [global]
    cache-dir = ~/.cache/pip
    index-url = https://mirrors.aliyun.com/pypi/simple/
    extra-index-url = https://pypi.doubanio.com/simple
    ```

  :::

  ```bash
  swcli runtime build example/runtime/pytorch
  ```

- Check your local Starwhale Runtime:

  ```bash
  swcli runtime list
  swcli runtime info pytorch/version/latest
  ```

- Restore Starwhale Runtime(optional):

  ```bash
  swcli runtime restore pytorch/version/latest
  ```

## 4. Building Model

Model example code are in the `example/mnist` directory.

- Download pre-trained model file:

  ```bash
  cd example/mnist
  make download-model
  # For users in the mainland of China, please add `CN=1` environment for make command:
  # CN=1 make download-model
  cd -
  ```

- Build a Starwhale Model with prebuilt Starwhale Runtime:

  ```bash
  swcli model build example/mnist --runtime pytorch/version/latest
  ```

- Check your local Starwhale Model:

  ```bash
  swcli model list
  swcli model info mnist/version/latest
  ```

## 5. Building Dataset

Dataset example code are in the `example/mnist` directory.

- Download the MNIST raw data:

  ```bash
  cd example/mnist
  make download-data
  # For users in the mainland of China, please add `CN=1` environment for make command:
  # CN=1 make download-data
  cd -
  ```

- Build a Starwhale Dataset with prebuilt Starwhale Runtime:

  ```bash
  swcli dataset build example/mnist --runtime pytorch/version/latest
  ```

- Check your local Starwhale Dataset:

  ```bash
  swcli dataset list
  swcli dataset info mnist/version/latest
  ```

- Head some records from the mnist dataset:

  ```bash
  swcli dataset head mnist/version/latest
  ```

## 6. Running an Evaluation Job

- Create an evaluation job

  ```bash
  swcli eval run --model mnist/version/latest --dataset mnist/version/latest --runtime pytorch/version/latest
  ```

- Check the evaluation result:

 ```bash
 swcli eval list
 swcli eval info $(swcli eval list | grep mnist | grep success | awk '{print $1}' | head -n 1)
 ```

  👏 Now, you have completed the basic steps for Starwhale standalone.
