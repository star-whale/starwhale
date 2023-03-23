---
title: 配置文件
---

Standalone Instance 是安装在用户的笔记本或开发服务器上，以Linux/Mac用户为粒度进行隔离。用户通过pip命令安装starwhale python package并执行任意swcli命令后，就可以在 `~/.config/starwhale/config.yaml` 中查看该用户的Starwhale 配置。**绝大多数情况加用户不需要手工修改config.yaml文件**。

`~/.config/starwhale/config.yaml` 文件权限为 0o600，由于里面存有密钥信息，不建议用户修改该文件权限。

## config.yaml 说明

典型的config.yaml文件内容如下，表示当前默认默认Instance为local，管理4个Instance，其中cloud-cn/cloud-k8s/pre-k8s三个为Cloud Instance，local为Standalone Instance。Standalone本地存储的根目录为 `/home/liutianwei/.starwhale` ：

```yaml
current_instance: local
instances:
  cloud-cn:
    sw_token: ${TOKEN}
    type: cloud
    updated_at: 2022-09-28 18:41:05 CST
    uri: https://cloud.starwhale.cn
    user_name: starwhale
    user_role: normal
  cloud-k8s:
    sw_token: ${TOKEN}
    type: cloud
    updated_at: 2022-09-19 16:10:01 CST
    uri: http://cloud.pre.intra.starwhale.ai
    user_name: starwhale
    user_role: normal
  local:
    current_project: self
    type: standalone
    updated_at: 2022-06-09 16:14:02 CST
    uri: local
    user_name: liutianwei
  pre-k8s:
    sw_token: ${TOKEN}
    type: cloud
    updated_at: 2022-09-19 18:06:50 CST
    uri: http://console.pre.intra.starwhale.ai
    user_name: starwhale
    user_role: normal
storage:
  root: /home/liutianwei/.starwhale
version: '2.0'
```

|参数|说明|类型|默认值|是否必须|
|---|----|---|-----|-------|
|current_instance|默认使用的instance名字，一般用 `swcli instance select` 命令设置|String|self|是|
|instances|管理的Instances，包括Standalone Instance和Cloud Instance，至少会有Standalone Instance(名称为local)，Cloud Instance有一个或多个，一般用 `swcli instance login` 登陆一个新的instance，`swcli instance logout` 退出一个instance|Dict|Standalone Instance，名称为local|是|
|instances.{instance-alias-name}.sw_token|登陆Token，只对Cloud Instance生效，后续swcli对Cloud Instance进行操作时都会使用该Token。需要注意Token有过期时间，默认1个月，可以在Cloud Instance中进行设置|String||Cloud-是，Standalone-否|
|instances.{instance-alias-name}.type|instance类型，目前只能填写 `cloud` 或 `standalone` |Choice[String]||是|
|instances.{instance-alias-name}.uri|对于Cloud Instance，uri是http/https地址，对于Standalone Instance，uri是 `local` |String||是|
|instances.{instance-alias-name}.user_name|用户名|String||是|
|instances.{instance-alias-name}.current_project|当前Instance下默认的Project是什么，在URI的表述中会作为project字段进行默认填充，可以通过 `swcli project select` 命令进行设置|String||是|
|instances.{instance-alias-name}.user_role|用户角色|String|normal|是|
|instances.{instance-alias-name}.updated_at|该条Instance配置更新时间|时间格式字符串||是|
|storage|与本地存储相关的设置|Dict||是|
|storage.root|Standalone Instance本地存储的根目录。通常情况下，当home目录空间不足，手工把数据文件移动到其他位置时，可以修改该字段|String|`~/.starwhale`|是|
|version|config.yaml的版本，目前仅支持2.0|String|2.0|是|

您可以通过`swci config edit`来修改配置

```shell
swcli config edit
```

## Standalone Instance的文件存储结构

${storage.root} 目录中存储了Starwhale Standalone Instance所有的用户数据，包括Project、Runtime、Model、Dataset、Evaluation等用户直接感知的数据，也包括ObjectStore、DataStore等Starwhale后台实现的存储。具体说明如下：

```console
+-- ${storage.root}
|  +-- .objectstore     --> 存储数据集chunk文件的简单存储，使用blake2b hash算法
|  |   +-- blake2b      --> hash算法名称
|  |   |  +-- 00        --> hash2位前缀
|  |   |  |  +-- 0019ad58...   --> object文件，文件名是文件内容的hash值
|  |   |  +-- 05
|  +-- .datastore       --> 基于pyarrow的列式存储
|  |   +-- project
|  |   |   +-- self     --> 按照project名称进行分类存储
|  |   |   |  +-- dataset --> 数据集相关的datastore存储，一般用来存储数据集的索引信息
|  |   |   |  +-- eval    --> 模型评测结果存储
|  +-- .recover   --> 软删除某个project的存储目录，可以用 `swcli project recover` 进行恢复
|  +-- .tmp       --> Dataset/Model/Runtime 构建过程中临时目录
|  +-- myproject  --> 用户创建的project，所有myproject信息都存储在该目录
|  +-- self       --> Standalone Instance自动创建的project
|  |   +-- dataset      --> swds数据集存储目录
|  |   +-- evaluation   --> 模型评测配置文件、日志等存储目录
|  |   +-- model        --> swmp模型包存储目录
|  |   +-- runtime      --> swrt环境包存储目录
|  |   +-- workdir      --> 解压、复原包文件的目录
|  |   |   +-- model    --> swmp解压后的目录
|  |   |   +-- runtime  --> swrt解压后的目录，若进行runtime restore操作，生成的venv或conda隔离环境，也会存放在该目录中
```

## link_auths

有时候您可能需要用到`starwhale.Link`来存储一些信息。理论上，`Link`里面的URI可以是任意的合法URI（星鲸目前只支持S3协议族和HTTP），比如`s3://10.131.0.1:9000/users/path`。然而，有些`Link`是需要鉴权才能访问的。`link_auths`就是用来存放这些鉴权信息的。

```yaml
link_auths:
  - type: s3
    ak: starwhale
    bucket: users
    region: local
    connect_timeout: 10.0
    endpoint: http://10.131.0.1:9000
    read_timeout: 100.0
    sk: starwhale
```

`link_auths` 里面的每一条都会自动匹配您的URI。 目前`S3`类型的鉴权信息通过`bucket` 和 `endpoint`来匹配URI。
