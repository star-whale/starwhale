---
title: Instance
---

## Usage

```bash
swcli instance [OPTIONS] COMMAND [ARGS]...
```

## Summary

- Commands for instances management.
- For the cloud instance, you should log in first. Using the cloud instance alias name is the best practice.
- **Instance URI** in format:

  - `local`: standalone instance.
  - `[http(s)://]<hostname or ip>[:<port>]`: cloud instance with HTTP address.
  - `[cloud://]<cloud alias>`: cloud instance with an alias name, which can be configured in the instance login phase.

## All Sub-Commands

  |Command|Standalone|Cloud|
  |-------|----------|-----|
  |login|❌|✅|
  |logout|❌|✅|
  |select|✅|✅|
  |list|✅|✅|
  |info|✅|✅|

## Log in a cloud instance

```bash
swcli instance login [OPTIONS] [INSTANCE]
```

- `INSTANCE` argument uses `Instance URI` format. If ignore it, swcli will login current selected instance.
- Login operation will add configurations into `~/.config/starwhale/config.yaml`.
- Options:

    |Option|Alias Option|Required|Type|Default|Description|
    |------|--------|-------|-----------|-----|-----------|
    |`--username`||✅|String||Username|
    |`--password`||✅|String||Password|
    |`--alias`||✅|String||Starwhale instance alias name|

- Example:

    ```bash
    ❯ swcli instance login --username starwhale --password abcd1234 http://console.pre.intra.starwhale.ai --alias pre-k8s
    👨‍🍳 login http://console.pre.intra.starwhale.ai successfully!
    ```

## Log out a cloud instance

```bash
swcli instance logout [OPTIONS] [INSTANCE]
```

- `INSTANCE` argument uses the `Instance URI` format. If ignored, swcli will log out the currently selected instance.
- Today when logout a instance, swcli will clear the related configurations in `~/.config/starwhale/config.yaml`.
- Example:

    ```bash
    ❯ swcli instance logout
    Do you want to continue? [y/N]: y
    ```

## Select the default instance

```bash
swcli instance select INSTANCE
```

- The `INSTANCE` argument is required. It uses the `Instance URI` format.
- For cloud instances, you should log in first.
- Example:

    ```bash
    ❯ swcli instance select local
    👏 select local instance
    ❯ swcli instance select pre-k8s
    👏 select pre-k8s instance
    ```

## List instances

```bash
swcli instance list
```

- This command lists all instances.

## Show instance details

```bash
swcli instance info [INSTANCE]
```

- This command shows instance details.
- For cloud instances, you should log in first. If swcli cannot find the corresponding cloud instance, it will use the selected instance details as the output.
- Example:

    ```bash
    ❯ swcli instance info pre-k8s
    ❯ swcli instance info local
    ```
