---
title: On-Premises Quickstart
---

:::tip Disabling Automatic Logging
It is recommended to read [standalone quickstart](./standalone.md) first.
:::

## Installing On-Premises

Starwhale provides two ways to install an On-Premises instance in your private cluster:

- For Kubernetes: 
  - Standard Kubernetes Cluster: A pre-deployed Kubernetes cluster is required.
  - Minikube: You should have minikube and docker installed on your machine.
  - For more deployment details of Kubernetes, you can refer to this [doc](../cloud/helm-charts.md).
- For Bare Metal: You should have these components installed: docker(>=19.03), nvidia-docker-plugin(optional). You can get more details from this [doc](../cloud/ansible.md).

**In this tutorial, minikube is used instead of the standard Kubernetes cluster**
### Prerequisites
- Minikube 1.25+
- Helm 3.2.0+

There are standard installation tutorials for [minikube](https://minikube.sigs.k8s.io/docs/start/) and [helm](https://helm.sh/docs/intro/install/) here.

### Start minikube
```bash
minikube start
alias kubectl="minikube kubectl --"
```
For users in the mainland of China, please add these startup parameters：--image-mirror-country='cn' --image-repository=registry.cn-hangzhou.aliyuncs.com/google_containers

### Installation process    
```bash
helm repo add starwhale https://star-whale.github.io/charts
helm repo update
helm install my-starwhale starwhale/starwhale --version 0.2.0 -n starwhale --create-namespace --set minikube.enabled=true
```
After the installation is successful, the following prompt message appears:
```bash
NAME: my-starwhale
LAST DEPLOYED: Thu Jun 23 14:48:02 2022
NAMESPACE: starwhale
STATUS: deployed
REVISION: 1
NOTES:
******************************************
Chart Name: starwhale
Chart Version: 0.2.0
App Version: 0.2.0
Starwhale Image:
  - server: ghcr.io/star-whale/server:0.2.0
  - taskset: ghcr.io/star-whale/taskset:0.1.1

******************************************
Web Visit:
  - starwhale controller: http://console.minikube.local
  - minio admin: http://minio.pre.intra.starwhale.ai

Port Forward Visist:
  - starwhale controller:
    - run: kubectl port-forward --namespace starwhale svc/my-starwhale-controller 8082:8082
    - visit: http://localhost:8082
  - minio admin:
    - run: kubectl port-forward --namespace starwhale svc/my-starwhale-minio 9001:9001
    - visit: http://localhost:9001
  - mysql:
    - run: kubectl port-forward --namespace starwhale svc/my-starwhale-mysql 3306:3306
    - visit: mysql -h 127.0.0.1 -P 3306 -ustarwhale -pstarwhale

******************************************
Login Info:
- starwhale: u:starwhale, p:abcd1234
- minio admin: u:minioadmin, p:minioadmin

*_* Enjoy using Starwhale. *_*
```

Then keep checking the minikube service status until all pods are running.
```bash
watch -n 1 kubectl get pods -n starwhale
```

| NAME | READY | STATUS | RESTARTS | AGE |
|:-----|-------|--------|----------|-----|
|my-starwhale-agent-cpu-64699|2/2|Running|0|1m
|my-starwhale-controller-7d864558bc-vxvb8|1/1|Running|0|1m
|my-starwhale-minio-7d45db75f6-7wq9b|1/1|Running|0|2m
|my-starwhale-mysql-0|1/1|Running|0|2m

Make the Starwhale controller accessible locally with the following command:
```bash
kubectl port-forward --namespace starwhale svc/my-starwhale-controller 8082:8082
```
## Upload resources to the server
### pre-prepared resources
Before starting this tutorial, the following three resources should already exist on your machine：
- a starwhale model named mnist
- a starwhale dataset named mnist
- a starwhale runtime named pytorch-mnist

The above three resources are what we built in the [standalone tutorial](standalone.md).
### Use swcli to operate the remote server
First, log in to the server:
```bash
swcli instance login --username starwhale --password abcd1234 --alias server http://localhost:8082
```

Use the server instance as the default:
```bash
swcli instance select server
```

Create a project named 'project_for_mnist' and make it default:
```bash
swcli project create project_for_mnist
swcli project select project_for_mnist
```

Start copying the model, dataset, and runtime that we constructed earlier:
```bash
swcli model copy local/project/self/model/mnist/version/latest http://localhost:8082/
swcli dataset copy local/project/self/dataset/mnist/version/latest http://localhost:8082/
swcli runtime copy local/project/self/runtime/pytorch-mnist/version/latest http://localhost:8082/
```
## Use the web UI to run an evaluation
### login
Ok, let's use the username(starwhale) and password(abcd1234) to open the server web UI(http://localhost:8082/). 

Then, we will see the project named 'project_for_mnist' that we created earlier with swcli.
![project list](../img/ui-list-project.jpg)
Click the project name, you will see the model, runtime, and dataset uploaded in the previous step.
![model list](../img/ui-list-model.jpg)
![dataset list](../img/ui-list-dataset.jpg)
![runtime list](../img/ui-list-runtime.jpg)
### Create an evaluation job
![Create job](../img/ui-create-job.jpg)
![Create Job Workflow](../img/create-job-workflow.gif)
#### The job is completed and the results can be viewed
![Show Job Results](../img/ui-job-results.jpg)

**Congratulations! You have completed the evaluation process for a model.**