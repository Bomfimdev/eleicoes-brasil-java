# Exporta overview da API para frontend/public/archive/<slug>.json (modo arquivado).
param(
  [string]$ApiUrl = "http://localhost:8080",
  [string]$RoundSlug = "demo-1",
  [string]$OutDir = ""
)

$ErrorActionPreference = "Stop"
if (-not $OutDir) {
  $OutDir = Join-Path $PSScriptRoot "..\frontend\public\archive"
}
New-Item -ItemType Directory -Force -Path $OutDir | Out-Null
$out = Join-Path $OutDir "$RoundSlug.json"
$url = "$ApiUrl/api/elections/$RoundSlug/export"
Write-Host "GET $url"
Invoke-WebRequest -Uri $url -OutFile $out -UseBasicParsing
Write-Host "Salvo em $out"
Write-Host "Faça rebuild/deploy do frontend para publicar o arquivo estático."
