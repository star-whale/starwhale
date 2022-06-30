---
title: Docker Compose for Starwhale Cloud
---

## Prerequisites

- Docker >= 19.03
- [Docker Compose Plugin](https://docs.docker.com/compose/install/compose-plugin/) >= 2.3
- x86-64 System(Linux, Windows and MacOS)

Docker Desktop is an easy-to-use way to run containers in your machine that includes all dependencies.

## Usage

```bash
docker compose up
```

Setup a Starwhale cloud environment in your single server. `compose.yaml` contains Starwhale base configuration. The override file `compose.override.yaml`, as its name implies, can contain configuration overrides for `server` and `agent` images. You can also override other configurations.