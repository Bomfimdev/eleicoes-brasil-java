# Cria jobs J1/J2 no cron-job.org (requer CRON_JOB_ORG_API_KEY em D:\credenciais\eleicoes-brasil\cron-job.env).
param(
  [string]$CredFile = "D:\credenciais\eleicoes-brasil\cron-job.env",
  [string]$HealthUrl = "https://eleicoes-brasil-api.onrender.com/api/health"
)

$ErrorActionPreference = "Stop"
if (-not (Test-Path $CredFile)) {
  throw "Arquivo não encontrado: $CredFile"
}

Get-Content $CredFile | ForEach-Object {
  if ($_ -match '^\s*#' -or $_ -notmatch '=') { return }
  $k, $v = $_ -split '=', 2
  Set-Item -Path "Env:$($k.Trim())" -Value $v.Trim()
}

if (-not $env:CRON_JOB_ORG_API_KEY) {
  throw "Defina CRON_JOB_ORG_API_KEY em $CredFile (Console → Settings → API)."
}

$headers = @{
  Authorization = "Bearer $($env:CRON_JOB_ORG_API_KEY)"
  "Content-Type" = "application/json"
}

# minutes: 0,5,10,...,55 → a cada 5 min
$minutes = @(0, 5, 10, 15, 20, 25, 30, 35, 40, 45, 50, 55)

function New-JobBody([string]$title, [int[]]$mdays, [int[]]$months, [int[]]$hours) {
  @{
    job = @{
      url            = $HealthUrl
      enabled        = $true
      title          = $title
      saveResponses  = $false
      requestMethod  = 0  # GET
      schedule       = @{
        timezone = "America/Sao_Paulo"
        expiresAt = 0
        hours     = $hours
        mdays     = $mdays
        minutes   = $minutes
        months    = $months
        wdays     = @(-1)
      }
    }
  } | ConvertTo-Json -Depth 6 -Compress
}

# J1: 4/out 16h–23h + 5/out 0h–22h
$j1a = New-JobBody "eleicoes-J1-dia1" @(4) @(10) @(16, 17, 18, 19, 20, 21, 22, 23)
$j1b = New-JobBody "eleicoes-J1-dia2" @(5) @(10) @(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22)
# J2: 25/out 16h–23h + 26/out 0h–22h
$j2a = New-JobBody "eleicoes-J2-dia1" @(25) @(10) @(16, 17, 18, 19, 20, 21, 22, 23)
$j2b = New-JobBody "eleicoes-J2-dia2" @(26) @(10) @(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22)

foreach ($body in @($j1a, $j1b, $j2a, $j2b)) {
  $resp = Invoke-RestMethod -Method Put -Uri "https://api.cron-job.org/jobs" -Headers $headers -Body $body
  Write-Host "Criado jobId=$($resp.jobId)"
  Start-Sleep -Seconds 1
}

Write-Host "OK — liste com: GET https://api.cron-job.org/jobs"
