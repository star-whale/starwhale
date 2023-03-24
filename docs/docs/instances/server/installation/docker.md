---
title: Install Starwhale Server with Docker
---

## Prerequisites

* A running Kubernetes 1.19+ cluster to run tasks.
* A running MySQL 8.0+ instance to store metadata.
* A S3-compatible object storage to save datasets, models, and others.

Please make sure pods on the Kubernetes cluster can access the port exposed by the Starwhale Server installation.

## Prepare an env file for Docker

Starwhale Server can be configured by environment variables.

An env file template for Docker is [here](../config/starwhale_env). You may create your own env file by modifying the template.

## Prepare a kubeconfig file

The kubeconfig file is used for accessing the Kubernetes cluster. For more information about kubeconfig files, see the [Official Kubernetes Documentation](https://kubernetes.io/docs/concepts/configuration/organize-cluster-access-kubeconfig/).

If you have a local `kubectl` command-line tool installed, you can run `kubectl config view` to see your current configuration.

## Run the Docker image

```bash
docker run -it -d --name starwhale-server -p 8082:8082 \
        --restart unless-stopped \
        --mount type=bind,source=<path to your kubeconfig file>,destination=/root/.kube/config,readonly \
        --env-file <path to your env file> \
        ghcr.io/star-whale/server
