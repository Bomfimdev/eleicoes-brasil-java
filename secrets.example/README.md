# Secrets locais (não versionados)

Copie esta pasta para `secrets/` na raiz do repo (já está no `.gitignore`).

Arquivos esperados:

- `neon.env` — JDBC + usuários do Neon
- `deploy.env` — URLs Render / Cloudflare / CORS
- `aplicacao.env` — espelho das env vars do Render

Nunca commitar `secrets/`.
