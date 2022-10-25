---
title: Controller Admin Settings
---

## Superuser Password Reset

In case you forget the superusers password, you could use the sql below to reset the password to `abcd1234`

```sql
update user_info set user_pwd='ee9533077d01d2d65a4efdb41129a91e', user_pwd_salt='6ea18d595773ccc2beacce26' where id=1
```

After that, you could login to the console and then change the password to what you really want.

## System setting

You could customize system to make it easier to use by leverage of `System setting`. Here is an example below:

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

### 1. Overwrite the image registry of a runtime

Tasks dispatched by the server are based on docker images. Pulling these images could be slow if your internet is not working well.
We offer a convenience to overwrite the registry of a runtime: Put the YAML below to system setting, the registry of images is overwritten to the one you specified at runtime.

```yaml
dockerSetting:
  registry: "docker-registry.starwhale.ai"
```

The priority of the system setting is the highest. Fine-grained setting is not provided yet.

### 2. The `resourcePoolSetting`

The `resourcePoolSetting` allows you to manage your cluster in a group manner. It is currently implemented by K8S `nodeSelector`, you could label your machines in K8S cluster and make them a `resourcePool` in Starwhale.
