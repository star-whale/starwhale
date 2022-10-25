---
title: Controller 系统设置
---

## 超级管理员密码重置

一旦你忘记了超级管理员的密码, 你可以通过下面的SQL语句将密码重置为 `abcd1234`

```sql
update user_info set user_pwd='ee9533077d01d2d65a4efdb41129a91e', user_pwd_salt='6ea18d595773ccc2beacce26' where id=1
```

重置后，你可以使用上述密码登录到console。 然后再次修改密码为你想要的密码。

## 系统设置

你可以在console上对系统设置进行更改，目前支持runtime的docker镜像源修改以及资源池的划分。

```yaml
---
dockerSetting:
  registry: "docker-registry.starwhale.ai" 
resourcePoolSetting:
  - name: pool1
    nodeSelector:
      kubernetes.io/hostname: host003-bj01
    resources:
      - name: nvidia.com/gpu # the resource name the cluster supported
        max: 2 # maximu request per task
        min: 1 # minium request per task
        defaults: 1 # the default value for the task
      - name: memory # the resource name the cluster supported
        max: 10240 # maximu request per task
        min: 1024 # minium request per task
        defaults: 2048 # the default value for the task
  - name: pool2
    nodeSelector:
      kubernetes.io/hostname: host005-bj01
    resources:
      - name: cpu
        max: 2
        min: 1
        defaults: 1

```

### 1. 修改runtime镜像源

Server下发的Tasks都是基于docker镜像实现的。如果你的网络不太好，可能拉镜像非常慢。我们提供了在console中设置镜像源的界面。把下面的YAML放到系统设置中，runtime的镜像源就会在运行时被覆盖。

系统设置中的镜像源是最高优先级的。目前还没有提供更细粒度的镜像源设置。

### 2. 修改资源池

资源池实现了集群机器分组的功能。用户在创建任务时可以通过选择资源池将自己的任务下发到想要的机器组中。资源池可以理解为K8S中的`nodeSelector`（当前也是这么实现的）。所以当你在K8S集群中给你的机器打上标签后，就可以在这里配置你的`resourcePool`了。
