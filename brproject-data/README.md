# brproject-data (compatibilidade temporária)

Esta pasta mantém somente os exemplos de configuração usados pelos wrappers
legados. As migrations, seeds e fixtures oficiais foram consolidadas em
[`../database`](../database/README.md).

Game XML/HTML under `game/data/` remains in the main tree (or a future split).  
This pack owns:

| Path | Purpose |
|------|---------|
| `config-examples/` | Safe templates (no secrets) |

## Version policy

- Esta pasta será removida quando os exemplos forem movidos para a estrutura
  canônica de configuração.

## Sync examples into local runtime

```bash
./tools/sync-brproject-data.sh
```

Copies `config-examples` → `game/config/*.example` and `login/config/*.example` (never overwrites live secrets).

Para migrations e geração do baseline, consulte [`database/README.md`](../database/README.md).
