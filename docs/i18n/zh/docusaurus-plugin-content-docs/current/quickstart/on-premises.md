---
title: Cloud 快速上手
---

:::tip
建议先阅读[Standalone快速入门](standalone.md)。
:::

## 1. 安装私有化版本的Starwhale Cloud服务

Starwhale Cloud 有两种形态，一种是私有化到用户独立集群的On-Premises版本，另一种是Starwhale托管的Hosted-SaaS版本。Starwhale Cloud 是面向云原生的，完全由Kubernetes来托管，既支持标准Kubernetes集群，又支持MiniKube这种开发调试用的单机Kubernetes服务。本文介绍如何在单机环境下，使用MiniKube快速安装On-Premises的Starwhale Cloud Instance，并体验模型评测全流程。

### 1.1 前置条件

- [Minikube](https://minikube.sigs.k8s.io/docs/start/) 1.25+
- [Helm](https://helm.sh/docs/intro/install/) 3.2.0+

### 1.2 启动Minikube

```bash
minikube start
```

对于中国大陆的网络环境，可以在minikube start命令中增加 `--image-mirror-country=cn --image-repository=registry.cn-hangzhou.aliyuncs.com/google_containers` 参数来提升镜像下载速度。另外如果本机没有 `kubectl` 命令，可以使用 `minikube kubectl` 代替，也可以采用 `alias kubectl="minikube kubectl --"` 命令，在当前终端中提供 `kubectl` 命令的alias。

### 1.3 使用Helm安装Starwhale Cloud

```bash
helm repo add starwhale https://star-whale.github.io/charts
helm repo update
helm install --devel my-starwhale starwhale/starwhale -n starwhale --create-namespace --set minikube.enabled=true
```

更详细的Helm Charts参数配置，请参考[使用Helm安装Cloud Instance](../guides/install/helm-charts.md)文档。当成功安装后，会有类似如下信息输出：

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

可以检查starwhale namespace下的Pod是否都运行起来，正常情况会产生类似如下的输出：

```bash
kubectl get pods -n starwhale
```

| NAME | READY | STATUS | RESTARTS | AGE |
|:-----|-------|--------|----------|-----|
|my-starwhale-controller-7d864558bc-vxvb8|1/1|Running|0|1m
|my-starwhale-minio-7d45db75f6-7wq9b|1/1|Running|0|2m
|my-starwhale-mysql-0|1/1|Running|0|2m

可以使用kubectl的port-forward命令，在宿主机浏览器直接通过8082端口访问Starwhale Controller Web：

```bash
kubectl port-forward --namespace starwhale svc/my-starwhale-controller 8082:8082
```

## 2. 发布Model/Runtime/Dataset到Cloud Instance上

我们使用[Standalone 快速上手](standalone.md)文档中构建出来的Pytorch的Starwhale Runtime，MNIST的Starwhale Model和Starwhale Dataset 作为基础制品，完成在Cloud Instance上的评测任务。

### 2.1 登陆Cloud Instance

登陆地址为 `http://localhost:8082` 的Cloud Instance，并将其命名为dev。

```bash
swcli instance login --username starwhale --password abcd1234 --alias dev http://localhost:8082
```

### 2.2 发布制品

Starwhale Cloud Instance首次启动后，会默认创建一个名称为 `starwhale` 的project。

```bash
swcli model copy mnist/version/latest dev/project/starwhale
swcli dataset copy mnist/version/latest dev/project/starwhale
swcli runtime copy pytorch/version/latest dev/project/starwhale
```

## 3. 使用Starwhale Controller Web UI进行模型评测

### 3.1 在Cloud Instance上查看制品

在Web浏览器中打开 http://localhost:8082 地址，使用默认用户名(starwhale)和密码(abcd1234)登陆。进入 `starwhale` project中，可以查看发布的Runtime、Dataset和Model。

![console-artifacts.gif](../img/console-artifacts.gif)

### 3.2 创建模型评测任务

![console-create-job.gif](../img/console-create-job.gif)

👏 恭喜，目前已经完成了Starwhale Cloud的基本操作任务。
