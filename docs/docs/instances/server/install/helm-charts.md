---
title: Deploy Starwhale Server with Helm Charts
---

## Helm Charts

Starwhale is an MLOps platform. Starwhale Helm Charts help you deploy the whole platform in Kubernetes easily.

- Easy to deploy, upgrade and maintain Starwhale controller and agent services.
- Easy to deploy 3rd dependencies, such as minio and MySQL.

## TL; DR

```bash
helm repo add starwhale https://star-whale.github.io/charts
helm repo update
helm upgrade --install starwhale starwhale/starwhale -n starwhale --create-namespace
```

## Prerequisites

- Kubernetes 1.19+
- Helm 3.2.0+

## Installing the Chart

To install the chart with the release name starwhale:

```bash
helm repo add starwhale https://star-whale.github.io/charts
helm repo update
helm upgrade --install starwhale starwhale/starwhale -n starwhale --create-namespace
```

We have offered a variety of installation modes in advance.

- For minikube(all-in-one) and global network:

    ```bash
    helm pull starwhale/starwhale --untar --untardir ./charts
    helm upgrade --install starwhale ./charts/starwhale -n starwhale --create-namespace -f ./charts/starwhale/values.minikube.global.yaml
    ```

- For minikube(all-in-one) and China's mainland network:

    ```bash
    helm pull starwhale/starwhale --untar --untardir ./charts
    helm upgrade --install starwhale ./charts/starwhale -n starwhale --create-namespace -f ./charts/starwhale/values.minikube.cn.yaml
    ```

    `helm pull` command will pull and untar the chart package, you can modify the values files for the deep customize.

If you want to install the specified version, you can use `--version` argument. By default, the latest release version will be installed.

## Uninstalling the Chart

To remove the starwhale deployment:

```bash
helm delete starwhale
```

`helm delete` command will not delete the namespace, you can run `kubectl delete namespace starwhale` to cleanup the namespace.

## Upgrading the Chart

To upgrade new chart version:

```bash
helm repo update starwhale
```

The `update` command will update the information of available charts locally from the Starwhale chart repository. You can get more version information from [ArtifactHub](https://artifacthub.io/packages/helm/starwhale/starwhale).

## Parameters

### Common parameters

| Name| Description | Default Value |
|-----|-------------|---------------|
| `image.registry` | image registry, you can find Starwhale docker images in docker.io, ghcr.io and docker-registry.starwhale.cn.|`ghcr.io`|
| `image.org`      | image registry org, [starwhaleai](https://hub.docker.com/u/starwhaleai)(docker.io) or [star-whale](https://github.com/orgs/star-whale)(ghcr.io and docker-registry.starwhale.cn) or some custom org name in other registry.| `star-whale`|

### Starwhale controller parameters

| Name | Description | Default Value |
|------|-------------|---------------|
| `controller.auth.username`| login username for console web| `starwhale`|
| `controller.auth.password`| login password for console web| `abcd1234`|
| `controller.ingress.enabled`| enable ingress for Starwhale controller | `true` |
| `controller.ingress.ingressClassName` | ingress class name | `nginx`|
| `controller.ingress.host` | Starwhale controller domain | `console.pre.intra.starwhale.ai` |
| `controller.containerPort`| Starwhale console web port | `8082` |
| `controller.storageType` | Starwhale supports `s3`, `minio`, `aliyun` and `fs` as the main file storage. | `minio` |

### Infra parameters

Starwhale provides MySQL and minio infra charts, but the charts only support standalone mode for controller experiential, debugging and development, for example, minikube all-in-one scenario. In production, you should use external high available infra by the `externalMySQL` and `externalOSS` parameters.

Standalone Infra for test scenario:

| Name | Description | Default Value |
|------|-------------|---------------|
| `mysql.enabled` | Deploy a standalone mysql instance with starwhale charts. pv/storageClass will be created automatically. | `true` |
| `minio.enabled` | Deploy a standalone minio instance with starwhale charts. pv/storageClass will be created automatically. | `true` |
| `minio.ingress.enabled` | enable ingress for minio admin web. | `true` |
| `minio.ingress.host` | minio admin web domain | `minio.pre.intra.starwhale.ai` |

External Infra for production scenario:

| Name | Description | Default Value |
|------|-------------|---------------|
| `externalMySQL.host` | When mysql.enabled is false, charts will use external mysql. | `localhost` |
| `externalMySQL.port` | port for the external mysql | `3306` |
| `externalMySQL.username` | username for the external mysql | `` |
| `externalMySQL.password` | password for the external mysql | `` |
| `externalMySQL.database` | The System Admin should create a database for Starwhale in the external mysql. | `starwhale` |
| `externalOSS.host` | When minio.enabled is false, charts will use the external OSS service. | `localhost` |
| `externalOSS.port` | port for the external OSS service | `9000` |
| `externalOSS.accessKey` | access key for the external OSS service | `` |
| `externalOSS.secretKey` | secret key for the external OSS service | `` |
| `externalOSS.defaultBuckets` | The System Admin should create a bucket for Starwhale in the external OSS service. | `starwhale` |
| `externalOSS.region` | bucket's region for the external OSS service | `local` |

### dev mode

| Name                        | Description                                              | Default Value    |
|-----------------------------|----------------------------------------------------------|------------------|
| `devMode.createPV.enabled`  | enable auto create PV                                    | `false`          |
| `devMode.createPV.host`     | Node selector matchExpressions in kubernetes.io/hostname | ""               |
| `devMode.createPV.rootPath` | Local path for test PV                                   | `/var/starwhale` |

Dev mode support creating local path PV automatically when devMode.createPV.enabled sets to `true`

e.g.

```bash
helm install starwhale . -n starwhale --create-namespace \
    --set devMode.createPV.enabled=true \
    --set devMode.createPV.host=pv-host \
    --set devMode.createPV.rootPath=/path/to/pv-storage
```

### ServiceAccount

Starwhale Controller can only work properly with ServiceAccount with sufficient permissions. The list of permissions required is as follows (take RBAC as an example):

| Resource | API Group | Get | List | Watch | Create | Delete |
|----------|-----------|-----|------|-------|--------|--------|
| jobs     | batch     | Y   | Y    | Y     | Y      | Y      |
| pods     | core      | Y   | Y    | Y     |        |        |
| nodes    | core      | Y   | Y    | Y     |        |        |
| events   | ""        | Y   |      |       |        |        |

example

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
