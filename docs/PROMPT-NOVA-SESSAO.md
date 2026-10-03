# Prompt para colar na nova guia do Cursor

Copie o bloco abaixo na primeira mensagem da sessão aberta em `D:\eleicoes-brasil-java`:

---

Continua o projeto **eleicoes-brasil-java** (fork/adaptação do lucianookdp/eleicoes-brasil para **Spring Boot + Angular + PostgreSQL**).

Antes de qualquer código:
1. Leia `AGENTS.md`
2. Leia `docs/PLANO.md`
3. Respeite `.cursor/rules/projeto.mdc`

Contexto:
- Autor: Gabriel Bomfim (Java/Spring/Angular — sistemas de governo)
- Repo: https://github.com/Bomfimdev/eleicoes-brasil-java
- Original de referência (só espelhar domínio/TSE): `D:\Eleicoes_2026`
- Princípio: browser nunca fala com o TSE; adapter isola formato bruto; `null` ≠ `0`

Estado atual (Fase 0):
- Scaffold backend Spring Boot 4 / Java 17 em `backend/`
- Scaffold Angular 19 em `frontend/`
- Docker Compose com Postgres
- `GET /api/health` + shell Angular checando a API

Próximo passo: **Fase 1 — domínio e banco** (entidades + Flyway alinhados ao modelo do original). Não pular para mapa/TSE real ainda.

Responda em português, simples e direto. Commits só se eu pedir.

---
