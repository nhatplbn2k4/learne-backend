# Starts Postgres and the packaged app on port 8080 - without the tunnel.
#
# Run this when a tunnel is already up: the tunnel forwards to a port, not to a process, so the app
# behind it can be restarted and the public URL stays the same.

param(
    # Serve the frontend straight from learnE-Frontend/dist instead of from inside the jar, so a
    # frontend change only needs "npm run build" and a browser refresh - no repackage, no restart.
    [switch]$LiveFrontend
)

$ErrorActionPreference = "Stop"

. "$PSScriptRoot\_common.ps1"

$root = Split-Path -Parent $PSScriptRoot
$backend = Join-Path $root "learnE-Backend"

Get-AppSecrets
Use-Jdk21

Write-Host "`n[1/2] Postgres..." -ForegroundColor Cyan
Start-Postgres
Stop-LearnEApp
Write-Host "  -> san sang"

$jar = Get-AppJar -BackendPath $backend
$java = Join-Path $env:JAVA_HOME "bin\java.exe"
if (-not (Test-Path $java)) { $java = "java" }

$extraArgs = @()
if ($LiveFrontend) {
    $dist = Join-Path $root "learnE-Frontend\dist"
    if (-not (Test-Path (Join-Path $dist "index.html"))) {
        throw "Khong thay $dist\index.html - chay 'npm run build' trong learnE-Frontend truoc"
    }
    # Spring wants a URL; backslashes and spaces in the path would break it.
    $uri = ([Uri](Resolve-Path $dist).Path).AbsoluteUri
    $extraArgs += "--app.frontend.location=$uri/"
}

Write-Host "`n[2/2] Ung dung (profile prod) tren http://localhost:8080" -ForegroundColor Cyan
if ($LiveFrontend) {
    Write-Host "  Frontend doc truc tiep tu learnE-Frontend\dist:" -ForegroundColor Yellow
    Write-Host "  sua giao dien -> 'npm run build' -> F5 trinh duyet. Khong can build lai jar." -ForegroundColor Yellow
}
Write-Host "  Ctrl+C de dung ung dung. Tunnel (neu dang chay) khong bi anh huong.`n"
& $java -jar $jar.FullName --spring.profiles.active=prod @extraArgs
