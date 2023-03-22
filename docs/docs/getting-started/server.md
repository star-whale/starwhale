---
title: Getting started with Starwhale Server
---

# 1. Install Starwhale Server

To install Starwhale Server, see the [installation guide]

# 2. Create your first project

## 2.1 Login to the server

Open your browser and enter your server's URL in the address bar. Login with your username(starwhale) and password(abcd1234).

![console-artifacts.gif](../img/console-artifacts.gif)

## 2.2 Create a new project

# 3. Build the dataset, model, and runtime on your local machine

Follow step 1 to step 4 in [Getting started with Starwhale Standalone](standalone) to create:

- a Starwhale model named mnist
- a Starwhale dataset named mnist
- a Starwhale runtime named pytorch

# 4. Copy the dataset, the model, and the runtime to the server

```bash
swcli instance login --username <your username> --password <your password> --alias server <Your Server URL>
swcli model copy mnist/version/latest server/project/demo
swcli dataset copy mnist/version/latest server/project/demo
swcli runtime copy pytorch/version/latest server/project/demo
```

# 3. Use the Web UI to run an evaluation

Navigate to the "demo" project in your browser and create a new one.

![console-create-job.gif](../img/console-create-job.gif)

**Congratulations! You have completed the Starwhale Server Getting Started Guide.**
