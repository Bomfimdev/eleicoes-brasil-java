#!/usr/bin/env bash
# Exporta overview da API para frontend/public/archive/<slug>.json
set -euo pipefail
API_URL="${1:-http://localhost:8080}"
ROUND_SLUG="${2:-demo-1}"
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
OUT_DIR="$ROOT/frontend/public/archive"
mkdir -p "$OUT_DIR"
OUT="$OUT_DIR/${ROUND_SLUG}.json"
URL="$API_URL/api/elections/${ROUND_SLUG}/export"
echo "GET $URL"
curl -fsSL "$URL" -o "$OUT"
echo "Salvo em $OUT"
echo "Faça rebuild/deploy do frontend para publicar o arquivo estático."
