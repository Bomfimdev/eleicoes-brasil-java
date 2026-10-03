# Eleicoes Brasil - Java / Angular

Adaptacao do projeto [eleicoes-brasil](https://github.com/lucianookdp/eleicoes-brasil) para a stack **Java (Spring Boot) + Angular + PostgreSQL**.

> Projeto independente. Nao e um servico oficial da Justica Eleitoral.

## Stack

- **Backend:** Spring Boot / Java 17 - REST + SSE + collector
- **Frontend:** Angular 19
- **Banco:** PostgreSQL 16 + Liquibase
- **Infra local:** Docker Compose
- **Deploy MVP (free):** Cloudflare Pages + Render + Neon (ver `docs/PLANO.md`)

## Estrutura

```
backend/     Spring Boot (API + futuro collector)
frontend/    Angular
docs/        Plano, prompt de sessao
docker/      Init do Postgres (roles)
```

## Como rodar (dev)

```bash
# 1) Postgres (recria volume se os roles mudarem)
docker compose down -v
docker compose up -d postgres

# 2) API
cd backend
.\mvnw.cmd spring-boot:run

# 3) Angular (outro terminal)
cd frontend
npm start
```

Credenciais locais padrao:

| Uso | Usuario | Senha |
|-----|---------|-------|
| Admin Docker | `eleicoes` | `eleicoes` |
| Liquibase (DDL) | `eleicoes_migrator` | `eleicoes_migrator` |
| App (DML) | `eleicoes_app` | `eleicoes_app` |

- API health (sem banco): http://localhost:8080/api/health
- API ready (com banco): http://localhost:8080/api/ready
- Front: http://localhost:4200

## Contexto para o Cursor

Ao abrir este repo em uma **nova guia/janela**, leia:

1. `AGENTS.md`
2. `docs/PLANO.md`
3. Cole o prompt de `docs/PROMPT-NOVA-SESSAO.md`

Referencia do original (TypeScript): `D:\Eleicoes_2026`

## Creditos

Arquitetura e dominio inspirados em [lucianookdp/eleicoes-brasil](https://github.com/lucianookdp/eleicoes-brasil).
