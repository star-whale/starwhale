---
title: On-Premises Quickstart
---

:::tip Disabling Automatic Logging
We recommend you read and practice [standalone quickstart](./standalone.md) first.
:::

## Installing Standalone

Starwhale provides two ways to install in your private cluster:

- For Kubernetes: a pre-deployed kubernetes cluster is the precondition.
- For Bare Metal: host machine should install docker(>=19.03), nvidia-docker-plugin(optional).

### Kubernetes

We need Kubernetes 1.19+ and Helm 3.2.0+.

```bash
helm repo add starwhale https://star-whale.github.io/charts
helm repo update
helm install starwhale starwhale/starwhale -n starwhale --create-namespace
```

You can get more details from the [doc](../cloud/helm-charts.md).

### Bare Metal