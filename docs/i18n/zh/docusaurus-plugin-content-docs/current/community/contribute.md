---
title: Starwhale开源贡献指南
---

## 1. 参与贡献

Starwhale 非常欢迎来自开源社区的贡献，包括但不限于以下方式：

- 描述使用过程中的遇到的问题
- 提交Feature Request
- 参与Slack和Github Issues讨论
- 参与Code Review
- 改进文档和示例程序
- 修复程序Bug
- 增加Test Case
- 改进代码的可读性
- 开发新的Features
- 编写Enhancement Proposal

可以通过以下方式参与开发者社区，获取最新信息和联系Starwhale开发者：

- [Slack](https://starwhale.slack.com/)
- [Github Issues](https://github.com/star-whale/starwhale/issues)
- [Twitter](https://twitter.com/starwhaleai)
- Email: *developer@starwhale.ai*

Starwhale社区使用[Github Issues](https://github.com/star-whale/starwhale/issues)来跟踪问题和管理新特性的开发。可以选择"good first issue"或"help wanted"标签的issue，作为参与开发Starwhale的起点。

## 2. Starwhale资源列表

- [主页](http://starwhale.ai)
- [Starwhale Cloud](https://cloud.starwhale.cn)
- [官方文档](https://doc.starwhale.ai)
- [Github Repo](https://github.com/star-whale/starwhale)
- [Python Package](https://pypi.org/project/starwhale/)
- Docker镜像：[Docker Hub](https://hub.docker.com/u/starwhaleai)，[ghcr.io](https://github.com/orgs/star-whale/packages)
- [Helm Charts](https://artifacthub.io/packages/helm/starwhale/starwhale)

## 3. 代码基本结构

核心目录组织及功能说明如下：

- [client](https://github.com/star-whale/starwhale/tree/main/client)：swcli和Python SDK的实现，使用Python3编写，对应Starwhale Standalone Instance的所有功能。
  - [api](https://github.com/star-whale/starwhale/tree/main/client/starwhale/api)：Python SDK的接口定义和实现。
  - [cli](https://github.com/star-whale/starwhale/tree/main/client/starwhale/cli)：Command Line Interface的入口点。
  - [base](https://github.com/star-whale/starwhale/tree/main/client/starwhale/base)：Python 端的一些基础抽象。
  - [core](https://github.com/star-whale/starwhale/tree/main/client/starwhale/core)：Starwhale 核心概念的实现，包括Dataset、Model、Runtime、Project、Job、Evaluation等。
  - [utils](https://github.com/star-whale/starwhale/tree/main/client/starwhale/utils)：Python 端的一些工具函数。
- [console](https://github.com/star-whale/starwhale/tree/main/console)：前端的实现，使用React + TypeScript编写，对应Starwhale Cloud Instance的Web UI。
- [server](https://github.com/star-whale/starwhale/tree/main/server)：Starwhale Controller的实现，使用Java编写，对应Starwhale Cloud Instance的后端API。
- [docker](https://github.com/star-whale/starwhale/tree/main/docker)：Helm Charts，绝大多数Docker Image的Dockerfile等。
- [docs](https://github.com/star-whale/starwhale/tree/main/docs)：Starwhale官方文档。
- [example](https://github.com/star-whale/starwhale/tree/main/example)：示例程序，包含MNIST等例子。
- [scripts](https://github.com/star-whale/starwhale/tree/main/scripts)：一些Bash和Python脚本，用来进行E2E测试和软件发布等。

## 4. Fork&Clone Starwhale仓库

你需要fork Starwhale仓库代码并clone到本机，

- Fork Starwhale仓库：[Fork Starwhale Github Repo](https://github.com/star-whale/starwhale/fork)，更多使用详情可参考：[Fork a repo](https://docs.github.com/en/get-started/quickstart/fork-a-repo)
- 安装Git-LFS：[Git LFS](https://github.com/git-lfs/git-lfs/blob/main/INSTALLING.md#installing-packages)

  ```bash
  git lfs install
  ```

- Clone代码到本地

  ```bash
  git clone https://github.com/${your username}/starwhale.git
  ```

## 5. 搭建针对Standalone Instance的本地开发环境

Standalone Instance采用Python编写，当要修改Python SDK和swcli时，需要进行相应的环境搭建。

### 5.1 前置条件

- OS：Linux或macOS
- Python：3.7~3.10
- Docker：>=19.03 (非必须，当调试dockerize、生成docker image或采用docker为载体运行模型任务时需要)
- Python隔离环境：Python venv 或 virtualenv 或 conda等都可以，用来构建一个隔离的Python环境

### 5.2 从源码进行安装

基于上一步clone到本地的仓库目录：starwhale，并进入到client子目录：

```bash
cd starwhale/client
```

使用Conda创建一个Starwhale开发环境，或者使用venv/virtualenv等创建：

```bash
conda create -n starwhale-dev python=3.8 -y
conda activate starwhale-dev
```

安装Client包及依赖到starwhale-dev环境中：

```bash
make install-sw
make install-dev-req
```

输入`swcli --version`命令，观察是否安装成功，开发环境的swcli版本是 `0.0.0.dev0` ：

```bash
❯ swcli --version
swcli, version 0.0.0.dev0

❯ swcli --version
/home/username/anaconda3/envs/starwhale-dev/bin/swcli
```

### 5.3 本地修改代码

现在可以对Starwhale代码进行修改，**不需要重复安装(`make install-sw`命令)就能在当前starwhale-dev环境是测试cli或sdk**。Starwhale Repo中设置了 [.editorconfig](https://github.com/star-whale/starwhale/blob/main/.editorconfig) 文件，大部分IDE或代码编辑器会自动支持该文件的导入，采用统一的缩进设置。

### 5.4 执行代码检查和测试

在 `starwhale` 目录中操作，会执行单元测试、client的e2e测试、mypy检查、flake8检查和isort检查等。

```console
make client-all-check
```

## 6. 搭建针对Cloud Instance的本地开发环境

Cloud Instance的后端采用Java编写，前端采用React+TypeScript编写，可以按需搭建相应的开发环境。

### 6.1 搭建前端Console开发环境

### 6.2 搭建后端Server开发环境

- 开发语言：Java
- 项目构建工具：Maven
- 开发框架：Spring Boot+Mybatis
- 测试框架：Junit5（其中mock框架为mockito，断言部分使用hamcrest，数据库、web服务等模拟使用Testcontainers）
- 代码检查：使用maven插件 maven-checkstyle-plugin

#### 6.2.1 前置条件

- OS：Linux、macOS或Windows
- JDK: >=11
- Docker：>=19.03
- Maven：>=3.8.1
- Mysql：>=8.0.29
- Minio
- Kubernetes cluster/Minikube（如果没有k8s集群，可以使用Minikube作为开发调试时的备选方案）

#### 6.2.2 修改代码并增加单测

现在可以进入到相应模块，对server端的代码进行修改、调整。其中业务功能代码位置为src/main/java，单元测试目录为src/test/java。

#### 6.2.3 执行代码检查和单元测试

```bash
cd starwhale/server
mvn clean package
```

#### 6.2.4 本地部署服务

- 前置服务
  - Minikube（可选，无k8s集群时可使用此服务，安装方式可见：[Minikube](https://minikube.sigs.k8s.io/docs/start/)）

    ```bash
    minikube start
    minikube addons enable ingress
    minikube addons enable ingress-dns
    ```

  - Mysql

    ```bash
    docker run --name sw-mysql -d \
    -p 3306:3306 \
    -e MYSQL_ROOT_PASSWORD=starwhale \
    -e MYSQL_USER=starwhale \
    -e MYSQL_PASSWORD=starwhale \
    -e MYSQL_DATABASE=starwhale \
    mysql:latest
    ```

  - Minio

    ```bash
    docker run --name minio -d
    -p 9000:9000  --publish 9001:9001
    -e MINIO_DEFAULT_BUCKETS='starwhale'
    -e MINIO_ROOT_USER="minioadmin"
    -e MINIO_ROOT_PASSWORD="minioadmin"
    bitnami/minio:latest
    ```

- 打包server程序
  > 若部署server端时，需要把前端同时部署上，可先执行前端部分的构建命令，然后执行'mvn clean package'，则会自动将已编译好的前端文件打包进来。

  使用如下命令对程序进行打包：

  ```bash
  cd starwhale/server
  mvn clean package
  ```

- 指定server启动所需的环境变量

  ```bash
  # Minio相关配置
  export SW_STORAGE_ENDPOINT=http://${Minio IP，默认为127.0.0.1}:9000
  export SW_STORAGE_BUCKET=${Minio bucket，默认为starwhale}
  export SW_STORAGE_ACCESSKEY=${Minio accessKey，默认为starwhale}
  export SW_STORAGE_SECRETKEY=${Minio secretKey，默认为starwhale}
  export SW_STORAGE_REGION=${Minio region，默认为local}
  # kubernetes配置
  export KUBECONFIG=${.kube配置文件所在路径}\.kube\config

  export SW_INSTANCE_URI=http://${Server服务所在机器IP}:8082
  # Mysql相关配置
  export SW_METADATA_STORAGE_IP=${Mysql IP，默认为127.0.0.1}
  export SW_METADATA_STORAGE_PORT=${Mysql port，默认为3306}
  export SW_METADATA_STORAGE_DB=${Mysql dbname，默认为starwhale}
  export SW_METADATA_STORAGE_USER=${Mysql user，默认为starwhale}
  export SW_METADATA_STORAGE_PASSWORD=${user password，默认为starwhale}
  ```

- 部署server服务

  使用IDE或如下方式部署均可。

  ```bash
  java -jar controller/target/starwhale-controller-0.1.0-SNAPSHOT.jar
  ```

- 功能调试

    这里有两种方式对修改的功能进行调试：

  - 使用swagger-ui进行接口调试，访问 /swagger-ui/index.html找到对应的api即可。
  - 或直接在ui访问，进行相应功能的调试（前提是打包时已经按说明将前端代码进行了提前构建）
