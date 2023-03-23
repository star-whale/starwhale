---
title: Instance命令
---

## 基本信息

```bash
swcli instance [OPTIONS] COMMAND [ARGS]
```

instance命令提供基本的Instance管理，包括登陆、查看本地Instance列表等操作。已经登陆的Instance信息会存储在 `~/.config/starwhale/config.yaml` 文件中。Cloud Instance的所有操作都需要先进行登录。

**Instance URI**格式：

- `local`：standalone instance.
- `[http(s)://]<hostname or ip>[:<port>]`：通过http/https协议指定的Cloud Instance。
- `[cloud://]<cloud alias>`：通过登陆阶段指定的alias名字访问Cloud Instance。

instance包含如下子命令：

|命令|别名|Standalone|Cloud|
|---|---|----------|-----|
|login||❌|✅|
|logout||❌|✅|
|select|use|✅|✅|
|list|ls|✅|✅|
|info||✅|✅|

## 登陆Instance

```bash
swcli instance login [OPTIONS] [INSTANCE]
```

```bash
❯ swcli instance login --username starwhale --password abcd1234 http://console.pre.intra.starwhale.ai --alias pre-k8s
👨‍🍳 login http://console.pre.intra.starwhale.ai successfully!
```

`instance login` 命令用来登陆远端的Cloud Instance。`INSTANCE` 参数为Instance URI，如果不指定，则使用默认选择的Instance。登陆的时候任意选择用户名+密码或Token一种方式即可。成功登陆后，会将登陆信息写入到 `~/.config/starwhale/config.yaml` 文件中。登陆过期时间默认为1个月，可以在Cloud Instance中进行设置。需要注意的时，Standalone Instance无需登陆。

|参数|参数别名|必要性|类型|默认值|说明|
|------|--------|-------|-----------|-----|-----------|
|`--username`||❌|String||登陆用户名|
|`--password`||❌|String||登陆密码|
|`--token`||❌|String||登陆Token，可以在Cloud Instance的Web页面中获取|
|`--alias`||✅||String||instance别名|

## 登出Instance

```bash
swcli instance logout [INSTANCE]
```

`instance logout` 命令用来退出已登录的Instance。`INSTANCE` 参数是Instance URI。当 `INSTANCE` 参数不指定时，会退出默认选定的Instance。需要注意的时，Standalone Instance无需登出。

## 查看Instance详细信息

```bash
swcli instance info [INSTANCE]
```

`instance info` 命令输出Instance的详细信息。`INSTANCE` 参数是Instance URI。当 `INSTANCE` 参数不指定时，会退出默认选定的Instance。

![instance-info.png](../../../img/instance-info.png)

## 展示Instance列表

```bash
swcli instance list
```

`instance list` 命令展示Standalone Instance和所有已经登陆的Cloud Instance。

![instance-list.png](../../../img/instance-list.png)

## 选择默认Instance

```bash
swcli instance select INSTANCE
```

`instance select` 命令选择本机默认的Instance。在其他种类的URI中，如果省略instance字段，就会根据此处select的instance进行填充。`INSTANCE` 参数为Instance URI。

```bash
❯ swcli instance select local
👏 select local instance
❯ swcli instance select pre-k8s
👏 select pre-k8s instance
```
