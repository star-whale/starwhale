---
title: 关于.swignore文件
---

`.swignore` 文件用于在 Starwhale 数据集和模型的构建过程中忽略一些文件。

默认情况下，SWCLI将遍历目录树并包含所有.py/.sh/.yaml文件。对于模型，SWCLI还将包括在model.yaml中指定的模型。如果要排除某些文件，例如 .git 下的文件，则需要将它们的模式放在.swignore中。

# 文件格式

* swignore文件中的每一行指定一个匹配文件和目录的模式。
* 空行不匹配任何文件，因此它可以作为可读性的分隔符。
* 星号`*`匹配除斜杠以外的任何内容。
* 以`#`开头的行作为注释。

# 例子

这是[MNIST](../../examples/mnist)示例中使用的.swignore文件

```bash
venv
.git
.history
.vscode
.venv
```
