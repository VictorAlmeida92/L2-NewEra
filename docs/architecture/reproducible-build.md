# Build reproduzível e validação de clone limpo

Este documento fecha o último bloco da Fase 4.1. O checkout versionado é a
fonte do build; `libs/server.jar`, logs, bancos e caches são artefatos de
execução e não são pré-requisitos do repositório.

## Contrato do build

Em um clone limpo, o fluxo mínimo é:

```text
checkout
  -> Gradle compila :app-dist:jar
  -> testes e guardrails de arquitetura
  -> Dockerfile compila a distribuição no estágio build
  -> migrations inicializam PostgreSQL
  -> LoginServer e GameServer sobem
```

Os Dockerfiles oficiais (`deploy/docker/Dockerfile.*`) não copiam um JAR
pré-compilado do checkout. Cada imagem recompila a distribuição com o Gradle
e só então copia o resultado para a imagem de runtime. Isso evita que uma
imagem use código diferente daquele que está no commit.

## Validação local

Pré-requisitos: Java compatível com o Gradle do projeto, Docker Engine e
Docker Compose v2.

```powershell
.\gradlew.bat --no-daemon --no-parallel `
  :app-dist:jar `
  :db-migrate:test `
  :extensions-spi:test `
  :game-server-core:test `
  :mod-pix:test `
  :proxy:test `
  checkComponentBoundaries `
  checkRuntimeScripts `
  checkDatabaseSql `
  checkDatabaseConnectionBoundary `
  checkPersistenceConstructionBoundary

$env:POSTGRES_PASSWORD = "local-validation-only"
docker compose -f deploy/docker/docker-compose.yml config
.\StartL2NewEra.ps1 restart
.\StartL2NewEra.ps1 status
.\StartL2NewEra.ps1 logs -Services @("migrate", "login-server", "game-server")
```

Para interromper sem apagar o banco persistente:

```powershell
.\StartL2NewEra.ps1 down
```

Não usar `docker compose down -v` durante uma validação normal: essa opção
remove o volume PostgreSQL.

## Gate da CI

`.github/workflows/ci.yml` reproduz o fluxo em um runner limpo. Ele verifica:

1. ausência de `libs/server.jar` antes da compilação;
2. compilação e testes Gradle;
3. contrato do Compose;
4. build das imagens oficiais;
5. execução de PostgreSQL, migrations, LoginServer e GameServer;
6. mensagens mínimas de prontidão nos logs;
7. limpeza dos containers e volumes temporários da CI.

O smoke test não tenta autenticar um cliente gráfico. Login de cliente e
criação de personagem continuam sendo a validação manual necessária depois
que a CI confirma que os serviços e a persistência inicializaram.

## Publicação futura

O mesmo commit validado deve gerar uma imagem imutável identificada por SHA ou
tag de release. A EC2 deve consumir uma tag publicada, nunca `latest` sem
controle. O banco deve permanecer em volume gerenciado (RDS PostgreSQL em
produção), e a CI não deve carregar dados de desenvolvimento para esse banco.

Essa separação permite usar o workflow como base para publicação no GHCR e,
posteriormente, para deploy controlado na EC2 sem alterar o código do jogo.
