# Ferramentas do repositório

As ferramentas são agrupadas por responsabilidade:

| Caminho | Estado | Uso |
|---|---|---|
| `runtime/` | oficial | operação do Docker local |
| `legacy/launcher-helpers/` | compatibilidade | helpers dos launchers diretos antigos; não é estado de runtime |
| `sql/` | legado compatível | fontes SQL antigas e conversor; não é raiz canônica de migrations |
| `network/` | auxiliar Windows | configuração local do cliente/LAN |
| `phase3/`, `phase4/` | histórico de refatoração | evidências e scripts one-shot; não executar em runtime |
| `migrate-db.sh` | compatibilidade | execução direta do runner de migrations |
| `install_db.*` | legado | instalação anterior ao Compose; preferir `runtime/` |
| `sync-config-examples.sh` | compatibilidade | sincronização dos exemplos canônicos de configuração |

Novos comandos operacionais devem entrar em `runtime/`. Novas migrations,
seeds e fixtures devem entrar em `database/`, nunca em `tools/sql/`.
