# Plano de execução

## Fase 0 — Fundação (atual)

- [x] Pasta + repo GitHub (`Bomfimdev/eleicoes-brasil-java`)
- [x] `AGENTS.md` + regras Cursor
- [x] Scaffold Spring Boot (API)
- [x] Scaffold Angular
- [x] `docker-compose.yml` com PostgreSQL
- [x] `GET /api/health` + shell Angular checando a API

## Fase 1 — Domínio e banco

- [ ] Entidades alinhadas ao modelo do original
- [ ] Migrações Flyway
- [ ] Repositórios JPA
- [ ] Seed mínimo da eleição demo

## Fase 2 — Collector (demo)

- [ ] Fixtures locais no formato TSE
- [ ] Ciclo de polling em DEVELOPMENT
- [ ] Persistência de `area_progress` / `area_results`
- [ ] Eventos de ingestão

## Fase 3 — API

- [ ] REST read-only (país, UF, município, cargos)
- [ ] Cache em memória + invalidação
- [ ] SSE `GET /api/realtime/elections/{roundId}`
- [ ] Swagger

## Fase 4 — Angular

- [ ] Shell da aplicação
- [ ] Dashboard nacional
- [ ] Navegação estado / município
- [ ] Cliente HTTP + SSE

## Fase 5 — Produção TSE + polish

- [ ] `TseAdapter2026` + rate limit + ETag/304
- [ ] Modos SIMULATION / PRODUCTION / REPLAY
- [ ] Docker Compose full stack
- [ ] Testes JUnit + e2e leve
- [ ] README de operação

## Critério de “MVP utilizável”

Docker sobe Postgres + API + Angular; modo demo mostra apuração fictícia avançando; UI nacional atualiza via SSE.
