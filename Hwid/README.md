# Material legado do patch do cliente

Apesar do nome, esta pasta não contém o módulo de proteção HWID do servidor.
Ela reúne somente material do cliente Interlude:

- `PatchSettings.ini`: configurações visuais do patch e IP do cliente;
- `Capturar.JPG`: imagem de referência/documentação.

Nenhum módulo Java/Kotlin, migration ou container consome esta pasta. O
subsistema HWID real do servidor vive em
`modules/game-server-core/src/main/java/ext/mods/protection/hwid` e persiste
seus dados pelas tabelas próprias.

Estes arquivos não são enviados às imagens Docker. Uma futura distribuição do
cliente deve movê-los para um repositório/pacote de client patch separado, sem
misturá-los com o deployment do servidor.
