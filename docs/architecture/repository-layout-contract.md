# Contrato de organização do repositório

Esta é a classificação oficial para a reorganização gradual. Ela não autoriza
mover todas as pastas de uma vez: caminhos de runtime existentes são contratos
até que seus consumidores sejam migrados e testados.

| Área | Papel | Política |
|---|---|---|
| `modules/` | código Gradle do servidor e serviços | fonte canônica |
| `game/`, `login/` | configuração e dados de runtime | manter caminhos atuais durante a transição |
| `database/` | migrations, seeds e fixtures | única raiz canônica de banco |
| `deploy/` | Docker Compose e Dockerfiles oficiais | deployment reproduzível |
| `tools/runtime/` | comandos oficiais de operação | novos comandos entram aqui |
| `docs/` | documentação mantida | não misturar logs ou artefatos |
| `L2-NewEra-Frontend` (repositório separado) | frontend React público | build independente, Pages e sem segredos |
| `site/` | snapshot web legado | não tratar como fonte reproduzível |
| `Hwid/`, `bin/` | componentes legados/cliente | isolar, catalogar e só então extrair |
| `build/`, `.gradle/`, `.kotlin/`, `cache/`, `logs/` | estado gerado/runtime | ignorado e nunca versionado |

## Regras de migração

1. mapear cada consumidor antes de mover uma pasta;
2. mover uma fronteira por PR;
3. manter wrappers compatíveis quando necessário;
4. validar checkout limpo, Gradle, migrations, Compose e login do cliente;
5. remover compatibilidade somente após uma janela de depreciação documentada.

Gradle, `gradlew*`, `settings.gradle.kts`, `build.gradle.kts`, `AGENTS.md` e
`README.md` permanecem na raiz por convenção e por serem pontos de entrada do
projeto.
