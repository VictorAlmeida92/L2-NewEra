# L2 NewEra — Docker + PostgreSQL

Este é o ambiente local reproduzível da primeira etapa de persistência. Ele
sobe somente os componentes necessários para validar o jogo: PostgreSQL, Flyway,
LoginServer e GameServer. Site, API/HPC e painel operacional ficam fora deste
stack até terem uma imagem e um contrato de configuração próprios.

## Pré-requisitos

- Docker Desktop com WSL 2 habilitado;
- distribuição compilada durante o build Docker com `:app-dist:jar`.

Na raiz do projeto:

```bash
cp .env.example .env
```

Altere as senhas de `.env` antes de expor qualquer porta fora da máquina local.
As imagens compilam o JAR a partir do código-fonte durante o próprio build.

## Interface operacional recomendada

Na raiz do projeto, use os wrappers oficiais:

```powershell
./StartL2NewEra.ps1 validate
./StartL2NewEra.ps1 up
./StartL2NewEra.ps1 status
./StartL2NewEra.ps1 logs -Follow
./StartL2NewEra.ps1 down
```

Em Linux/macOS, os mesmos comandos estão disponíveis em
`./StartL2NewEra.sh`. Os comandos abaixo permanecem documentados como interface
direta do Compose para diagnóstico e automação.

## Subir

```bash
docker compose --env-file .env -f deploy/docker/docker-compose.yml build
docker compose --env-file .env -f deploy/docker/docker-compose.yml up -d
```

O serviço `migrate` executa as migrations PostgreSQL e registra o GameServer
local com `server_id=1`. O registro usa um HexID de desenvolvimento definido
por `GAME_SERVER_HEXID`; troque-o antes de qualquer implantação compartilhada.

Serviços e portas padrão:

| Serviço | Função | Porta |
|---|---|---:|
| `db` | PostgreSQL 16 | 5433 no host / 5432 no container |
| `migrate` | Flyway + seed do GameServer | — |
| `login-server` | LoginServer | 2106, 9014 |
| `game-server` | GameServer | 7777 |

## Diagnóstico

```bash
docker compose --env-file .env -f deploy/docker/docker-compose.yml ps
docker compose --env-file .env -f deploy/docker/docker-compose.yml logs -f login-server game-server
docker compose --env-file .env -f deploy/docker/docker-compose.yml logs migrate
```

Para validar a configuração sem iniciar:

```bash
docker compose --env-file .env -f deploy/docker/docker-compose.yml config
```

## Parar e resetar

```bash
docker compose --env-file .env -f deploy/docker/docker-compose.yml down
```

O volume PostgreSQL é persistente. Para apagar o banco local e recriar tudo do
zero, use `down -v`; isso remove personagens, contas e demais dados locais.
