---
title: On-Premises Quickstart
---

:::tip
It is recommended to read [standalone quickstart](./standalone.md) first.
:::

## 1. Installing On-Premises

Starwhale provides two ways to install an On-Premises instance in your private cluster:

- For Kubernetes:

  - Standard Kubernetes Cluster: A pre-deployed Kubernetes cluster is required.
  - Minikube: You should have minikube and docker installed on your machine.
  - For more deployment details of Kubernetes, you can refer to this [doc](../guides/install/helm-charts.md).

:::notes
In this tutorial, minikube is used instead of the standard Kubernetes cluster
:::

### 1.1 Prerequisites

- [Minikube](https://minikube.sigs.k8s.io/docs/start/) 1.25+
- [Helm](https://helm.sh/docs/intro/install/) 3.2.0+

### 1.2 Start Minikube

```bash
minikube start
```

For users in the mainland of China, please add these startup parameters：`--image-mirror-country=cn --image-repository=registry.cn-hangzhou.aliyuncs.com/google_containers`. If there is no kubectl bin in your machine, you may use `minikube kubectl` or `alias kubectl="minikube kubectl --"` alias command.

### 1.3 Installing Starwhale

```bash
helm repo add starwhale https://star-whale.github.io/charts
helm repo update
helm install --devel my-starwhale starwhale/starwhale -n starwhale --create-namespace --set minikube.enabled=true
```

After the installation is successful, the following prompt message appears:

```bash
NAME: my-starwhale
LAST DEPLOYED: Thu Jun 23 14:48:02 2022
NAMESPACE: starwhale
STATUS: deployed
REVISION: 1
NOTES:
******************************************
Chart Name: starwhale
Chart Version: 0.3.0
App Version: 0.3.0
Starwhale Image:
  - server: ghcr.io/star-whale/server:0.3.0

******************************************
Web Visit:
  - starwhale controller: http://console.minikube.local
  - minio admin: http://minio.pre.intra.starwhale.ai

Port Forward Visist:
  - starwhale controller:
    - run: kubectl port-forward --namespace starwhale svc/my-starwhale-controller 8082:8082
    - visit: http://localhost:8082
  - minio admin:
    - run: kubectl port-forward --namespace starwhale svc/my-starwhale-minio 9001:9001
    - visit: http://localhost:9001
  - mysql:
    - run: kubectl port-forward --namespace starwhale svc/my-starwhale-mysql 3306:3306
    - visit: mysql -h 127.0.0.1 -P 3306 -ustarwhale -pstarwhale

******************************************
Login Info:
- starwhale: u:starwhale, p:abcd1234
- minio admin: u:minioadmin, p:minioadmin

*_* Enjoy using Starwhale. *_*
```

Then keep checking the minikube service status until all pods are running.

```bash
kubectl get pods -n starwhale
```

| NAME | READY | STATUS | RESTARTS | AGE |
|:-----|-------|--------|----------|-----|
|my-starwhale-controller-7d864558bc-vxvb8|1/1|Running|0|1m
|my-starwhale-minio-7d45db75f6-7wq9b|1/1|Running|0|2m
|my-starwhale-mysql-0|1/1|Running|0|2m

Make the Starwhale controller accessible locally with the following command:

```bash
kubectl port-forward --namespace starwhale svc/my-starwhale-controller 8082:8082
```

## 2. Upload the artifacts to the cloud instance

Before starting this tutorial, the following three artifacts should already exist on your machine：

- a starwhale model named mnist
- a starwhale dataset named mnist
- a starwhale runtime named pytorch

The above three artifacts are what we built in the [standalone tutorial](standalone.md).

### 2.1 Login Cloud Instance

First, log in to the server:

```bash
swcli instance login --username starwhale --password abcd1234 --alias dev http://localhost:8082
```

### 2.2 Release artifacts

Start copying the model, dataset, and runtime that we constructed earlier:

```bash
swcli model copy mnist/version/latest dev/project/starwhale
swcli dataset copy mnist/version/latest dev/project/starwhale
swcli runtime copy pytorch/version/latest dev/project/starwhale
```

## 3. Use the web UI to run an evaluation

### 3.1 Viewing Cloud Instance

Ok, let's use the username(starwhale) and password(abcd1234) to open the server [web UI](http://localhost:8082/).

![console-artifacts.gif](../img/console-artifacts.gif)

### 3.2 Create an evaluation job

![console-create-job.gif](../img/console-create-job.gif)

**Congratulations! You have completed the evaluation process for a model.**
