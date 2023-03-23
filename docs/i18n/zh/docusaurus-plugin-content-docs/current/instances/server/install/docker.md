---
title: 使用 Docker 部署 Starwhale Controller
---

## 使用场景

本章节主要介绍如何使用 docker 独立部署 Starwhale 的 Controller， 独立部署 Controller 一般适用于如下场景

* 不具备将 Starwhale Controller 部署到 K8s 集群中的条件 (比如无法给Controller创建SVC)
* 不想将 Controller 部署到 K8s

## 依赖

* docker 运行环境 （podman等类似）
* K8s上的 Pod 能够访问到 docker 暴露的端口
* 第三方的对象存储，目前支持 MinIO，aws s3，阿里云对象存储
* MySQL 数据库

## 配置

### . 环境变量

Controller 的大部分配置都可以通过环境变量来控制， 下面是可用的环境变量列表

| 环境变量名称                   | 说明                              | 是否必须 | 默认值   | 举例                       |
|------------------------------|---------------------------------|---------|--------|--------------------------|
| SW_JWT_SECRET                | JWT的加密secret字符串                 | 是    |||
| SW_JWT_TOKEN_EXPIRE_MINUTES  | JWT token 的过期时间（单位：分钟）          |否|43200||
| SW_CONTROLLER_PORT           | Controller对外Serve的端口            | 否    | 8082 |
| SW_K8S_NAME_SPACE            | 对接的K8s集群时使用的命名空间                |是    | | default                  |
| SW_INSTANCE_URI              | Controller 对外暴露的访问方法            |是    | | <http://controller:8082> |
| SW_STORAGE_ENDPOINT          | 外部对象存储的 endpoint                |是| | <http://foo.com:8088>    |
| SW_STORAGE_BUCKET            | 外部对象存储的 bucket                  |是| | starwhale                |
| SW_STORAGE_ACCESSKEY         | 外部对象存储的 access key              |是|||
| SW_STORAGE_SECRETKEY         | 外部对象存储的 secret key              |是|||
| SW_STORAGE_REGION            | 外部对象存储的 region                  |否|local||
| SW_STORAGE_TYPE              | 外部对象存储的类型，目前支持s3、aliyun、minio三种 |否|minio||
| SW_METADATA_STORAGE_IP       | 外部的MySQL host                   |是|||
| SW_METADATA_STORAGE_PORT     | 外部的MySQL port                   |是|||
| SW_METADATA_STORAGE_USER     | 外部的MySQL 用户名                    |是|||
| SW_METADATA_STORAGE_PASSWORD | 外部的MySQL 密码                     |是|||
| SW_METADATA_STORAGE_DB       | 外部的MySQL 的database 名称           |是|||

更多环境变量可参考 [application.yaml](https://github.com/star-whale/starwhale/blob/main/server/controller/src/main/resources/application.yaml)

### . K8s 鉴权文件

Kubeconfig 的简要说明可参考[官方文档](https://kubernetes.io/docs/concepts/configuration/organize-cluster-access-kubeconfig/)

Controller 需要将任务运行在K8s集群上，所以需要将这个文件提供给 Controller, 我们可以通过挂载文件或者目录的方式提供此文件

一个简单启动示例如下

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
