# Binários opcionais do painel legado

Esta pasta não faz parte do runtime oficial Docker. Ela contém executáveis
Windows consumidos opcionalmente por `ProcessManagerService` quando o painel
Swing legado é usado:

| Arquivo | Papel | Origem conhecida |
|---|---|---|
| `cloudflared.exe` | Túnel HTTP opcional para o site | Cloudflare, versão 2026.8.2 |
| `site-native.exe` | Servidor web nativo legado | BrProject Site Ktor Native 1.0.0.0; fonte ausente deste repositório |

O LoginServer e o GameServer não dependem desses arquivos. Eles não são
copiados para as imagens oficiais e não devem ser usados como caminho de
produção sem revisão de procedência, atualização e assinatura.

Não há evidência suficiente neste repositório para afirmar que ele lê o
snapshot em `site/`; essa relação deve ser validada antes de qualquer extração.

Os hashes aprovados ficam em `CHECKSUMS.sha256` e são verificados pelo task
Gradle `checkComponentBoundaries`. Uma atualização deliberada precisa trocar o
binário, atualizar sua versão documentada e revisar o checksum no mesmo PR.
