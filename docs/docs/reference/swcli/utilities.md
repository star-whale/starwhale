---
title: Utility Commands
---

## swcli gc {#gc}

```bash
swcli [GLOBAL OPTIONS] gc [OPTIONS]
```

`gc` clears removed projects, models, datasets, and runtimes according to the internal garbage collection policy.

| Option | Required | Type | Defaults | Description |
| --- | --- | --- | --- | --- |
| `--dry-run` | ❌ | Boolean | False | If true, outputs objects to be removed instead of clearing them. |
| `--yes` | ❌ | Boolean | False | Bypass confirmation prompts. |
