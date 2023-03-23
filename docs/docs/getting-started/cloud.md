---
title: Getting started with Starwhale Cloud
---

Starwhale Cloud is hosted on [AWS](https://cloud.starwhale.ai) and [Aliyun](https://cloud.starwhale.cn). These are two independent instances. Accounts and data are not shared. You can choose anyone you like.

You need to install the [Starwhale Client (SWCLI)](../swcli) at first.

## Sign Up for Starwhale Cloud and create your first project

You can either directly log in with your GitHub or Google account or sign up for an account. You will be asked for an account name if you log in with your GitHub or Google account. For more information, see the [Starwhale Cloud user guide - Create your account](../starwhale-cloud/index)

Then you can create a new project. In this tutorial, we will use the name `demo` for the project name.

## Build the dataset, model, and runtime on your local machine

Follow step 1 to step 4 in [Getting started with Starwhale Standalone](standalone) to create:

- a Starwhale model named mnist
- a Starwhale dataset named mnist
- a Starwhale runtime named pytorch

## Login to the cloud instance

```bash
swcli instance login --username <your account name> --password <your password> --alias swcloud https://cloud.starwhale.ai
# replace the URL with https://cloud.starwhale.cn if you choose to use Starwhale Cloud CN
```

## Copy the dataset, model, and runtime to the cloud instance

```bash
swcli model copy mnist/version/latest swcloud/project/demo
swcli dataset copy mnist/version/latest swcloud/project/demo
swcli runtime copy pytorch/version/latest swcloud/project/demo
```

## Run an evaluation with the web UI

![console-create-job.gif](../img/console-create-job.gif)

**Congratulations! You have completed the Starwhale Cloud Getting Started Guide.**
