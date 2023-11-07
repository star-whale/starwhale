---
title: CNN on CIFAR-10 model evaluation
---


## Prepare model

### Download pre-trained model

```bash
make download-model
```

### Train CNN model

We will use Starwhale Runtime to train model, so you should build `image-classification` runtime in advance.

```bash
make train
```

## Build Starwhale Model Package

The following command will load `model.yaml` in the current dir to build the Starwhale Model.

```bash
swcli model build .
```

## Run Evaluation in Starwhale Standalone

```bash
# use source code
swcli model run -w . --dataset cifar10 --runtime image-classification

# use starwhale model uri
swcli model run -u cnn --dataset cifar10 --runtime image-classification
```

## Run Online Evaluation in Starwhale Standalone

```bash
# use source code in the current shell environment
swcli model serve -w .

# use starwhale model uri in the starwhale runtime environment
swcli model server -u cnn --runtime image-classification
```
