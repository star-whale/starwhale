---
title: Getting started with Starwhale Standalone
---

When the [Starwhale Command Line Interface (SWCLI)](../swcli) is installed, you are ready to use Starwhale Standalone.

# 1. Downloading Examples

Download Starwhale examples by cloning the Starwhale project via:

```bash
git clone https://github.com/star-whale/starwhale.git
cd starwhale
```

We will use MNIST with PyTorch to start your Starwhale journey. The following steps are all performed in the `starwhale` directory.

![Core Workflow](../img/standalone-core-workflow.gif)

# 2. Building a Pytorch Runtime

Runtime example codes are in the `example/runtime/pytorch` directory.

- Build the Starwhale runtime bundle:

  ```bash
  swcli runtime build example/runtime/pytorch
  ```

- Check your local Starwhale runtimes:

  ```bash
  swcli runtime list
  swcli runtime info pytorch/version/latest
  ```

# 3. Building a Model

Model example codes are in the `example/mnist` directory.

- Download the pre-trained model file:

  ```bash
  mkdir -p example/mnist/models
  wget -O example/mnist/models/mnist_cnn.pt https://starwhale-examples.s3.us-west-1.amazonaws.com/mnist_cnn.pt
  
  # Users in the mainland of China may use the following URL to improve the download speed:
  # wget -O example/mnist/models/mnist_cnn.pt https://starwhale-examples.oss-cn-beijing.aliyuncs.com/mnist_cnn.pt
  ```

- Build a Starwhale model:

  ```bash
  swcli model build example/mnist
  ```

- Check your local Starwhale models.

  ```bash
  swcli model list
  swcli model info mnist/version/latest
  ```

# 4. Building a Dataset

Dataset example codes are in the `example/mnist` directory.

- Download the MNIST raw data:

  ```bash
  mkdir -p example/mnist/data
  wget -O example/mnist/data/t10k-images-idx3-ubyte.gz http://yann.lecun.com/exdb/mnist/t10k-images-idx3-ubyte.gz
  wget -O example/mnist/data/t10k-labels-idx1-ubyte.gz http://yann.lecun.com/exdb/mnist/t10k-labels-idx1-ubyte.gz
  gzip -d example/mnist/data/*.gz
  ls -lah example/mnist/data/*
  ```

- Build a Starwhale dataset:

  ```bash
  swcli dataset build example/mnist
  ```

- Check your local Starwhale dataset:

  ```bash
  swcli dataset list
  swcli dataset info mnist/version/latest
  ```

# 5. Running an Evaluation Job

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
When you use a new runtime in the eval run command, it may take a lot of time to download python dependencies. We recommend you set an appropriate PyPI mirror in the `~/.pip/pip.conf` file.
:::

**Congratulations! You have completed the Starwhale Standalone Getting Started Guide.**
