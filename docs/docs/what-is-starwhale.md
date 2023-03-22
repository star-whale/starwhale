---
title: What is Starwhale
---

# Overview

Starwhale is an MLOps platform that make your model creation, evaluation and publication much eaiser. It aims to create a handy tool for data scientists and machine learning engineers.

Starwhale helps you

* Keep track of your training/testing data history including data items and their labels, so that you can easily access them.
* Manage your model packages that you can share across your team.
* Run your models in different environments, either on a Nvidia GPU server or on an embeded device like Cherry Pi.
* Create a online service with interactive Web UI for your models.

Starwhale is designed to be an open platform. You can create your own plugins to meet your requirements.

# Deployment options

Each deployment of Starwhale is called an instance. All instances can be managed by the Starwhale Command Line Interface (SWCLI).

You can start using Starwhale with one of the following instance types:

* **Starwhale Standalone** - Rather than a running service, Starwhale Standalone is actually a repository that resides in your local file system. It is created and managed by the Starwhale Command Line Interface (SWCLI). You only need to install SWCLI to use it. Currently, each user on a single machine can have only ONE Starwhale Standalone instance. We recommend you use the Starwhale Standalone to build and test your datasets, runtime, and models before pushing them to Starwhale Server/Cloud instances.
* **Starwhale Server** - Starwhale Server is a service deployed on your local server. Besides text-only results from the Starwhale Command Line Interface (SWCLI), Starwhale Server provides Web UI for you to manage your datasets and models, evaluate your models in your local Kubernetes cluster, and review the evaluation results.
* **Starwhale Cloud** - Starwhale Cloud is a managed service hosted on public clouds. By registering an account on <https://cloud.starwhale.ai> or <https://cloud.starwhale.cn>, you are ready to use Starwhale without needing to install, operate, and maintain your own instances. Starwhale Cloud also provides public resources for you to download, like datasets, runtimes, and models. Check the "starwhale/public" project on Starwhale Cloud for more details.

When choosing which instance type to use, consider the following:

| Instance Type | Deployment location | Maintained by | User Interface | Scalability |
| ------------- | ------------- |  ------------- |  ------------- | ------------- |
| Starwhale Standalone | Your laptop or any server in your data center | Not required | Command line | Not scalable |
| Starwhale Server | Your data center | Yourself | Web UI and command line | Scalable, depends on your Kubernetes cluster |
| Starwhale Cloud | Public cloud, like AWS or Aliyun | the Starwhale Team  |Web UI and command line | Scalable, but currently limited by the freely available resource on the cloud |
