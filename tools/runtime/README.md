# Runtime oficial do L2 NewEra

Esta pasta contém a interface operacional canônica do ambiente local. O stack
oficial usa Docker Compose com PostgreSQL, migrations, LoginServer e
GameServer.

## Comandos

Windows:

```powershell
./StartL2NewEra.ps1 init
./StartL2NewEra.ps1 validate
./StartL2NewEra.ps1 up
./StartL2NewEra.ps1 status
./StartL2NewEra.ps1 logs -Follow
./StartL2NewEra.ps1 down
```

Linux/macOS:

```bash
./StartL2NewEra.sh init
./StartL2NewEra.sh validate
./StartL2NewEra.sh up
./StartL2NewEra.sh status
./StartL2NewEra.sh logs --follow
./StartL2NewEra.sh down
```

`up` e `restart` reconstruirão as imagens por padrão. Durante uma iteração que
não alterou código ou imagem, use `-SkipBuild` no PowerShell ou `--skip-build`
no shell.

`down` preserva os volumes. Não existe comando de reset nesta interface para
evitar a remoção acidental de contas e personagens. Um reset deliberado deve
ser feito manualmente, após backup, com o comando documentado no README do
Docker.

## Compatibilidade

Os wrappers `StartL2NewEra.*` da raiz delegam para estes scripts. Os launchers
`StartLogin_SemDashboard.*`, `StartGame_SemDashboard.*` e `StartBrproject.*`
continuam disponíveis somente para diagnóstico e execução legada diretamente
pelo JAR; eles não representam o ambiente oficial PostgreSQL/Docker.
