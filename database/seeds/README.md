# Seeds

Seeds versionados devem ser determinísticos, idempotentes e não conter
credenciais reais.

O seed atual do registro do GameServer é dinâmico e permanece implementado em
`br.project.db.MigrateMain`, pois recebe o HexID e o host pelo ambiente e usa o
dialeto JDBC selecionado.
