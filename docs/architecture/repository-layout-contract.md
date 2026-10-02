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
| `tools/legacy/launcher-helpers/` | helpers dos launchers diretos legados | manter somente para compatibilidade; não usar como cache ou runtime oficial |
| `docs/` | documentação mantida | não misturar logs ou artefatos |
| `L2-NewEra-Frontend` (repositório separado) | frontend React público | build independente, Pages e sem segredos |
| `site/` | snapshot web legado | não tratar como fonte reproduzível |
| ferramentas opcionais do painel | executáveis externos como `site-native.exe` e `cloudflared.exe` | manter fora do repositório e configurar por diretório externo |
| repositório do client patch | material de distribuição do cliente | separado do servidor |
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
