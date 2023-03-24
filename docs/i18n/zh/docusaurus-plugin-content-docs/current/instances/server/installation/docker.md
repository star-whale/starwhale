---
title: 使用Docker安装Starwhale Server
---

## 先决条件

* 1.19或者更高版本的Kubernetes集群用于执行任务。
* MySQL 8.0以上版本的数据库实例用于存储元数据。
* 兼容S3接口的对象存储，用于保存数据集、模型等。

请确保您的Kubernetes集群上的pod可以访问Starwhale Server侦听的端口。

## 为Docker准备env文件

Starwhale Server可以通过环境变量进行配置。

Docker的env文件模板参考[此处](../config/starwhale_env)。您可以通过修改模板来创建自己的env文件。

## 准备kubeconfig文件

kubeconfig文件用于访问Kubernetes集群。 有关kubeconfig文件的更多信息，请参阅[官方Kubernetes文档](https://kubernetes.io/docs/concepts/configuration/organize-cluster-access-kubeconfig/)。

如果您安装了`kubectl`命令行工具，可以运行`kubectl config view`来查看您当前的配置。

## 启动Docker镜像

```bash
docker run -it -d --name starwhale-server -p 8082:8082 \
        --restart unless-stopped \
        --mount type=bind,source=<您的kubeconfig文件路径>,destination=/root/.kube/config,readonly \
        --env-file <您的env文件路径> \
        ghcr.io/star-whale/server
