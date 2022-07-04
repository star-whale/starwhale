---
title: Utilities
---

## Garbage collection

```bash
swcli gc [OPTIONS]
```

- This command purges removed entities in the standalone instance. Purged entities are not recoverable. The entities include projects, jobs, models, datasets, and runtimes.
- Only support **Standalone instance**.
- Options:

    |Option|Alias Option|Required|Type|Default|Description|
    |------|--------|-------|-----------|-----|-----------|
    |`--dry-run`||❌|Boolean|False|Dry-run cleanup garbage collection|
    |`--yes`||❌|Boolean|False|all confirms yes|

- Example:

    ```bash
    ❯ swcli gc --dry-run --yes
    🦌 project:0607, no objects to gc
    🦌 project:myproject2, no objects to gc
    ⚡ project:self, find 3 objects to cleanup...
    🚫 /home/liutianwei/.cache/starwhale/self/job/.recover/gn/gnsdcmtdmrqtemtfgeztgzddmr4xc2i
    🚫 /home/liutianwei/.cache/starwhale/self/model/.recover/mnist/gq/gq4wmmrrgazwknrtmftdgyjzmfwxczi.swmp
    🚫 /home/liutianwei/.cache/starwhale/self/workdir/model/mnist/gq/gq4wmmrrgazwknrtmftdgyjzmfwxczi
    🦌 project:myproject, no objects to gc
    ```

## UI

```bash
swcli ui [INSTANCE]
```

- This command will open the instance web UI in the browser.
- `INSTANCE` argument uses the `Instance URI` format. If omitted, the currently selected instance is used.
- Example:

    ```bash
    ❯ swcli ui  pre-k8s
    👏 try to open http://console.pre.intra.starwhale.ai
    ```
