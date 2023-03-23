---
title: Contribute to Starwhale
---

## Getting Involved/Contributing

We welcome and encourage all contributions to Starwhale, including and not limited to:

- Describe the problems encountered during use.
- Submit feature request.
- Discuss in Slack and Github Issues.
- Code Review.
- Improve docs, tutorials and examples.
- Fix Bug.
- Add Test Case.
- Code readability and code comments to import readability.
- Develop new features.
- Write enhancement proposal.

You can get involved, get updates and contact Starwhale developers in the following ways:

- [Slack](https://starwhale.slack.com/)
- [Github Issues](https://github.com/star-whale/starwhale/issues)
- [Twitter](https://twitter.com/starwhaleai)
- Email: *developer@starwhale.ai*

## Starwhale Resources

- [Homepage](http://starwhale.ai)
- [Starwhale Cloud](https://cloud.starwhale.cn)
- [Official docs](https://doc.starwhale.ai)
- [Github Repo](https://github.com/star-whale/starwhale)
- [Python Package](https://pypi.org/project/starwhale/)
- Docker Image：[Docker Hub](https://hub.docker.com/u/starwhaleai)，[ghcr.io](https://github.com/orgs/star-whale/packages)
- [Helm Charts](https://artifacthub.io/packages/helm/starwhale/starwhale)

## Code Structure

- [client](https://github.com/star-whale/starwhale/tree/main/client): swcli and Python SDK with Pure Python3, which includes all Standalone Instance features.
  - [api](https://github.com/star-whale/starwhale/tree/main/client/starwhale/api): Python SDK.
  - [cli](https://github.com/star-whale/starwhale/tree/main/client/starwhale/cli): Command Line Interface entrypoint.
  - [base](https://github.com/star-whale/starwhale/tree/main/client/starwhale/base): Python base abstract.
  - [core](https://github.com/star-whale/starwhale/tree/main/client/starwhale/core): Starwhale core concepts which includes Dataset,Model,Runtime,Project, job and Evaluation, etc.
  - [utils](https://github.com/star-whale/starwhale/tree/main/client/starwhale/utils): Python utilities lib.
- [console](https://github.com/star-whale/starwhale/tree/main/console): frontend with React + TypeScript.
- [server](https://github.com/star-whale/starwhale/tree/main/server)：Starwhale Controller with java, which includes all Starwhale Cloud Instance backend apis.
- [docker](https://github.com/star-whale/starwhale/tree/main/docker)：Helm Charts, dockerfile.
- [docs](https://github.com/star-whale/starwhale/tree/main/docs)：Starwhale official docs.
- [example](https://github.com/star-whale/starwhale/tree/main/example)：Example code.
- [scripts](https://github.com/star-whale/starwhale/tree/main/scripts)：Bash and Python scripts for E2E testing and software releases, etc.

## Fork and clone the repository

You will need to fork the code of Starwhale repository and clone it to your local machine.

- Fork Starwhale repository: [Fork Starwhale Github Repo](https://github.com/star-whale/starwhale/fork),For more usage details, please refer to: [Fork a repo](https://docs.github.com/en/get-started/quickstart/fork-a-repo)
- Install Git-LFS:[Git-LFS](https://github.com/git-lfs/git-lfs/blob/main/INSTALLING.md#installing-packages)

 ```bash
  git lfs install
  ```

- Clone code to local machine

  ```bash
  git clone https://github.com/${your username}/starwhale.git
  ```

## Development environment for Standalone Instance

Standalone Instance is written in Python3. When you want to modify swcli and sdk, you need to build the development environment.

### Standalone development environment prerequisites

- OS: Linux or macOS.
- Python: 3.7~3.10.
- Docker: >=19.03(optional).
- Python isolated env tools：Python venv, virtualenv or conda, etc.

### Building from source code

Based on the previous step, clone to the local directory: starwhale, and enter the client subdirectory:

```bash
cd starwhale/client
```

Create an isolated python environment with conda:

```bash
conda create -n starwhale-dev python=3.8 -y
conda activate starwhale-dev
```

Install client package and python dependencies into the starwhale-dev environment:

```bash
make install-sw
make install-dev-req
```

Validate with the `swcli --version` command. In the development environment, the version is `0.0.0.dev0`:

```bash
❯ swcli --version
swcli, version 0.0.0.dev0

❯ swcli --version
/home/username/anaconda3/envs/starwhale-dev/bin/swcli
```

### Modifying the code

When you modify the code, you need not to install python package(run `make install-sw` command) again. [.editorconfig](https://github.com/star-whale/starwhale/blob/main/.editorconfig) will be imported into the most IDE and code editors which helps maintain consistent coding styles for multiple developers.

### Lint and Test

Run unit test, E2E test, mypy lint, flake lint and isort check in the `starwhale` directory.

```console
make client-all-check
```

## Development environment for Cloud Instance

Cloud Instance is written in Java(backend) and React+TypeScript(frontend).

### Development environment for Console

### Development environment for Server

- Language: Java
- Build tool: Maven
- Development framework: Spring Boot+Mybatis
- Unit test framework：Junit5
  - Mockito used for mocking
  - Hamcrest used for assertion
  - Testcontainers used for providing lightweight, throwaway instances of common databases, Selenium web browsers that can run in a Docker container.
- Check style tool：use maven-checkstyle-plugin

#### Server development environment prerequisites

- OS: Linux, macOS or Windows
- Docker: >=19.03
- JDK: >=11
- Maven: >=3.8.1
- Mysql: >=8.0.29
- Minio
- Kubernetes cluster/Minikube(If you don't have a k8s cluster, you can use Minikube as an alternative for development and debugging)

#### Modify the code and add unit tests

Now you can enter the corresponding module to modify and adjust the code on the server side. The main business code directory is src/main/java, and the unit test directory is src/test/java.

#### Execute code check and run unit tests

```bash
cd starwhale/server
mvn clean test
```

#### Deploy the server at local machine

- Dependent services that need to be deployed
  - Minikube（Optional. Minikube can be used when there is no k8s cluster, there is the installation doc: [Minikube](https://minikube.sigs.k8s.io/docs/start/)）

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
    docker run --name minio -d \
    -p 9000:9000  --publish 9001:9001 \
    -e MINIO_DEFAULT_BUCKETS='starwhale' \
    -e MINIO_ROOT_USER="minioadmin" \
    -e MINIO_ROOT_PASSWORD="minioadmin" \
    bitnami/minio:latest
    ```

- Package server program
  > If you need to deploy the front-end at the same time when deploying the server, you can execute the build command of the front-end part first, and then execute 'mvn clean package', and the compiled front-end files will be automatically packaged.

  Use the following command to package the program

  ```bash
    cd starwhale/server
    mvn clean package
  ```

- Specify the environment required for server startup

  ```bash
  # Minio env
  export SW_STORAGE_ENDPOINT=http://${Minio IP,default is:27.0.0.1}:9000
  export SW_STORAGE_BUCKET=${Minio bucket,default is:starwhale}
  export SW_STORAGE_ACCESSKEY=${Minio accessKey,default is:starwhale}
  export SW_STORAGE_SECRETKEY=${Minio secretKey,default is:starwhale}
  export SW_STORAGE_REGION=${Minio region,default is:local}
  # kubernetes env
  export KUBECONFIG=${the '.kube' file path}\.kube\config

  export SW_INSTANCE_URI=http://${Server IP}:8082
  export SW_METADATA_STORAGE_IP=${Mysql IP,default: 127.0.0.1}
  export SW_METADATA_STORAGE_PORT=${Mysql port,default: 3306}
  export SW_METADATA_STORAGE_DB=${Mysql dbname,default: starwhale}
  export SW_METADATA_STORAGE_USER=${Mysql user,default: starwhale}
  export SW_METADATA_STORAGE_PASSWORD=${user password,default: starwhale}
  ```

- Deploy server service

  You can use the IDE or the command to deploy.

  ```bash
  java -jar controller/target/starwhale-controller-0.1.0-SNAPSHOT.jar
  ```

- Debug

  there are two ways to debug the modified function:

  - Use swagger-ui for interface debugging, visit /swagger-ui/index.html to find the corresponding api
  - Debug the corresponding function directly in the ui (provided that the front-end code has been built in advance according to the instructions when packaging)
