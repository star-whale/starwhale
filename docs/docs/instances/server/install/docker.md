---
title: Deploy Starwhale Server with Docker
---

## 1. Prerequisites

* Docker or Podman environment
* Pod of Starwhale job can access the port exposed by Starwhale controller
* Object Store
* MySQL database

## 2. Configurations

### 2.1. environments

Most of the configuration of Controller can be controlled by environment variables. Here is a list of available
environment variables.

| Name                         | Description                                  | Optional | Default | Example                  |
|------------------------------|----------------------------------------------|----------|---------|--------------------------|
| SW_JWT_SECRET                | JWT secret                                   | No       |         |                          |
| SW_JWT_TOKEN_EXPIRE_MINUTES  | JWT token expire duration in minutes         | Yes      | 43200   |                          |
| SW_CONTROLLER_PORT           | Exposed port by Controller                   | Yes      | 8082    |                          |
| SW_K8S_NAME_SPACE            | K8s namespace used by   evaluations          | No       |         | default                  |
| SW_INSTANCE_URI              | Controller connection uri for evaluation job | No       |         | <http://controller:8082> |
| SW_STORAGE_ENDPOINT          | Endpoint of the external OSS                 | No       |         | <http://foo.com:8088>    |
| SW_STORAGE_BUCKET            | bucket of the external OSS                   | No       |         | starwhale                |
| SW_STORAGE_ACCESSKEY         | access key of the external OSS               | No       |         |                          |
| SW_STORAGE_SECRETKEY         | secret key of the external OSS               | No       |         |                          |
| SW_STORAGE_REGION            | region of the external OSS                   | Yes      | local   |                          |
| SW_STORAGE_TYPE              | OSS type, (s3, aliyun, minio)                | Yes      | minio   |                          |
| SW_METADATA_STORAGE_IP       | MySQL host                                   | No       |         |                          |
| SW_METADATA_STORAGE_PORT     | MySQL port                                   | No       |         |                          |
| SW_METADATA_STORAGE_USER     | MySQL user                                   | No       |         |                          |
| SW_METADATA_STORAGE_PASSWORD | MySQL password                               |          |         |                          |
| SW_METADATA_STORAGE_DB       | MySQL database name                          |          |         |                          |

More environment variables in [application.yaml](https://github.com/star-whale/starwhale/blob/main/server/controller/src/main/resources/application.yaml).

### 2.2. Kubeconfig

For a brief description of Kubeconfig, please refer to the [official documentation](https://kubernetes.io/docs/concepts/configuration/organize-cluster-access-kubeconfig/).

Controller needs to run the task on the K8s cluster, we can provide this file by mounting a file or directory to the container.

Demo startup script:

```sh
export MAIN_IP=$(ip addr show eth0 | grep "inet\b" | awk '{print $2}' | cut -d/ -f1)
export PORT=8082
export IMAGE=ghcr.io/star-whale/server:0.3.0

docker run -it -d --name controller -p 8082:8082 \
        --restart unless-stopped \
        --mount type=bind,source=/etc/kube/config,destination=/root/.kube/config,readonly \
        -e SW_JWT_SECRET=PfUeyDjzt4Vp8pFpOj7sR1eJ5r6 \
        -e SW_CONTROLLER_PORT=${PORT} \
        -e SW_RUNTIME_IMAGE_DEFAULT=ghcr.io/star-whale/starwhale:latest \
        -e SW_K8S_NAME_SPACE=default \
        -e SW_INSTANCE_URI=http://${MAIN_IP}:${PORT} \
        -e SW_STORAGE_ENDPOINT=http://foo.com:8088 \
        -e SW_STORAGE_BUCKET=starwhale \
        -e SW_STORAGE_ACCESSKEY=VC0x1qoTT7u9eFQTJemUI7s1 \
        -e SW_STORAGE_SECRETKEY=6mS15aUBloF8DuMiGBK7ij56VZhnwi \
        -e SW_STORAGE_REGION=local \
        -e SW_STORAGE_TYPE=s3 \
        -e SW_METADATA_STORAGE_IP=bar.com \
        -e SW_METADATA_STORAGE_PORT=3306 \
        -e SW_METADATA_STORAGE_PASSWORD=starwhale \
        -e SW_METADATA_STORAGE_USER=starwhale \
        -e SW_METADATA_STORAGE_DB=starwhale \
        ${IMAGE}
```
