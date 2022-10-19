---
title: Server Instance superuser Password Reset
---
In case you forget THE superusers password, you could use the sql below to reset the password to `abcd1234`
```sql
update user_info set user_pwd='ee9533077d01d2d65a4efdb41129a91e', user_pwd_salt='6ea18d595773ccc2beacce26' where id=1
```
After that, you could login to the console and then change the password to what you really want.

---
title: System setting
---

You could customize system to make it easier to use by leverage of System setting.

## Overwrite the image registry of a runtime
Tasks dispatched by the server are based on docker images. Pulling these images could be slow if your internet is not working well. 
We offer a convenience to overwrite the registry of a runtime: Put the YAML below to system setting, the registry of images is overwritten to the one you specified at runtime.
```yaml
---
dockerSetting:
  registry: "docker-registry.starwhale.ai"
```
The priority of the system setting is the highest. Fine-grained setting is not provided yet.
