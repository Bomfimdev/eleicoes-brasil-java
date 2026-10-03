# Plano de execução (escopo fechado)

Decisões abaixo valem até o MVP. Não abrir discussão de stack no meio da implementação.

---

## Janelas de execução

O backend **não roda 24/7**. Só precisa estar no ar durante a apuração. Fora das janelas ele dorme e não custa nada.

| Janela | Liga | Desliga | Observação |
|--------|------|---------|------------|
| J1 | dom 04/10/2026 17h | seg 05/10/2026 22h | 1º turno |
| J2 | dom 25/10/2026 17h | seg 26/10/2026 22h | 2º turno, **só se houver** (confirmar na noite de 04/10) |

Horários em Brasília. O TSE começa a divulgar os resultados às 17h, simultaneamente para todo o país.

> Nota: a J2 estava como 11/10. O calendário do TSE (Res. 23.760/2026) marca o 2º turno em **25/10**. As janelas são configuráveis por env, então ajustar a data não exige mudar código.

### Config das janelas (env)

```
ELECTION_WINDOWS=2026-10-04T17:00-03:00/2026-10-05T22:00-03:00,2026-10-25T17:00-03:00/2026-10-26T22:00-03:00
```

- Collector **só coleta dentro da janela**; fora dela o ciclo não faz nada (no-op).
- Fora da janela a API continua respondendo com o último dado bom (somente leitura).

### Metas por janela

| Janela | Meta |
|--------|------|
| J1 | **Mínimo ao vivo:** collector em `PRODUCTION` + API nacional + página Angular nacional no ar. Pode ser simples. |
| J2 | **MVP completo** (todas as fases). |

Se o mínimo de J1 não ficar pronto, não forçar: mirar J2.

---

## Decisões travadas

| Tema | Decisão |
|------|---------|
| Stack app | Spring Boot (Java 17) + Angular SPA + PostgreSQL |
| Migrations | **Liquibase** (padrão Gabriel; não Flyway) |
| Tempo real | SSE (não WebSocket no MVP). O evento SSE só avisa "nova versão"; os dados vêm por REST |
| Collector | Mesmo processo da API (um serviço), ativo só dentro das janelas |
| Browser → TSE | Proibido; só o collector fala com o TSE |
| Dado ausente | `null` (nunca inventar `0`) |
| Dev local | Docker Compose (Postgres + API; Angular no host ou compose) |
| Deploy MVP (free) | Cloudflare Pages + Render + Neon |
| Região | API (Render `oregon`) e banco (Neon `aws-us-west-2` / Oregon). O Render free não tem região no Brasil |
| Agendamento | cron-job.org (free): ping em `/api/health` a cada 5 min, **só dentro das janelas** |
| Health | `/api/health` leve, **sem tocar no banco** (usado pelo cron). `/api/ready` verifica o banco |
| Volume de dados | Sem resultados por município no MVP (`COLLECT_CITY_RESULTS=false`). Gravar snapshot só quando o dado mudou. Fotos fora do banco (só URL) |
| Cache | REST com `Cache-Control` curto (10–15s). Cache em memória reconstruído a partir do banco |
| Segurança | CORS só para o domínio do Pages; API somente leitura; limite de conexões SSE por IP; secrets só em env; `sslmode=require`; usuário de banco da app com permissão mínima (separado do usuário das migrations) |
| Deploys | Sem deploy durante uma janela. Congelar na véspera |
| Commits | Só quando o Gabriel pedir |

---

## Fora de escopo do MVP

- Mapa interativo do Brasil
- Histórico/gráficos avançados de evolução
- Resultados por município
- App mobile nativo
- WebSocket
- Auth de usuário / painel admin
- Multi-eleição complexa na UI (seed demo de uma eleição basta)
- Hospedagem 24/7 (Oracle ou similar): desnecessária, só há duas janelas
- CI pesado / e2e completo (só smoke leve na Fase 5)

---

## Hospedagem

| Camada | Onde |
|--------|------|
| Angular | Cloudflare Pages |
| API + collector | Render (Docker, free), região `oregon` |
| Postgres | Neon free, região `aws-us-west-2` (Oregon, mesma área do Render) |
| Agendamento | cron-job.org (jobs com data, só dentro das janelas) |
| Schema | Liquibase no boot |

Regras de operação:

1. Cron-job.org cria os jobs de ping com datas das janelas. Primeiro ping às **16h00**, para acordar a API antes do TSE começar às 17h.
2. Angular faz warmup ao abrir, com SSE com reconnect e indicador de "última atualização ok".
3. UI mostra três estados: **aguardando início**, **ao vivo**, **encerrado**.
4. Após a janela, o site continua útil com o **snapshot final estático** (modo arquivado).

---

## Fases

### Fase 0 — Fundação ✅

- [x] Repo + `AGENTS.md` + regras Cursor
- [x] Scaffold Spring Boot + Angular
- [x] Docker Compose Postgres
- [x] `GET /api/health` + shell Angular

### Fase 1 — Domínio e banco

- [x] Entidades alinhadas ao original (`elections`, rounds, offices, cities, parties, candidates, area_progress, area_results, snapshots, ingestion, collector_cycles, photos)
- [x] Liquibase (`db/changelog`) — sem DDL manual
- [x] Repositórios JPA
- [x] Seed mínimo eleição **demo** (rotulado como fictício)
- [x] Config: local Docker; datasource via env (pronto para Neon)
- [x] Dois usuários de banco: migrations e aplicação (permissão mínima)
- [x] `/api/health` (sem banco) e `/api/ready` (com banco)

**Critério de pronto:** app sobe, Liquibase aplica, seed visível no banco.

### Fase 2 — Collector + adapter TSE

O adapter do TSE vem **cedo** porque é a parte de maior risco.

- [ ] Fixtures locais formato TSE (pasta `fixtures/`)
- [ ] Polling em modo `DEVELOPMENT`
- [ ] `TseAdapter2026`: URLs a partir do `ele-c.json`, validação dos schemas, conversão de números/horários
- [ ] Rate limit + ETag/304 (`TSE_REQUESTS_PER_SECOND`, `TSE_POLL_INTERVAL=15`)
- [ ] Modos `SIMULATION` / `PRODUCTION` (além de `DEVELOPMENT`)
- [ ] Gate das janelas (`ELECTION_WINDOWS`): fora da janela o ciclo é no-op
- [ ] Persistir `area_progress` / `area_results` + eventos de ingestão
- [ ] Em erro: manter último estado bom
- [ ] `COLLECT_CITY_RESULTS=false` por padrão; snapshot só se mudou

**Critério de pronto:** ciclo demo grava progresso/resultados sem TSE real **e** o adapter lê um JSON real do TSE (simulado ou oficial) e passa na validação.

### Fase 3 — API REST + SSE

- [ ] REST read-only (país, UF, cargos)
- [ ] `Cache-Control` curto + cache em memória com invalidação simples
- [ ] SSE `GET /api/realtime/elections/{roundId}` enviando só o evento de versão
- [ ] `/api/health` (sem banco) e `/api/ready` (com banco)
- [ ] CORS restrito, limite de conexões SSE por IP
- [ ] springdoc / Swagger

**Critério de pronto:** Swagger ok; cliente pode polir REST e receber evento SSE em mudança.

### Fase 4 — Angular

- [ ] Shell mobile-first
- [ ] Dashboard nacional
- [ ] Navegação estado (mínimo útil)
- [ ] HTTP + SSE com warmup, reconnect e indicador de atraso
- [ ] Estados de UI: aguardando início / ao vivo / encerrado
- [ ] Fallback para snapshot estático (modo arquivado)

**Critério de pronto:** UI nacional atualiza sozinha (demo e, quando disponível, TSE).

### Fase 5 — Deploy e operação

- [ ] Modo `REPLAY`
- [ ] Docker Compose full stack documentado
- [ ] Testes JUnit essenciais + smoke manual
- [ ] Deploy: Neon + Render + Cloudflare Pages (mesma região para API e banco)
- [ ] Jobs no cron-job.org para as janelas J1 e J2
- [ ] Export do snapshot final para o site estático (modo arquivado)
- [ ] README de operação (local + deploy) com o runbook abaixo
- [ ] Disclaimer de projeto independente

**Critério de MVP utilizável:** demo local com SSE; URL pública no ar; ciclo completo testado em `SIMULATION`; disclaimer visível.

---

## Runbook das janelas

**Véspera (sábado)**
- Congelar deploys
- Testar o fluxo inteiro em `SIMULATION`
- Conferir os jobs do cron-job.org e o `ELECTION_WINDOWS`

**Dia da janela**
- 16h00: primeiro ping acorda a API
- 16h30: conferir `/api/ready`, status do collector e banco
- 17h00: TSE começa a divulgar; collector em `PRODUCTION`
- Durante: observar atraso de ingestão e erros; sem deploy

**Encerramento (seg 22h)**
- Gate fecha o collector sozinho; jobs do cron terminam
- Exportar o snapshot final e publicar no Pages
- Conferir que o site mostra "encerrado" com os dados finais

---

## Ordem de trabalho (anti-distração)

1. Uma fase por vez; sem misturar deploy com domínio.
2. Deploy completo só na Fase 5. **Exceção:** o slice mínimo de J1 pode subir antes, depois de Fase 2 e da parte nacional das Fases 3 e 4.
3. Mapa / histórico / mobile = pós-MVP, novo plano.
4. Referência `D:\Eleicoes_2026` = espelhar domínio/contratos; não portar Node/Next.

---

## Critério único de “pronto para mostrar”

Docker local: Postgres + API + Angular; demo avança; UI nacional via SSE; adapter do TSE validado; deploy no ar com `/api/ready` verde dentro da janela.