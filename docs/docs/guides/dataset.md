---
title: Starwhale Dataset
---

## 1. Overview

### 1.1 Design Objective

Starwhale Dataset is a data management tool for the ML/DL domain with three core phases: dataset construction, dataset loading and dataset visualization. Starwhale Dataset can directly use the environment built by Starwhale Runtime which also can be seamlessly integrated by Starwhale Model and Starwhale Evaluation. Starwhale Dataset is an important part of the Starwhale MLOps toolchain.

According to the classification of MLOps Roles in [Machine Learning Operations (MLOps): Overview, Definition, and Architecture](https://arxiv.org/abs/2205.02302), the three stages of Starwhale Dataset are aimed at user groups as follows:

- Dataset construction: Data Engineer, Data Scientist
- Dataset loading: Data Scientist, ML Developer
- Dataset visualization: Data Engineer, Data Scientist, ML Developer

![mlops-users](../img/mlops-users.png)

### 1.2 Core Features

- **Efficient loading**: The original files of the dataset are stored on external remote storage such as OSS or NAS, and are loaded on demand without data dumping.
- **Simple construction**: Support swds-bin, user-raw and remote-link three data formats. By writing some simple Python code and dataset.yaml(optional), executing swcli command-line, then you will get a Starwhale Dataset.
- **Versioning**: Starwhale Dataset supports version tracking, data appending and other version management operations. Avoiding duplication of the data storage through the internal ObjectStore mechanism.
- **Distribution**: By the copy of swcli command, you can exchange Starwhale datasets between standalone instances and cloud instances.
- **Visualization**: The web ui of the Cloud Instance can provide multi-dimensional, multi-type data presentation of datasets.
- **Artifact Storage**: Standalone Instance stores swds files which are from locally built or remote distributed. Cloud Instance provides centralized swds storage with the Object Store infra.
- **Seamless Integration**: Starwhale Dataset can use Starwhale Runtime to build datasets. Starwhale Evaluation and Starwhale Model can automatic data loading through the `--dataset` parameter which is convenient for model inference and model evaluation.

### 1.3 Dataset Scenarios

According to the actual usage scenarios, there are three formats of Starwhale Dataset:

- swds-bin: swds-bin is a binary format implemented by Starwhale, which can merge a large number of small files into serval large files. swds-bin can be indexed, sliced and efficiently loaded.
- user-raw: user-raw format does not change the original data format, only establish index relationships and generate annotations to provide data abstraction. At the same time, for the convenient sharing, the original data files will be carried in the process of dataset distribution.
- remote-link: remote-link format is a further evolution of the user-raw format. The format satisfies the user's original data which are stored on the external storage such as OSS or NAS. 

### 1.4 Key Concepts

## 2. Best Practice

### 2.1 Command-Line Groups

### 2.2 Core Processes

## 3. dataset.yaml

### 3.1 YAML specification

### 3.2 Examples

#### 3.2.1 Minimalist Example

#### 3.2.2 MNIST Dataset Example

#### 3.2.3 Generator Example

## 4. Starwhale Dataset Viewer