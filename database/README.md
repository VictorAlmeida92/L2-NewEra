# Banco de dados

Esta é a raiz canônica para artefatos versionáveis do banco do L2 NewEra.
Estado de runtime, dumps locais e bancos SQLite não pertencem ao Git.

## Estrutura

```text
database/
├── migrations/
│   ├── postgresql/  # banco oficial
│   ├── mariadb/     # compatibilidade
│   └── sqlite/      # testes isolados e desenvolvimento legado
├── seeds/           # bootstrap determinístico e sem segredos
├── fixtures/        # dados exclusivos de testes
├── VERSION
├── BUILT_AT
└── LEGACY_SQL_MANIFEST.txt
```

## Migrations

O runner `:db-migrate` seleciona automaticamente a pasta pelo prefixo da URL
JDBC. PostgreSQL é o banco oficial do Compose.

```bash
./tools/migrate-db.sh
```

Para regenerar o baseline legado a partir de `tools/sql`:

```bash
python3 tools/generate_flyway_migrations.py
```

Essa geração não substitui revisão manual nem permite reescrever migrations já
aplicadas em ambientes persistentes.

## Seeds

O registro inicial do GameServer depende de `GAME_SERVER_HEXID` e
`GAME_SERVER_HOST`. Por isso ele é aplicado pelo `MigrateMain` após o Flyway,
usando o dialeto JDBC correto, e não por um SQL estático com segredo embutido.

## Fixtures

Fixtures são exclusivamente de testes. Dados de contas, personagens,
inventários ou credenciais reais não devem ser adicionados aqui.

## Compatibilidade legada

- `tools/sql/` continua temporariamente como fonte do gerador de baseline e do
  preparador legado.
- `config/examples/` é a raiz canônica dos templates de configuração; o script
  `tools/sync-config-examples.sh` os copia para o runtime quando necessário.
- Dumps locais devem ser informados explicitamente a
  `tools/sql/mysql_to_sqlite.py --input <arquivo>`.
