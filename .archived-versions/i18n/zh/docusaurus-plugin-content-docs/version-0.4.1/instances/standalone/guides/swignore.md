---
title: .swignore文件
---

## 1. 用途

`.swignore` 文件与 `.gitignore`, `.dockerignore` 等文件类似，都是用来定义忽略某些文件或文件夹。`.swignore` 文件主要应用在Starwhale的构建过程中：

- Starwhale Dataset构建：当 `.swignore` 目录与 `dataset.yaml` 在相同目录中，执行 `swcli dataset build` 命令制作swds虚拟包时，可以忽略某些文件的拷贝。默认构建数据集，会拷贝 `.py/.sh/.yaml` 文件。
- Starwhale Model构建：当 `.swignore` 目录与 `model.yaml` 在相同目录中，执行 `swcli model build` 命令制作swmp包文件时，可以忽略某些文件的拷贝。默认构建模型包，会拷贝 `.py/.sh/.yaml`和model.yaml中定义的model、config字段包含的文件。

Starwhale Runtime 构建暂不支持 `.swignore` 文件。

## 2. 示例

mnist的 `.swignore` 文件示例如下：

```text
venv
.git
.history
.vscode
.venv
```

支持注释，在行首使用 `#` 标识。

```text
# this is a comment
```

支持wildcard的表达。

```text
*.jpg
*.png
```
