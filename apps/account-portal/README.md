# Account Portal

Frontend React/Vite do Lineage2 NewEra.

## Limites de segurança

Este projeto é somente o frontend público. Ele não acessa PostgreSQL e nunca
deve receber `GameApiSecret`, credenciais JDBC ou qualquer outro segredo.

O cadastro, login, verificação de e-mail e recuperação de senha serão
implementados por uma Account API/BFF server-side. Essa API chamará a Game API
interna por HMAC em uma rede privada.

## Desenvolvimento

Requer Node.js e pnpm:

```bash
pnpm install --frozen-lockfile
pnpm run dev
```

## Build do GitHub Pages

O workflow [`../../.github/workflows/account-portal-pages.yml`](../../.github/workflows/account-portal-pages.yml)
publica o resultado estático usando `VITE_BASE_PATH=/L2-NewEra/`.

No repositório GitHub, Pages deve estar configurado para publicar via GitHub
Actions. O domínio e a API devem usar HTTPS; CORS será limitado ao domínio
oficial do portal.
