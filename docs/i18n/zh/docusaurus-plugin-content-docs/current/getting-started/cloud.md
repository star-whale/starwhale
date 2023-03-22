---
title: Starwhale Cloud入门指南
---

Starwhale Cloud运行在[AWS](https://cloud.starwhale.ai)和[阿里云](https://cloud.starwhale.cn)上。 这是两个相互独立的实例。 帐户和数据不共享。 你可以选择任何一个开始。

在开始之前，您需要先安装[Starwhale命令行工具(SWCLI)](../swcli)。

# 1. 注册Starwhale Cloud并创建您的第一个项目

您可以直接使用自己的GitHub或Google帐号登录，也可以注册一个新的帐号。如果您使用 GitHub 或 Google帐号登录，系统会要求您提供用户名。更多信息参见【Starwhale Cloud用户指南-创建账户】(../starwhale-cloud/index)

然后你可以创建一个新项目。在本教程中，我们将使用名称“demo”作为项目名称。

# 2. 在本地机器上构建数据集、模型和运行时

按照[Starwhale Standalone入门指南](standalone)中的步骤1到步骤4在本地机器上创建：

- 一个名为mnist的Starwhale模型
- 一个名为mnist的Starwhale数据集
- 一个名为pytorch的Starwhale运行时

# 3.登录云实例

```bash
swcli instance login --username <您的用户名> --password <您的密码> --alias swcloud https://cloud.starwhale.ai
# 如果您选择使用Starwhale Cloud CN，请将URL替换为https://cloud.starwhale.cn
```

# 4. 将数据集、模型和运行时复制到Starwhale Cloud

```bash
swcli model copy mnist/version/latest swcloud/project/demo
swcli dataset copy mnist/version/latest swcloud/project/demo
swcli runtime copy pytorch/version/latest swcloud/project/demo
```

# 5.使用 Web UI 运行评估

![console-create-job.gif](../img/console-create-job.gif)

**恭喜！ 您已完成Starwhale Cloud的入门指南。**
