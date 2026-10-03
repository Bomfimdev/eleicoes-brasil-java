# Operação — Eleições Brasil (Java)

Projeto **independente**. Não é serviço oficial da Justiça Eleitoral.

## URLs

| Camada | URL |
|--------|-----|
| Front (Pages) | https://eleicoes-brasil.pages.dev |
| API (Render) | https://eleicoes-brasil-api.onrender.com |
| Health (cron) | https://eleicoes-brasil-api.onrender.com/api/health |
| Ready | https://eleicoes-brasil-api.onrender.com/api/ready |
| Swagger | https://eleicoes-brasil-api.onrender.com/swagger-ui.html |

## Modos (`APP_MODE`)

| Modo | Comportamento |
|------|----------------|
| `DEVELOPMENT` | Fixtures classpath; collector pode ignorar janelas (`COLLECTOR_IGNORE_WINDOWS`) |
| `SIMULATION` | HTTP em `resultados-sim.tse.jus.br` + rate limit/ETag |
| `PRODUCTION` | HTTP em `resultados.tse.jus.br` |
| `REPLAY` | Reaplica `progress_snapshots` / `result_snapshots` do round (`REPLAY_SOURCE_ROUND_SLUG`), sem TSE |

## Docker Compose local

```bash
# Postgres + API
docker compose up -d --build postgres api

# Full stack (exige build Angular antes)
cd frontend && npm ci && npm run build -- --configuration production && cd ..
docker compose --profile full up -d frontend
# Front: http://localhost:8088  · API: http://localhost:8080
```

Dev sem Docker da API:

```bash
docker compose up -d postgres
cd backend && ./mvnw spring-boot:run
cd frontend && npm start
```

## Keep-alive (acordar Render Free)

### Ativo: Cloudflare Worker (cron a cada 5 min)

| Item | Valor |
|------|--------|
| Worker | `eleicoes-brasil-keepalive` |
| URL (teste manual) | https://eleicoes-brasil-keepalive.gbomfimprofissional.workers.dev |
| Cron | `*/5 * * * *` |
| Alvo | `GET https://eleicoes-brasil-api.onrender.com/api/health` |
| Código | `ops/keepalive-worker/` |

Redeploy:

```bash
cd ops/keepalive-worker
# CLOUDFLARE_API_TOKEN em D:\credenciais\cloudflare\cloudflare.env
npx wrangler deploy
```

`/api/health` **não** toca no banco (leve). Use `/api/ready` só em checagem manual.

### Opcional: cron-job.org (backup J1/J2)

Credenciais: `D:\credenciais\eleicoes-brasil\cron-job.env` (e-mail + senha; preencher `CRON_JOB_ORG_API_KEY` em Settings → API).

Com a API key:

```powershell
.\scripts\create-cron-jobs.ps1
```

Cria dois jobs GET `/api/health` a cada 5 min (timezone `America/Sao_Paulo`):

- **J1:** 04/10/2026 16:00 → 05/10/2026 22:00
- **J2:** 25/10/2026 16:00 → 26/10/2026 22:00 (2º turno)

O Worker Cloudflare já cobre o keep-alive contínuo do MVP; cron-job.org é redundância nas janelas.

Env alinhado no Render:

```
ELECTION_WINDOWS=2026-10-04T17:00-03:00/2026-10-05T22:00-03:00,2026-10-25T17:00-03:00/2026-10-26T22:00-03:00
CORS_ORIGINS=http://localhost:4200,https://eleicoes-brasil.pages.dev
```

JVM free tier: `JAVA_TOOL_OPTIONS` no Dockerfile (`MaxMetaspaceSize=160m`, heap 160m) — sem isso a API estoura Metaspace no plano Free.

## Export modo arquivado

Após o encerramento da janela (collector parado, dados finais no Neon):

```powershell
# PowerShell
.\scripts\export-archive.ps1 -ApiUrl https://eleicoes-brasil-api.onrender.com -RoundSlug demo-1
```

```bash
# bash
./scripts/export-archive.sh https://eleicoes-brasil-api.onrender.com demo-1
```

Isso grava `frontend/public/archive/<slug>.json`. Rebuild + deploy do Pages.  
Se a API estiver dormindo/offline, o Angular carrega esse JSON (`archivePath`).

## Runbook das janelas

Ver também `docs/PLANO.md`.

**Véspera (sábado)**  
Congelar deploys · testar `APP_MODE=SIMULATION` · conferir cron e `ELECTION_WINDOWS`.

**Dia**  
16h ping · 16h30 `/api/ready` · 17h `PRODUCTION` · sem deploy durante a apuração.

**Encerramento (seg ~22h)**  
Gate fecha collector · parar jobs do cron · exportar archive · Pages mostra Encerrado.

## Smoke checklist

1. `GET /api/health` → 200  
2. `GET /api/ready` → 200 / database up  
3. `GET /api/elections` → lista com `demo`  
4. `GET /api/elections/demo-1/overview` → progresso/headline  
5. `GET /api/realtime/elections/demo-1` → SSE `ready`  
6. Front Pages carrega overview (CORS)  
7. Disclaimer visível no rodapé
