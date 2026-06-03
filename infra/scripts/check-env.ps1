param(
  [string]$BackendUrl = "http://localhost:8080",
  [string]$FrontendUrl = "http://localhost:5173"
)

$ErrorActionPreference = "Stop"

Write-Host "Checking backend: $BackendUrl/healthz"
$health = Invoke-RestMethod -Uri "$BackendUrl/healthz" -Method Get
if (-not $health.ok) {
  throw "Backend health check failed"
}

Write-Host "Checking stores API"
$stores = Invoke-RestMethod -Uri "$BackendUrl/api/stores" -Method Get
if ($stores.data.Count -lt 1) {
  throw "No stores returned"
}

Write-Host "Checking frontend: $FrontendUrl"
$frontend = Invoke-WebRequest -Uri $FrontendUrl -Method Get
if ($frontend.StatusCode -ne 200) {
  throw "Frontend check failed"
}

Write-Host "NekoCafe environment is ready."
