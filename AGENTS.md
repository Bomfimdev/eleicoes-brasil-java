# AGENTS.md — Eleições Brasil (Java / Angular)

Leia este arquivo no início de **toda** sessão neste repositório.

## Quem é o autor

**Gabriel Bomfim** — Full Stack Java | Spring Boot | Angular  
GitHub: https://github.com/Bomfimdev · Repo: https://github.com/Bomfimdev/eleicoes-brasil-java

Stack do dia a dia: Java 8/11/17, Spring Boot, Spring Security, Spring Data JPA, Angular, PostgreSQL/Oracle, Docker, JUnit, Swagger, CI/CD. Experiência em sistemas de governo (CNJ, Exército).

## O que é este projeto

Adaptação / reimplementação do [lucianookdp/eleicoes-brasil](https://github.com/lucianookdp/eleicoes-brasil) na stack **Spring Boot + Angular + PostgreSQL**.

Objetivo: acompanhar a apuração das eleições brasileiras em tempo real com **dados públicos oficiais do TSE**.

Referência local do original (código TypeScript/Next/Fastify): `D:\Eleicoes_2026`

## Stack alvo

| Camada | Tecnologia |
| --- | --- |
| API REST + SSE | Spring Boot 3, Java 17 |
| Collector (polling TSE) | Spring Boot (módulo ou app) |
| Frontend | Angular (SPA, mobile-first) |
| Banco | PostgreSQL 16 + Flyway |
| Tempo real | Server-Sent Events (SSE) |
| Infra local | Docker Compose |
| Docs API | springdoc-openapi (Swagger) |

## Princípios (não negociáveis)

1. **O navegador nunca fala com o TSE.** Só o collector consulta o TSE.
2. **Formato bruto do TSE isolado** no adapter (`TseAdapter2026`). Resto fala domínio limpo.
3. **Eleição nova = dados/config**, não tabelas/componentes com ano no nome.
4. **Nunca inventar dado.** Indisponível é `null`, não `0`.
5. **Falhar mantendo o último dado bom.** UI mostra atraso + horário da última atualização ok.
6. Projeto **independente** — não é serviço oficial da Justiça Eleitoral.

## Arquitetura alvo

```
TSE CDN → Collector → TseAdapter → PostgreSQL ──LISTEN/NOTIFY──► API (REST + SSE) → Angular
```

Pacotes/módulos sugeridos:

```
backend/                 # Maven multi-módulo (ou monólito modular no início)
  eleicoes-core/         # domínio, DTOs, contratos
  eleicoes-collector/    # polling TSE, adapter, gravação
  eleicoes-api/          # REST + SSE + cache
frontend/                # Angular
docs/                    # plano, ADRs, integração TSE
fixtures/                # eleição demo local
docker-compose.yml
```

## Modelo de dados (espelhar o original)

Entidades principais: `elections`, `election_rounds`, `offices`, `cities`, `parties`,
`candidates`, `area_progress`, `area_results`, `progress_snapshots`, `result_snapshots`,
`ingestion_events`, `collector_cycles`, `candidate_photos`.

- `area_key` estável: `br`, `sp`, `sp-71072`, `sp-71072-z0001`
- Snapshots **completos por mudança** (não delta)
- Tudo em UTC (`timestamptz`); exibição em `America/Sao_Paulo`

## Modos de execução

| Modo | Fonte |
| --- | --- |
| DEVELOPMENT | servidor/fixtures locais |
| SIMULATION | `resultados-sim.tse.jus.br` |
| PRODUCTION | `resultados.tse.jus.br` |
| REPLAY | snapshots no banco |

## Plano de execução (ordem)

1. **Fundação** — scaffold, Docker Postgres, health, Flyway vazio
2. **Domínio + banco** — entidades, migrações, repositórios
3. **Collector demo** — fixtures locais, sem TSE real
4. **API REST** — endpoints read-only + Swagger
5. **Angular** — dashboard nacional consumindo a API
6. **SSE** — tempo real
7. **TSE real** — adapter + rate limit + ETag/304
8. **Polish** — mapa, histórico, Docker full stack, testes

## Como o agente deve trabalhar

- Respostas em **português**, simples e diretas.
- Preferir padrões do Gabriel: Spring Boot, JPA, Angular standalone components quando fizer sentido, Swagger, JUnit, Docker.
- Consultar `D:\Eleicoes_2026` quando precisar espelhar contratos, schemas TSE ou regras de negócio — **não copiar Node/Next**; reimplementar em Java/Angular.
- Não inventar métricas do TSE nem dados eleitorais fictícios como se fossem oficiais (fixtures de demo ok, bem rotuladas).
- Commits só quando o usuário pedir.
- Não expandir escopo além da fase atual do plano.

## Próximo passo imediato

Continuar o scaffold e deixar `GET /api/health` + Postgres no Docker + app Angular “hello” rodando.
