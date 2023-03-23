---
title: 快速入门
---

:::tip
建议先阅读[Standalone快速入门](../standalone/getting_started.md)。
:::

## 安装私有化版本的Starwhale Cloud服务

Starwhale Cloud 有两种形态，一种是私有化到用户独立集群的On-Premises版本，另一种是Starwhale托管的Hosted-SaaS版本。Starwhale Cloud 是面向云原生的，完全由Kubernetes来托管，既支持标准Kubernetes集群，又支持MiniKube这种开发调试用的单机Kubernetes服务。本文介绍如何在单机环境下，使用MiniKube快速安装On-Premises的Starwhale Cloud Instance，并体验模型评测全流程。

### 前置条件

- [Minikube](https://minikube.sigs.k8s.io/docs/start/) 1.25+
- [Helm](https://helm.sh/docs/intro/install/) 3.2.0+

### 启动Minikube

```bash
minikube start --image-mirror-country=cn --kubernetes-version=1.25.3
```

上面命令中使用 `--kubernetes-version=1.25.3` 参数来固定安装版本，该版本是经过测试确保cn mirror中已经存在的，用户也可以使用尝试其他版本。对于非中国大陆网络环境，可以去掉 `--image-mirror-country=cn --kubernetes-version=1.25.3` 参数，直接使用 `minikube start` 命令即可。另外如果本机没有 `kubectl` 命令，可以使用 `minikube kubectl` 代替，也可以采用 `alias kubectl="minikube kubectl --"` 命令，在当前终端中提供 `kubectl` 命令的alias。

### 使用Helm安装Starwhale Cloud

```bash
helm repo add starwhale https://star-whale.github.io/charts
helm repo update
helm pull starwhale/starwhale --untar --untardir ./charts

helm upgrade --install starwhale ./charts/starwhale -n starwhale --create-namespace -f ./charts/starwhale/values.minikube.cn.yaml
```

对于非中国大陆网络环境，可以使用如下upgrade命令：

```bash
helm upgrade --install starwhale ./charts/starwhale -n starwhale --create-namespace -f ./charts/starwhale/values.minikube.global.yaml
```

更详细的Helm Charts参数配置，请参考[使用Helm安装Cloud Instance](install/helm-charts.md)文档。当成功安装后，会有类似如下信息输出：

```bash
Release "starwhale" has been upgraded. Happy Helming!
NAME: starwhale
LAST DEPLOYED: Tue Feb 14 16:25:03 2023
NAMESPACE: starwhale
STATUS: deployed
REVISION: 14
NOTES:
******************************************
Chart Name: starwhale
Chart Version: 0.1.0
App Version: latest
Starwhale Image:
    - server: docker-registry.starwhale.cn/star-whale/server:latest
Runtime default Image:
  - runtime image: docker-registry.starwhale.cn/star-whale/starwhale:latest

******************************************
Web Visit:

Port Forward Visit:
  - starwhale controller:
    - run: kubectl port-forward --namespace starwhale svc/controller 8082:8082
    - visit: http://localhost:8082
  - minio admin:
    - run: kubectl port-forward --namespace starwhale svc/minio 9001:9001
    - visit: http://localhost:9001
  - mysql:
    - run: kubectl port-forward --namespace starwhale svc/mysql 3306:3306
    - visit: mysql -h 127.0.0.1 -P 3306 -ustarwhale -pstarwhale

******************************************
Login Info:
- starwhale: u:starwhale, p:abcd1234
- minio admin: u:minioadmin, p:minioadmin

*_* Enjoy to use Starwhale Platform. *_*
```

可以检查starwhale namespace下的Deployments是否都运行起来，正常情况会产生类似如下的输出：

```bash
kubectl get deployments -n starwhale
```

| NAME | READY | UP-TO-DATE| AVAILABLE | AGE |
|------|-------|--------|----------|-----|
|controller|1/1|1|1|5m|
|minio|1/1|1|1|5m|
|mysql|1/1|1|1|5m|

可以使用kubectl的port-forward命令，在宿主机浏览器直接通过8082端口访问Starwhale Controller Web：

```bash
kubectl port-forward --namespace starwhale svc/controller 8082:8082
```

需要注意，当controller的pod重启时，需要重新执行port-forward命令做端口转发。

## 发布Model/Runtime/Dataset到Cloud Instance上

我们使用[Standalone 快速上手](../standalone/getting_started.md)文档中构建出来的Pytorch的Starwhale Runtime，MNIST的Starwhale Model和Starwhale Dataset 作为基础制品，完成在Cloud Instance上的评测任务。

### 登陆Cloud Instance

登陆地址为 `http://localhost:8082` 的Cloud Instance，并将其命名为dev。

```bash
swcli instance login --username starwhale --password abcd1234 --alias dev http://localhost:8082
```

### 发布制品

Starwhale Cloud Instance首次启动后，会默认创建一个名称为 `starwhale` 的project。

```bash
swcli model copy mnist/version/latest dev/project/starwhale
swcli dataset copy mnist/version/latest dev/project/starwhale
swcli runtime copy pytorch/version/latest dev/project/starwhale
```

## 使用Starwhale Controller Web UI进行模型评测

### 在Cloud Instance上查看制品

在Web浏览器中打开 [http://localhost:8082](http://localhost:8082) 地址，使用默认用户名(starwhale)和密码(abcd1234)登陆。进入 `starwhale` project中，可以查看发布的Runtime、Dataset和Model。

![console-artifacts.gif](../../img/console-artifacts.gif)

### 创建模型评测任务

![console-create-job.gif](../../img/console-create-job.gif)

👏 恭喜，目前已经完成了Starwhale Cloud的基本操作任务。
