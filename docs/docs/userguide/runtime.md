---
title: Starwhale Runtime User Guide
---

## Overview

Starwhale Runtime aims to provide a reproducible and sharable running environment for python programs. You can easily share your working environment with your teammates or outsiders, and vice versa. Furthermore, you can run your programs on Starwhale Server or Starwhale Cloud without bothering with the dependencies.

Starwhale works well with virtualenv, conda, and docker. If you are using one of them, it is straightforward to create a Starwhale Runtime based on your current environment.

Multiple Starwhale Runtimes on your local machine can be switched freely by one command. You can work on different projects without messing up the environment.

Starwhale Runtime consists of two parts: the base image and the dependencies.

### The base image

The base is a docker image with Python, CUDA, and cuDNN installed. Starwhale provides various base images for you to choose from; see the following list:

* Computer system architecture:
  * X86 (amd64)
  * Arm (aarch64)
* Operating system:
  * Ubuntu 20.04 LTS (ubuntu:20.04)
* Python:
  * 3.7
  * 3.8
  * 3.9
  * 3.10
  * 3.11
* CUDA
  * CUDA 11.3 + cuDNN 8.4
  * CUDA 11.4 + cuDNN 8.4
  * CUDA 11.5 + cuDNN 8.4
  * CUDA 11.6 + cuDNN 8.4

To choose the base image, see the [environment section of runtime.yaml](#yaml).

## Create a Starwhale Runtime

### Create from your current environment

### Create from runtime.yaml

## runtime.yaml {#yaml}

`runtime.yaml` is the core configuration file of Starwhale Runtime.

```yaml
# The name of Starwhale Runtime
name: demo
configs:
  # If you do not use conda, ignore this field.
  conda:
    # Conda channels to use when restoring the runtime
    channels:
      - conda-forge
  docker:
    # Use this field if you want to use your own customized runtime docker image.
    # All other fields in runtime.yaml are ignored when this field is set.
    image: ghcr.io/star-whale/runtime/pytorch
  pip:
    # pip config set global.index-url
    index_url: https://example.org/
    # pip config set global.extra-index-url
    extra_index_url: https://another.net/
    # pip config set install.trusted-host
    trusted_host:
      - example.org
      - another.net
environment:
  # Now it must be ubuntu:20.04
  os: ubuntu:20.04
  # CUDA version. possible values: 11.3, 11.4, 11.5, 11.6
  cuda: 11.4
  # Python version. possible values: 3.7, 3.8, 3.9, 3.10, 3.11
  python: 3.8
  # Use this field if you want to use your own customized base docker image
  docker:
    image: my.com/self/custom
dependencies:
  # If this item is present, conda env create -f conda.yml will be executed
  - conda.yaml
  # If this item is present, pip install -r requirements.txt will be executed before installing other pip packages
  - requirements.txt
  # Packages to be installed with pip. The format is the same as requirements.txt
  - pip:
      - pillow
      - numpy
      - scikit-learn
      - torchvision
      - torch
      - torchdata
      - torchtext
      - torchaudio
      - pycocotools
  # Additional wheels packages to be installed when restoring the runtime
  - wheels:
      - dummy-0.0.0-py3-none-any.whl
  # Additional files to be included in the runtime
  - files:
      - dest: bin/prepare.sh
        name: prepare
        src: scripts/prepare.sh
```
