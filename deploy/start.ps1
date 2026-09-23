# Convenience: starts the app and a tunnel together in one window.
#
# Handy for a one-off session. If you expect to rebuild while it runs, use two windows instead
# (.\deploy	unnel.ps1 and .\deploypp.ps1) so Ctrl+C on the app does not drop the tunnel URL.

param(
    [string]$TunnelName = ""
)

$ErrorActionPreference = "Stop"

. "$PSScriptRoot\_common.ps1"

$root = Split-Path -Parent $PSScriptRoot
$backend = Join-Path $root "learnE-Backend"

Get-AppSecrets
Use-Jdk21

Write-Host "`n[1/3] Postgres..." -ForegroundColor Cyan
Start-Postgres
Stop-LearnEApp
Write-Host "  -> san sang"

$jar = Get-AppJar -BackendPath $backend
$java = Join-Path $env:JAVA_HOME "bin\java.exe"
if (-not (Test-Path $java)) { $java = "java" }

Write-Host "`n[2/3] Khoi dong ung dung (profile prod)..." -ForegroundColor Cyan
$app = Start-Process -FilePath $java `
    -ArgumentList "-jar", "`"$($jar.FullName)`"", "--spring.profiles.active=prod" `
    -PassThru -NoNewWindow

$ready = $false
for ($i = 0; $i -lt 60; $i++) {
    Start-Sleep -Seconds 2
    try {
        # 401 is a healthy answer here: the endpoint exists and security is live.
        Invoke-WebRequest -Uri "http://localhost:8080/api/courses" -UseBasicParsing -TimeoutSec 3 | Out-Null
        $ready = $true; break
    } catch {
        if ($_.Exception.Response.StatusCode.value__ -eq 401) { $ready = $true; break }
    }
}
if (-not $ready) {
    Stop-Process -Id $app.Id -Force -ErrorAction SilentlyContinue
    throw "Ung dung khong khoi dong duoc"
}
Write-Host "  -> http://localhost:8080"

Write-Host "`n[3/3] Cloudflare Tunnel..." -ForegroundColor Cyan
Stop-LearnETunnel
$cloudflared = Resolve-Cloudflared
# A configured named tunnel is what the domain points at, so it wins by default.
if (-not $TunnelName) { $TunnelName = Get-ConfiguredTunnelName }
try {
    if ($TunnelName) {
        Write-Host "  Named tunnel: $TunnelName (URL co dinh)"
        & $cloudflared tunnel run $TunnelName
    } else {
        Write-Host "  Quick tunnel - URL *.trycloudflare.com se hien ben duoi, doi moi lan chay."
        Write-Host "  Ctrl+C de dung ca tunnel va ung dung.`n"
        & $cloudflared tunnel --url http://localhost:8080
    }
} finally {
    Stop-Process -Id $app.Id -Force -ErrorAction SilentlyContinue
    Write-Host "`nDa dung." -ForegroundColor Green
}
