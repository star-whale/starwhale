---
title: Contribute to Starwhale
---

## 1. Getting Involved/Contributing

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

## 2. Starwhale Resources

- [Homepage](http://starwhale.ai)
- [Starwhale Cloud](https://cloud.starwhale.cn)
- [Official docs](https://doc.starwhale.ai)
- [Github Repo](https://github.com/star-whale/starwhale)
- [Python Package](https://pypi.org/project/starwhale/)
- Docker Image：[Docker Hub](https://hub.docker.com/u/starwhaleai)，[ghcr.io](https://github.com/orgs/star-whale/packages)
- [Helm Charts](https://artifacthub.io/packages/helm/starwhale/starwhale)

## 3. Code Structure

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

## 4. Development environment for Standalone Instance

Standalone Instance is written in Python3. When you want to modify swcli and sdk, you need to build the development environment.

### 4.1 Prerequisites

- OS: Linux or macOS.
- Python: 3.7~3.10.
- Docker: >=19.03(optional).
- Python isolated env tools：Python venv, virtualenv or conda, etc.
- [Fork Starwhale Github Repo](https://github.com/star-whale/starwhale/fork)
- Git and [Git-LFS](https://github.com/git-lfs/git-lfs/blob/main/INSTALLING.md#installing-packages)

### 4.2 Building from source code

Clone the repository：

```bash
git lfs install
git clone https://github.com/${your username}/starwhale.git
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

### 4.3 Modifying the code

When you modify the code, you need not to install python package(run `make install-sw` command) again. [.editorconfig](https://github.com/star-whale/starwhale/blob/main/.editorconfig) will be imported into the most IDE and code editors which helps maintain consistent coding styles for multiple developers.

### 4.4 Lint and Test

Run unit test, E2E test, mypy lint, flake lint and isort check in the `starwhale` directory.

```console
make client-all-check
```

## 5. Development environment for Standalone Instance

Cloud Instance is written in Java(backend) and React+TypeScript(frontend).
