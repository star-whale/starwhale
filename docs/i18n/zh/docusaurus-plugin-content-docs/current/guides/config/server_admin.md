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

你可以在console上对系统设置进行更改，目前支持runtime的docker镜像源修改。

### 1. 修改runtime镜像源

Server下发的Tasks都是基于docker镜像实现的。如果你的网络不太好，可能拉镜像非常慢。我们提供了在console中设置镜像源的界面。把下面的YAML放到系统设置中，runtime的镜像源就会在运行时被覆盖。

```yaml
dockerSetting:
  registry: "docker-registry.starwhale.ai"
```

系统设置中的镜像源是最高优先级的。目前还没有提供更细粒度的镜像源设置。
