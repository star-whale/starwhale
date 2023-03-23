---
title: Configuration
---

You could customize your `swcli` by `swci config edit`:

```shell
swcli config edit
```

```yaml
current_instance: local
instances:
  local:
    current_project: self
    type: standalone
    updated_at: 2022-12-29 15:16:10 CST
    uri: local
    user_name: renyanda
link_auths:
  - ak: starwhale
    bucket: users
    connect_timeout: 10.0
    endpoint: http://10.131.0.1:9000
    read_timeout: 100.0
    sk: starwhale
    type: s3
storage:
  root: /home/renyanda/.starwhale
version: '2.0'

```

### link_auths

You could put `starwhale.Link` to your assets while the URI in the `Link` could be whatever(only s3 like or http is implemented) you need, such as `s3://10.131.0.1:9000/users/path`. However, `Link`s may need to be authed, you could config the auth info in `link_auths`

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

Items in `link_auths` will match the uri in `Link`s automatically. `s3` typed link_auth matching `Link`s by looking up `bucket` and `endpoint`
