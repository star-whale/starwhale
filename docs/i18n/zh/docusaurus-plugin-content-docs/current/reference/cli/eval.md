---
title: 模型评测命令
---

## 基本信息

```bash
swcli [GLOBAL OPTIONS] eval [OPTIONS] COMMAND [ARGS]...
```

eval命令提供适用于Standalone Instance和Cloud Instance的模型评测管理，包括运行、查看等管理功能。在Standalone Instance中，eval命令使用本地磁盘存储模型评测过程和结果文件。eval命令通过HTTP API对Cloud Instance对象进行操作。

**Job URI** 格式: `[<Project URI>/job]<job id>`.