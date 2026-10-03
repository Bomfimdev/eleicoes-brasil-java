# Eleições Brasil — Java / Angular

Adaptação do [lucianookdp/eleicoes-brasil](https://github.com/lucianookdp/eleicoes-brasil) para **Spring Boot + Angular + PostgreSQL**.

> **Disclaimer:** projeto independente. **Não é** serviço oficial da Justiça Eleitoral.  
> Fonte dos dados: divulgação pública do TSE (via collector no backend — o navegador nunca fala com o TSE).

## Stack

| Camada | Tecnologia |
|--------|------------|
| API + collector + SSE | Spring Boot 4 / Java 17 |
| Frontend | Angular 19 (SPA, mobile-first) |
| Banco | PostgreSQL 16 + Liquibase |
| Local | Docker Compose |
| Deploy MVP | Cloudflare Pages + Render + Neon |

## URLs públicas

- Front: https://eleicoes-brasil.pages.dev  
- API: https://eleicoes-brasil-api.onrender.com  
- Swagger: https://eleicoes-brasil-api.onrender.com/swagger-ui.html  

Operação detalhada (cron, REPLAY, archive, runbook): **[docs/OPERACAO.md](docs/OPERACAO.md)**

## Estrutura

```
backend/     Spring Boot (API + collector + REPLAY)
frontend/    Angular
docs/        PLANO, OPERACAO, prompt de sessão
docker/      Postgres init + nginx
scripts/     export-archive (modo arquivado)
```

## Como rodar (dev)

```bash
# Postgres
docker compose up -d postgres

# API
cd backend
./mvnw spring-boot:run   # Windows: .\mvnw.cmd spring-boot:run

# Angular
cd frontend
npm start
```

- API: http://localhost:8080/api/health · /api/ready · /swagger-ui.html  
- Front: http://localhost:4200  

Credenciais locais:

| Uso | Usuário | Senha |
|-----|---------|-------|
| Admin Docker | `eleicoes` | `eleicoes` |
| Liquibase | `eleicoes_migrator` | `eleicoes_migrator` |
| App | `eleicoes_app` | `eleicoes_app` |

### Compose full stack

```bash
docker compose up -d --build postgres api
cd frontend && npm ci && npm run build -- --configuration production && cd ..
docker compose --profile full up -d frontend
# http://localhost:8088
```

## Modos

`APP_MODE=DEVELOPMENT|SIMULATION|PRODUCTION|REPLAY` — ver `docs/OPERACAO.md`.

## Cursor / agente

1. `AGENTS.md`  
2. `docs/PLANO.md`  
3. Prompt em `docs/PROMPT-NOVA-SESSAO.md`  

Referência TypeScript (só espelhar): `D:\Eleicoes_2026`

## Créditos

Arquitetura e domínio inspirados em [lucianookdp/eleicoes-brasil](https://github.com/lucianookdp/eleicoes-brasil).
