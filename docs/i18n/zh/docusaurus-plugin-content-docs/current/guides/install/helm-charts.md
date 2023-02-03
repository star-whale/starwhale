---
title: 使用Helm安装Cloud Instance
---

## 1. Helm Charts

使用Helm Charts可以非常容易的将Starwhale Cloud Instance在Kubernetes集群中进行安装，包括Starwhale Controller和第三方基础依赖（mysql/minio等）。

## 2. 核心命令

```bash
helm repo add starwhale https://star-whale.github.io/charts
helm repo update
helm install starwhale starwhale/starwhale -n starwhale --create-namespace
```

## 3. 前置条件

- Kubernetes 1.19+
- Helm 3.2.0+

## 4. 安装Chart

```bash
helm repo add starwhale https://star-whale.github.io/charts
helm install starwhale starwhale/starwhale
```

## 5. 卸载Chart

```bash
helm delete starwhale
```

## 6. 配置参数说明

### 6.1 基本参数

|字段|描述|默认值|
|---|---|-----|
| `image.registry` | 镜像Registry, Starwhale镜像会发布在 docker.io, ghcr.io和docker-registry.starwhale.cn上，中国大陆网络推荐使用docker-registry.starwhale.cn镜像源 | `ghcr.io`|
| `image.org`      | 镜像的org名字： [starwhaleai](https://hub.docker.com/u/starwhaleai)(docker.io)、[star-whale](https://github.com/orgs/star-whale)(ghcr.io和docker-registry.starwhale.cn) 或者其他在私有registry上定义的镜像org名字 | `star-whale`  |

### 6.2 Starwhale参数

|字段|描述|默认值|
|---|---|-----|
| `mirror.pypi.enabled`       | pypi 镜像源设置    | `true` |
| `ingress.enabled`           | 使用Ingress    | `true` |
| `ingress.ingressClassName`  | ingress class name  | `nginx`                          |
| `ingress.host`              | Starwhale Controller 的访问域名  | `console.pre.intra.starwhale.ai` |

### 6.3 基础设施参数

|字段|描述|默认值|
|---|---|-----|
| `mysql.enabled` | 部署一个单例的MySQL服务，需要为MySQL提供一个PV持久化数据 | `true` |
| `mysql.persistence.storageClass` | mysql pvc storageClass | `local-storage-mysql` |
| `externalMySQL.host` | 当 mysql.enabled 设置为 false, 使用外部的MySQL host | `localhost` |
| `externalMySQL.port` | 使用外部的MySQL port | `3306` |
| `externalMySQL.username` | 外部的MySQL 用户名 | |
| `externalMySQL.password` | 外部的MySQL 密码 | |
| `externalMySQL.database` | 外部的MySQL 的database 名称 | `starwhale` |
| `minio.enabled` | 部署一个单例的Minio服务，需要为Minio提供一个PV持久化数据| `true`|
| `minio.persistence.storageClass` | minio pvc storageClass | `local-storage-minio` |
| `externalS3OSS.host`| 当 minio.enabled 设置为 false, 使用外部的基于S3协议的对象存储的host | `localhost` |
| `externalS3OSS.port`| 外部对象存储的port | `9000` |
| `externalS3OSS.accessKey`| 外部对象存储的access key |  |
| `externalS3OSS.secretKey`| 外部对象存储的secret key | |
| `externalS3OSS.region`| 外部对象存储的region | `local` |
| `externalS3OSS.type`| 外部对象存储的类型，目前支持s3、aliyun、minio三种OSS | `minio` |

### 6.4 minikube参数

|字段|描述|默认值|
|---|---|-----|
| `minikube.enabled` | 使用minikube模式 | `false` |

使用minikube模式，可以在单机环境使用minikube安装一个all-in-one的Starwhale，方便进行本地开发、调试。安装命令如下：

```bash
export SWNAME=starwhale SWNS=starwhale
helm upgrade --install $SWNAME starwhale/starwhale --namespace $SWNS --create-namespace --set minikube.enabled=true --set mysql.primary.persistence.storageClass=$SWNAME-mysql --set minio.persistence.storageClass=$SWNAME-minio
```

若在中国大陆网络环境，推荐使用如下命令：

```bash
export SWNAME=starwhale SWNS=starwhale
helm upgrade --install $SWNAME starwhale/starwhale --namespace $SWNS --create-namespace --set minikube.enabled=true --set mysql.primary.persistence.storageClass=$SWNAME-mysql --set minio.persistence.storageClass=$SWNAME-minio --set image.registry=docker-registry.starwhale.cn --set minio.global.imageRegistry=docker-registry.starwhale.cn --set mysql.global.imageRegistry=docker-registry.starwhale.cn
```

### 6.5 开发模式

|字段|描述|默认值|
|---|---|-----|
| `devMode.createPV.enabled`  | 自动创建PV | `false` |
| `devMode.createPV.host`     | 使用Node selector进行选择何处创建PV | "" |
| `devMode.createPV.rootPath` | test PV的 Local path | `/var/starwhale` |

开发模式支持自动创建local path的PV。

```bash
export SWNAME=starwhale
export SWNS=starwhale
helm install $SWNAME . -n $SWNS --create-namespace \
    --set devMode.createPV.enabled=true \
    --set devMode.createPV.host=pv-host \
    --set devMode.createPV.rootPath=/path/to/pv-storage \
    --set mysql.primary.persistence.storageClass=$SWNAME-mysql \
    --set minio.persistence.storageClass=$SWNAME-minio
```

### 6.6 ServiceAccount

为了 Starwhale Controller 能够在 K8s 集群上正常运行，我们需要给 Controller 配置 ServiceAccount，并且分配足够的权限，目前需要的权限列表如下（以RBAC为例）

| Resource | API Group | Get | List | Watch | Create | Delete |
|----------|-----------|-----|------|-------|--------|--------|
| jobs     | batch     | Y   | Y    | Y     | Y      | Y      |
| pods     | core      | Y   | Y    | Y     |        |        |
| nodes    | core      | Y   | Y    | Y     |        |        |
| events   | ""        | Y   |      |       |        |        |

示例编排如下

```yaml
apiVersion: rbac.authorization.k8s.io/v1
kind: ClusterRole
metadata:
  name: test-role
rules:
- apiGroups:
  - ""
  resources:
  - pods
  - nodes
  verbs:
  - get
  - list
  - watch
- apiGroups:
  - "batch"
  resources:
  - jobs
  verbs:
  - create
  - get
  - list
  - watch
  - delete
- apiGroups:
  - ""
  resources:
  - events
  verbs:
  - get
  - watch
  - list
---

apiVersion: rbac.authorization.k8s.io/v1
kind: ClusterRoleBinding
metadata:
  name: test-binding
roleRef:
  apiGroup: rbac.authorization.k8s.io
  kind: ClusterRole
  name: test-role
subjects:
- kind: ServiceAccount
  name: test-sa
```
