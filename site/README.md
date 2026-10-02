# Snapshot estático do site legado

Esta pasta é um snapshot estático, não um projeto-fonte reproduzível completo.
Ela contém `index.html`, imagens e alguns fragmentos TSX, mas não possui
manifesto de dependências, lockfile ou pipeline de build. Também não existe
referência no servidor que faça um executável externo `site-native.exe` ler esta pasta; uma
relação entre os dois não pode ser assumida apenas pelo conteúdo visual.

Uma implementação de site deve se comunicar com o servidor pelo contrato
HTTP/HMAC do módulo `modules/game-api`; ela não deve acessar diretamente o
banco do jogo. O GameServer funciona sem este snapshot e o Compose oficial não
sobe um serviço web.

Consequências:

- a pasta não é copiada para LoginServer, GameServer ou migrations;
- mudanças visuais aqui não recompilam nem comprovadamente alteram
  `site-native.exe`;
- não tratar os arquivos TSX isolados como fonte completa;
- antes de produção, o site deve ganhar build reproduzível e imagem própria,
  preferencialmente em fronteira de deployment separada.
