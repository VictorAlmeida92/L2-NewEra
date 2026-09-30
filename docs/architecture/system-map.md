# Mapa arquitetural — L2 NewEra

Status: baseline investigativo, atualizado em 2026-09-30.

Este documento descreve o que foi confirmado no código atual. Propostas futuras estão marcadas como **planejado** e não devem ser interpretadas como funcionalidades já integradas.

## 1. Visão de execução atual

```mermaid
flowchart LR
    Client[Cliente Lineage II]
    Proxy[Proxy Netty\nportas públicas 7777/2106]
    Login[LoginServer\nporta interna 2107]
    Game[GameServer\nporta interna 7778]
    API[Game API\nloopback 9080]
    DB[(PostgreSQL runtime\nDocker/RDS)]
    Site[Site/integrações]

    Client -->|login| Proxy
    Client -->|game| Proxy
    Proxy --> Login
    Proxy --> Game
    Site -->|HMAC HTTP interno| API
    API --> Game
    Login --> DB
    Game --> DB
```

O proxy é um processo separado. O launcher local expõe as portas públicas e encaminha para as portas internas dos servidores. A Game API é interna e não deve ser confundida com acesso direto do site ao banco.

## 2. Organização dos módulos

```mermaid
flowchart TD
    Dist[app-dist\nempacotamento]
    Core[game-server-core\nWorld, AI, combate, players, geodata]
    Login[login-server\nautenticação]
    API[game-api\nAPI HTTP interna]
    Proxy[proxy\nproxy TCP/HTTP]
    Mods[mods\nextensões first-party]
    SPI[extensions-spi\ncontratos e lifecycle]
    Packets[game-packets\nprotocolo]
    Network[game-network\nNetty/buffers]
    Model[game-model-api\ncontratos de domínio]
    Enums[game-enums\nenums compartilhados]
    Commons[commons\nutilitários, pool, crypto]
    Migrate[db-migrate\nmigrations Flyway]
    HPC[cluster-hpc\nprotótipo write-behind]

    Dist --> Core
    Dist --> Mods
    Mods --> SPI
    Mods --> Core
    Core --> Packets
    Core --> Network
    Core --> Model
    Core --> Commons
    Packets --> Network
    Packets --> Enums
    Network --> Model
    Model --> Enums
    SPI --> Commons
    Login --> Packets
    Login --> Commons
    API --> Model
    API --> Commons
    Migrate --> Commons
```

### Módulos principais

| Área | Módulos | Responsabilidade |
|---|---|---|
| Base | `commons`, `game-enums`, `game-model-api` | Tipos compartilhados, utilitários e contratos de domínio. |
| Protocolo | `game-network`, `game-packets` | Transporte Netty, buffers e pacotes do cliente. |
| Servidores | `login-server`, `game-server-core`, `proxy` | Login, mundo/jogabilidade e borda de rede. |
| Integração | `game-api`, `extensions-spi`, `mods` | API interna, extensões e funcionalidades opcionais. |
| Distribuição | `app-dist`, `db-migrate` | Empacotamento e migrações. |
| Experimental | `cluster-hpc`, `benchmarks-network` | Protótipos e benchmarks; não presumir integração no runtime. |

## 3. Composição do jogador e persistência

```mermaid
classDiagram
    class GameClient {
      +autosave inicial: 5 min
      +autosave recorrente: 15 min
      +disconnect/cleanup
    }
    class Player {
      +estado em memória
      +level/exp/sp
      +inventário/skills/quests
      +deleteMe()
    }
    class PlayerPersistence {
      +store()
      +storeCharBase()
      +storeSkills()
      +storeSubclass()
    }
    class ConnectionPool {
      +HikariCP
      +JDBC configurável
      +DatabaseDialect
    }
    class PostgreSQLRuntime {
      +schema versionado
      +migrations Flyway
      +volume persistente
    }

    GameClient --> Player : mantém sessão
    GameClient --> PlayerPersistence : agenda store()
    Player --> PlayerPersistence : logout/shutdown
    PlayerPersistence --> ConnectionPool : JDBC
    ConnectionPool --> PostgreSQLRuntime : transações JDBC
```

### Runtime oficial e legado SQLite

O runtime oficial de desenvolvimento e produção usa PostgreSQL. O Compose
mantém os dados em volume persistente e o módulo `db-migrate` aplica as
migrations canônicas de `database/migrations/postgresql` antes de iniciar os
servidores. MariaDB permanece como alvo de compatibilidade e SQLite é usado
somente por testes leves e ferramentas legadas.

No runtime SQLite anterior:

- `brproject.sqlite` é o banco persistente local.
- `brproject.sqlite-wal` é o write-ahead log em disco das transações recentes.
- `brproject.sqlite-shm` é um arquivo auxiliar de coordenação/índice do modo WAL.
- Eles não formam um banco temporário separado em memória.
- Uma mudança só pode ser recuperada depois de reinício se tiver sido efetivamente confirmada pelo SQLite. Dados que permaneciam somente no objeto `Player` não podem ser recuperados pelo WAL.

## 4. Explicação do rollback observado

```mermaid
sequenceDiagram
    participant C as Cliente
    participant P as Player em memória
    participant S as Autosave/Logout
    participant DB as Banco persistente

    C->>P: level-up para 80
    Note over P: estado 80 existe somente na sessão
    C--xS: processo encerrado antes do store()
    S--xDB: nenhuma transação de level 80
    C->>DB: novo login
    DB-->>C: último estado confirmado: nível 79
```

O código agenda o primeiro autosave em 300.000 ms e os seguintes em 900.000 ms. O desligamento normal também salva jogadores, mas um encerramento forçado, queda de energia ou kill do processo pode interromper esse caminho. PostgreSQL elimina a dependência dos arquivos locais WAL/SHM do SQLite, mas não salva automaticamente um estado que ainda existe somente no objeto `Player`.

### Próxima investigação recomendada

1. Logar `characterId`, nível em memória, nível lido do banco, início/fim e exceção de cada `store()`.
2. Confirmar se level-up, troca de classe e subclass chamam `store()` imediatamente.
3. Adicionar um teste de recuperação: 79 → 80 → restart gracioso e forçado → login.
4. Fazer o launcher esperar o encerramento completo dos processos.
5. Evitar salvar cada movimento/ataque; preferir eventos importantes e autosave confiável.

## 5. Divergência entre documentação e integração real

O README descreve `cluster-hpc` como pipeline de persistência write-behind ativo. A auditoria atual encontrou o módulo e seus testes/protótipos, mas não encontrou seu wiring no caminho de inicialização do GameServer. O runtime confirmado usa `GameClient` → `PlayerPersistence` → `ConnectionPool`.

Essa divergência deve ser resolvida em uma feature futura: ou integrar o write-behind com cobertura completa das entidades, ou ajustar a documentação para apresentá-lo apenas como experimental.

## 6. Arquitetura de produção proposta

```mermaid
flowchart TB
    Players[Jogadores TCP/UDP]
    WebUsers[Site e fórum HTTPS]
    DNS[Route 53]
    GA[Global Accelerator\nplanejado para tráfego de jogo]
    CDN[CloudFront/ALB\nplanejado para web]
    Public[Subnets públicas\nedge e componentes necessários]
    App[EC2 Linux\nLoginServer + GameServer + Proxy]
    Web[Web/Fórum\npreferencialmente separado quando crescer]
    DB[(RDS PostgreSQL\nsubnets privadas)]
    Logs[CloudWatch Logs/Metrics]
    Ops[Systems Manager\nSession Manager]
    Backup[S3/RDS backups]

    Players --> GA
    WebUsers --> DNS
    DNS --> CDN
    GA --> App
    CDN --> Web
    App --> DB
    Web -->|API interna| App
    App --> Logs
    Web --> Logs
    Ops -. acesso administrativo .-> App
    DB --> Backup
```

### Princípios

- Banco sem IP público; somente o security group da aplicação pode acessá-lo.
- Uma EC2 única para jogo, login e site é aceitável no primeiro ambiente, mas deve ter processos, limites e deploys separados.
- Para aproximadamente 250 jogadores, o tamanho da EC2 deve ser escolhido por teste de carga com bots e cenários de cidade/PvP, não por estimativa.
- PostgreSQL é o banco oficial. O deployment de produção deve usar RDS PostgreSQL ou uma instância PostgreSQL compatível, aplicar as migrations antes da aplicação e validar backup/restauração.
- Session Manager é preferível a manter SSH público; bastion só deve ser criado se houver uma necessidade operacional concreta.
- Shield Standard é a camada básica. Para ataques relevantes contra TCP/UDP, estudar Global Accelerator e Shield Advanced; WAF protege a superfície web, não substitui proteção do protocolo do jogo.

## 7. Fontes AWS para o estudo

- [EC2 compute optimized](https://docs.aws.amazon.com/ec2/latest/instancetypes/co.html)
- [Escolha e medição de instância EC2](https://docs.aws.amazon.com/AWSEC2/latest/UserGuide/instance-discovery.html)
- [Amazon RDS](https://docs.aws.amazon.com/AmazonRDS/latest/UserGuide/)
- [VPC e security groups](https://docs.aws.amazon.com/vpc/latest/userguide/vpc-security-groups.html)
- [Systems Manager Session Manager](https://docs.aws.amazon.com/systems-manager/latest/userguide/session-manager.html)
- [CloudWatch Agent](https://docs.aws.amazon.com/AWSEC2/latest/UserGuide/monitoring-scripts-intro.html)
- [Shield e aplicações TCP/UDP](https://docs.aws.amazon.com/waf/latest/developerguide/ddos-resiliency-example-tcp-udp.html)
