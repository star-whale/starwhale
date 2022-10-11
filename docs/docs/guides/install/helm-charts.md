---
title: Helm Charts Installation
---

## Helm Charts

Helm Charts helps you quickly deploy the whole Starwhale instance in Kubernetes.

- To deploy, upgrade, and maintain the Starwhale `controller`.
- To deploy third-party dependencies, such as minio, mysql, etc.

## TL; DR

```bash
helm repo add starwhale https://star-whale.github.io/charts
helm repo update
helm install starwhale starwhale/starwhale -n starwhale --create-namespace
```

## Prerequisites

- Kubernetes 1.19+
- Helm 3.2.0+

## Installing the Chart

To install the chart with the release name starwhale:

```bash
helm repo add starwhale https://star-whale.github.io/charts
helm install starwhale starwhale/starwhale
```

## Uninstalling the Chart

To remove the starwhale deployment:

```bash
helm delete starwhale
```

## Parameters

### Common parameters

| Name             | Description                                                                                                                                                                                 | Default Value |
|------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|---------------|
| `image.registry` | image registry, you can find Starwhale docker images in docker.io or ghcr.io.                                                                                                               | `ghcr.io`     |
| `image.org`      | image registry org, [starwhaleai](https://hub.docker.com/u/starwhaleai)(docker.io) or [star-whale](https://github.com/orgs/star-whale)(ghcr.io) or some custom org name in other registries | `star-whale`  |

### Starwhale parameters

| Name                        | Description                                | Default Value                    |
|-----------------------------|--------------------------------------------|----------------------------------|
| `controller.taskSplitSize`  | task split size                            | `2`                              |
| `controller.auth.username`  | admin user name                            | `starwhale`                      |
| `controller.auth.password`  | admin user password                        | `abcd1234`                       |
| `storage.agentHostPathRoot` | persistent host-path for cache data of job | `/mnt/data/starwhale`            |
| `mirror.conda.enabled`      | conda mirror                               | `true`                           |
| `mirror.pypi.enabled`       | pypi mirror                                | `true`                           |
| `ingress.enabled`           | enable ingress for starwhale controller    | `true`                           |
| `ingress.ingressClassName`  | ingress class name                         | `nginx`                          |
| `ingress.host`              | starwhale controller domain                | `console.pre.intra.starwhale.ai` |

### Infra parameters

| Name                             | Description                                                                                                            | Default Value         |
|----------------------------------|------------------------------------------------------------------------------------------------------------------------|-----------------------|
| `mysql.enabled`                  | Deploy a standalone mysql instance with starwhale chart. If set mysql.enabled=true, you should provide a pv for mysql. | `true`                |
| `mysql.persistence.storageClass` | mysql pvc storageClass                                                                                                 | `local-storage-mysql` |
| `externalMySQL.host`             | When mysql.enabled is false, chart will use external mysql.                                                            | `localhost`           |
| `minio.enabled`                  | Deploy a standalone minio instance with starwhale chart. If set minio.enabled=true, you should provide a pv for minio. | `true`                |
| `minio.persistence.storageClass` | minio pvc storageClass                                                                                                 | `local-storage-minio` |
| `externalS3OSS.host`             | When minio.enabled is false, chart will use external s3 service.                                                       | `localhost`           |

### minikube parameters

| Name               | Description                            | Default Value |
|--------------------|----------------------------------------|---------------|
| `minikube.enabled` | minikube mode for the all-in-one test. | `false`       |

In minikube mode, you can easy to build an all-in-one starwhale. Run command example:

```bash
helm upgrade --install starwhale starwhale/starwhale --namespace starwhale --create-namespace --set minikube.enabled=true
```

## Community

- To report a bug or request a feature, use [Github Issues](https://github.com/star-whale/starwhale/issues/new/choose).
- For online support, please join us on [Slack](https://join.slack.com/t/starwhale/shared_invite/zt-19b6cwnyo-BxMrZYWKj2J~kly1c32oEA).
- Your pull requests are always welcomed: [Github Repo](https://github.com/star-whale/starwhale).
