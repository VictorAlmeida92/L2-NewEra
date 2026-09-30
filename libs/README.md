# Dependências vendorizadas

`libs/` contém duas categorias diferentes:

1. `server.jar` e `modules/`: artefatos gerados localmente e ignorados pelo Git;
2. JARs vendorizados ainda utilizados pelos scripts legados, Dockerfiles ou
   módulos que não possuem substituição confirmada em repositório Maven.

Os JARs especiais confirmados são:

- `ApiPix.jar`, usado por `mod-pix`;
- `DeepL.jar`, usado por `mod-crypta`;
- `Kamaloka.ext.jar` e `interface.ext.jar`, carregados pelo core/extensões.

Os demais incluem cópias de bibliotecas públicas também disponíveis via
Gradle. Elas não devem ser removidas em lote: os launchers diretos por JAR
constroem o classpath a partir desta pasta. A migração deve acontecer uma
dependência por vez, com teste do launcher legado e do Docker.

`CHECKSUMS.sha256` registra a integridade das dependências atuais, não sua
licença nem procedência. Esses dois aspectos ainda precisam de inventário antes
de uma distribuição pública de produção.
