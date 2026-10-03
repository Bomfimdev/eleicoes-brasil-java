# Prompt para colar na nova guia do Cursor

Copie o bloco abaixo na primeira mensagem da sessão aberta em `D:\eleicoes-brasil-java`:

---

Continua o projeto **eleicoes-brasil-java** (adaptação do lucianookdp/eleicoes-brasil para **Spring Boot + Angular + PostgreSQL**).

**Escopo fechado** — seguir só o que está em `docs/PLANO.md`. Não reabrir stack, hospedagem ou mapa no meio da fase.

Antes de qualquer código:
1. Leia `AGENTS.md`
2. Leia `docs/PLANO.md` (fonte da verdade do escopo)
3. Respeite `.cursor/rules/projeto.mdc`

Contexto:
- Autor: Gabriel Bomfim (Java/Spring/Angular)
- Repo: https://github.com/Bomfimdev/eleicoes-brasil-java
- Original (só espelhar domínio/TSE): `D:\Eleicoes_2026`
- Princípios: browser nunca fala com o TSE; adapter isola bruto; `null` ≠ `0`; último dado bom em falha

Janelas de execução (o backend NÃO roda 24/7):
- J1: dom 04/10/2026 17h → seg 05/10/2026 22h (1º turno)
- J2: dom 25/10/2026 17h → seg 26/10/2026 22h (2º turno, só se houver)
- Horário de Brasília; o TSE começa a divulgar às 17h. Janelas configuráveis por `ELECTION_WINDOWS`
- Collector só coleta dentro da janela; fora dela é no-op
- Meta J1: slice mínimo ao vivo (collector `PRODUCTION` + API nacional + Angular nacional). Meta J2: MVP completo

Decisões travadas (resumo):
- Liquibase (não Flyway); collector no mesmo Spring da API; SSE (não WebSocket)
- SSE só avisa "nova versão"; dados via REST com `Cache-Control` curto
- Dev: Docker Compose local
- Deploy free: Cloudflare Pages + Render + Neon, API e banco na mesma região (EUA leste)
- Agendamento: cron-job.org, ping de `/api/health` a cada 5 min só dentro das janelas (primeiro ping às 16h)
- `/api/health` sem banco; `/api/ready` com banco
- Sem resultados por município no MVP (`COLLECT_CITY_RESULTS=false`); snapshot só quando muda; fotos fora do banco
- Segurança: CORS restrito, limite de SSE por IP, secrets em env, usuário de banco com permissão mínima
- Sem deploy durante uma janela
- Sem Oracle/always-on 24h: não é necessário
- Fora do MVP: mapa, histórico avançado, mobile, auth, WebSocket

Estado atual: **Fase 2 concluída** (collector + `TseAdapter2026` + fixtures classpath + janelas + rate limit/ETag). Online: API Render + Pages + Neon.

Próximo passo: **Fase 3 — API REST + SSE** (read-only nacional, cache curto, evento de versão via SSE, Swagger). Não pular para mapa/histórico.

Responda em português, simples e direto. Commits só se eu pedir.

---
