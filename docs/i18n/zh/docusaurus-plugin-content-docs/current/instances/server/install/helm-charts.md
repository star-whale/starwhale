---
title: 使用Helm安装Cloud Instance
---

## Helm Charts

使用Helm Charts可以非常容易的将Starwhale Cloud Instance在Kubernetes集群中进行安装，包括Starwhale Controller和第三方基础依赖（mysql/minio等）。

## 核心命令

```bash
helm repo add starwhale https://star-whale.github.io/charts
helm repo update
helm upgrade --install starwhale starwhale/starwhale -n starwhale --create-namespace
```

## 前置条件

- Kubernetes 1.19+
- Helm 3.2.0+

## 安装Chart

```bash
helm repo add starwhale https://star-whale.github.io/charts
helm repo update
helm upgrade --install starwhale starwhale/starwhale -n starwhale --create-namespace
```

我们提供了多种使用不同场景的values.yaml:

- 针对minikube的all-in-one场景，适合非中国大陆网络环境：

    ```bash
    helm pull starwhale/starwhale --untar --untardir ./charts
    helm upgrade --install starwhale ./charts/starwhale -n starwhale --create-namespace -f ./charts/starwhale/values.minikube.global.yaml
    ```

- 针对minikube的all-in-one场景，适合中国大陆网络环境：

    ```bash
    helm pull starwhale/starwhale --untar --untardir ./charts
    helm upgrade --install starwhale ./charts/starwhale -n starwhale --create-namespace -f ./charts/starwhale/values.minikube.cn.yaml
    ```

    `helm pull` 命令会将chart包下载到本地并解压，可以修改其中的values文件，满足个性化场景需求。

可以通过 `--version` 参数指定版本，默认会安装最新发布的Charts

## 卸载Chart

```bash
helm delete starwhale
```

`helm delete` 命令不支持删除Kubernetes的namespace，需要执行 `kubectl delete namespace starwhale` 命令进行删除。

## 更新Chart

```bash
helm repo update starwhale
```

`update` 命令从远端的Starwhale Chart仓库中更新本地的charts文件。更详细的版本信息可以参考[ArtifactHub](https://artifacthub.io/packages/helm/starwhale/starwhale)。

## 配置参数说明

### 基本参数

|字段|描述|默认值|
|---|---|-----|
| `image.registry` | 镜像Registry, Starwhale镜像会发布在 docker.io, ghcr.io和docker-registry.starwhale.cn上，中国大陆网络推荐使用docker-registry.starwhale.cn镜像源 | `ghcr.io`|
| `image.org`      | 镜像的org名字： [starwhaleai](https://hub.docker.com/u/starwhaleai)(docker.io)、[star-whale](https://github.com/orgs/star-whale)(ghcr.io和docker-registry.starwhale.cn) 或者其他在私有registry上定义的镜像org名字 | `star-whale`  |

### Starwhale controller参数

|字段|描述|默认值|
|---|---|-----|
| `controller.auth.username`| console web 用户名| `starwhale`|
| `controller.auth.password`| console web 密码| `abcd1234`|
| `controller.ingress.enabled`| 使用Ingress | `true` |
| `controller.ingress.ingressClassName` | ingress class name | `nginx`|
| `controller.ingress.host` | Starwhale controller 访问域名 | `console.pre.intra.starwhale.ai` |
| `controller.containerPort`| Starwhale console web port | `8082` |
| `controller.storageType`| Controller文件存储的类型，目前支持s3、aliyun、minio、fs四种 | `minio` |

### 基础设施参数

Starwhale 提供MySQL和minio的Charts，由于是单例模式，故只能在开发调试场景中使用。如果是生产环境，需要通过externalMySQL和externalOSS参数配置外部的高可用基础设施。

测试环境中使用的单例基础设施：

|字段|描述|默认值|
|------|-------------|---------------|
| `mysql.enabled` | 部署一个单例的MySQL服务，需要为MySQL提供一个PV持久化数据 | `true` |
| `minio.enabled` | 部署一个单例的Minio服务，需要为Minio提供一个PV持久化数据| `true`|
| `minio.ingress.enabled` | 设置minio admin web的Ingress | `true` |
| `minio.ingress.host` | minio admin web 域名 | `minio.pre.intra.starwhale.ai` |

生产环境中使用的外部基础设施：

|字段|描述|默认值|
|---|---|-----|
| `externalMySQL.host` | 当 mysql.enabled 设置为 false, 使用外部的MySQL host | `localhost` |
| `externalMySQL.port` | 使用外部的MySQL port | `3306` |
| `externalMySQL.username` | 外部的MySQL 用户名 | `` |
| `externalMySQL.password` | 外部的MySQL 密码 | `` |
| `externalMySQL.database` | 系统管理创建一个给Starwhale独立使用的database | `starwhale` |
| `externalOSS.host`| 当 minio.enabled 设置为 false, 使用外部的基于S3协议的对象存储的host | `localhost` |
| `externalOSS.port`| 外部对象存储的port | `9000` |
| `externalOSS.accessKey`| 外部对象存储的access key | `` |
| `externalOSS.secretKey`| 外部对象存储的secret key | `` |
| `externalOSS.region`| 外部对象存储的region | `local` |
| `externalOSS.defaultBuckets` | 系统管理员创建一个给Starwhale独立使用的bucket | `starwhale` |

### 开发模式

|字段|描述|默认值|
|---|---|-----|
| `devMode.createPV.enabled`  | 自动创建PV | `false` |
| `devMode.createPV.host`     | 使用Node selector进行选择何处创建PV | "" |
| `devMode.createPV.rootPath` | test PV的 Local path | `/var/starwhale` |

开发模式支持自动创建local path的PV。

```bash
helm install starwhale . -n starwhale --create-namespace \
    --set devMode.createPV.enabled=true \
    --set devMode.createPV.host=pv-host \
    --set devMode.createPV.rootPath=/path/to/pv-storage
```

### ServiceAccount

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
