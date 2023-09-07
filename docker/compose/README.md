---
title: Docker Compose for Starwhale server
---

## Prerequisites

- Docker >= 19.03
- [Docker Compose Plugin](https://docs.docker.com/compose/install/compose-plugin/) >= 2.3
- x86-64 System(Linux)

## Usage

Start up the server
```bash
./star.sh --global-ip ${your_accessible_ip}
```

Stop the server
```bash
./stop.sh
```

Setup a StarWhale server instance. `compose.yaml` contains Starwhale base configuration. The override file `compose.override.yaml`, as its name implies, can contain [configuration overrides](https://docs.docker.com/compose/reference/#specifying-multiple-compose-files) for `compose.yaml`.
