---
title: On-Premises Quickstart
---

:::tip Disabling Automatic Logging
It is necessary to practice [standalone quickstart](./standalone.md) first.
:::

## Installing On-Premises

Starwhale provides two ways to install in your private cluster:

- For Kubernetes: 
  - Standard Kubernetes Cluster: a pre-deployed kubernetes cluster is the precondition.
  - Minikube: only need to install minikube and docker on this machine to complete the deployment.
  - For more deployment details of Kubernetes, you can refer to the [doc](../cloud/helm-charts.md).
- For Bare Metal: host machine should install docker(>=19.03), nvidia-docker-plugin(optional).You can get more details from the [doc](../cloud/ansible.md).

**In this tutorial, minikube is used instead of the standard Kubernetes cluster, so that the service can be quickly built**
### Prerequisites
- Minikube 1.25+
- Helm 3.2.0+

There are standard installation tutorials for [minikube](https://minikube.sigs.k8s.io/docs/start/) and [helm](https://helm.sh/docs/intro/install/) here.

### start minikube
```bash
minikube start
alias kubectl="minikube kubectl --"
```
For users in mainland China, please add in the startup parameters：--image-mirror-country='cn' --image-repository=registry.cn-hangzhou.aliyuncs.com/google_containers

### Installation process    
```bash
helm repo add starwhale https://star-whale.github.io/charts
helm repo update
helm install my-starwhale starwhale/starwhale --version 0.2.0-beta.9 -n starwhale --create-namespace --set minikube.enabled=true
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
Chart Version: 0.2.0-beta.9
App Version: 0.2.0-beta.9
Starwhale Image:
  - server: ghcr.io/star-whale/server:0.2.0-beta.9
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

*_* Enjoy to use Starwhale Platform. *_*
```

Next, observe the minikube service status until all statuses are running.
```bash
watch -n 1 minikube kubectl  -- get pods -n starwhale
```

| NAME | READY | STATUS | RESTARTS | AGE |
|:-----|-------|--------|----------|-----|
|my-starwhale-agent-cpu-64699|2/2|Running|0|1m
|my-starwhale-controller-7d864558bc-vxvb8|1/1|Running|0|1m
|my-starwhale-minio-7d45db75f6-7wq9b|1/1|Running|0|2m
|my-starwhale-mysql-0|1/1|Running|0|2m

Well, according to the prompt, we execute the following command so that the service of the controller can be directly accessed locally
```bash
kubectl port-forward --namespace starwhale svc/my-starwhale-controller 8082:8082
```
## Upload resources to the server
### pre-prepared resources
Before starting this tutorial, the following three entities based on swcli should already exist on your machine：
- a starwhale model named mnist
- a starwhale dataset named mnist
- a starwhale runtime named pytorch-mnist

The above three resources are what we built in the [standalone tutorial](standalone.md).
### Use swcli to operate remote server
first, log in to the server
```bash
swcli instance login --username starwhale --password abcd1234 --alias server http://localhost:8082
```

Set the current instance to the server side
```bash
swcli instance select server
```

create a project named 'project_for_mnist'
```bash
swcli project create project_for_mnist
swcli project select project_for_mnist
```

Start uploading the starwhale model, dataset, and runtime we constructed earlier to the project.
```bash
swcli model copy local/project/self/model/mnist/version/latest http://localhost:8082/
swcli dataset copy local/project/self/dataset/mnist/version/latest http://localhost:8082/
swcli runtime copy local/project/self/runtime/pytorch-mnist/version/latest http://localhost:8082/
```
## Use the web UI to make an evaluation
### login
Ok, let's use the account(starwhale) and password(abcd1234) to open the server web UI(http://localhost:8082/). 

Then, we will see the project named 'project_for_mnist' that we created earlier with swcli.
![project list](../img/ui-list-project.jpg)
Click the project name to enter the details, you will see the uploaded starwhale model, runtime, dataset resource information.
![model list](../img/ui-list-model.jpg)
![dataset list](../img/ui-list-dataset.jpg)
![runtime list](../img/ui-list-runtime.jpg)
### Create an evaluation job
![Create job](../img/ui-create-job.jpg)
![Create Job Workflow](../img/create-job-workflow.gif)
#### The job is completed and the results are displayed
![Show Job Results](../img/ui-job-results.jpg)

**Congratulations! The evaluation process for a model has been completed.**
