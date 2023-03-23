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
  registry: "docker-registry.starwhale.cn"
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
        defaults:
storageSetting:
  - type: s3
    tokens:
      - bucket: starwhale
        ak: access_key
        sk: scret_key
        endpoint: http://mybucket.s3.region.amazonaws.com
        region: region of the service
        hugeFileThreshold: 10485760
        hugeFilePartSize: 5242880
  - type: minio
    tokens:
      - bucket: starwhale
        ak: access_key
        sk: scret_key
        endpoint: http://10.131.0.1:9000
        region: local
        hugeFileThreshold: 10485760
        hugeFilePartSize: 5242880


```

### Overwrite the image registry of a runtime

Tasks dispatched by the server are based on docker images. Pulling these images could be slow if your internet is not working well.
We offer a convenience to overwrite the registry of a runtime: Put the YAML below to system setting, the registry of images is overwritten to the one you specified at runtime.

```yaml
dockerSetting:
  registry: "docker-registry.starwhale.cn"
```

The priority of the system setting is the highest. Fine-grained setting is not provided yet.

### The `resourcePoolSetting`

The `resourcePoolSetting` allows you to manage your cluster in a group manner. It is currently implemented by K8S `nodeSelector`, you could label your machines in K8S cluster and make them a `resourcePool` in Starwhale.

### The `storageSetting`

The `storageSetting` allows you to manage the storages the server could access.

```yaml
storageSetting:
  - type: s3
    tokens:
      - bucket: starwhale # required
        ak: access_key # required
        sk: scret_key # required
        endpoint: http://s3.region.amazonaws.com # optional
        region: region of the service # required when endpoint is empty
        hugeFileThreshold: 10485760 #  bigger than 10MB will use multiple part upload
        hugeFilePartSize: 5242880 # MB part size for multiple part upload
  - type: minio
    tokens:
      - bucket: starwhale # required
        ak: access_key # required
        sk: scret_key # required
        endpoint: http://10.131.0.1:9000 # required
        region: local # optional
        hugeFileThreshold: 10485760 #  bigger than 10MB will use multiple part upload
        hugeFilePartSize: 5242880 # MB part size for multiple part upload
  - type: aliyun
    tokens:
      - bucket: starwhale # required
        ak: access_key # required
        sk: scret_key # required
        endpoint: http://10.131.0.2:9000 # required
        region: local # optional
        hugeFileThreshold: 10485760 #  bigger than 10MB will use multiple part upload
        hugeFilePartSize: 5242880 # MB part size for multiple part upload

```

Every `storageSetting` item has a corresponding implementation of `StorageAccessService` interface. Starwhale has four build-in implementations:

- `StorageAccessServiceAliyun` matches `type` in (`aliyun`,`oss`)
- `StorageAccessServiceMinio` matches `type` in (`minio`)
- `StorageAccessServiceS3` matches `type` in (`s3`)
- `StorageAccessServiceFile` matches `type` in (`fs`, `file`)

Each of the implementations has different requirements for `tokens`. `endpoint` is required when `type` in (`aliyun`,`minio`), `region` is required when `type` is `s3` and `endpoint` is empty. While `fs/file` type requires tokens has name `rootDir` and `serviceProvider`.
Please refer the code for more details.
