# Ferramentas do repositório

As ferramentas são agrupadas por responsabilidade:

| Caminho | Estado | Uso |
|---|---|---|
| `runtime/` | oficial | operação do Docker local |
| `sql/` | legado compatível | fontes SQL antigas e conversor; não é raiz canônica de migrations |
| `network/` | auxiliar Windows | configuração local do cliente/LAN |
| `phase3/`, `phase4/` | histórico de refatoração | evidências e scripts one-shot; não executar em runtime |
| `migrate-db.sh` | compatibilidade | execução direta do runner de migrations |
| `install_db.*` | legado | instalação anterior ao Compose; preferir `runtime/` |
| `sync-brproject-data.sh` | transição | sincronização dos exemplos de configuração legados |

Novos comandos operacionais devem entrar em `runtime/`. Novas migrations,
seeds e fixtures devem entrar em `database/`, nunca em `tools/sql/`.
