# Builds the frontend, folds it into the backend, and packages one runnable jar.
#
# The result serves both the API and the React app on port 8080, which is what lets the Cloudflare
# Tunnel setup work with a single URL and no CORS.

$ErrorActionPreference = "Stop"

. "$PSScriptRoot\_common.ps1"

$root = Split-Path -Parent $PSScriptRoot
$frontend = Join-Path $root "learnE-Frontend"
$backend = Join-Path $root "learnE-Backend"
$static = Join-Path $backend "src\main\resources\static"

Use-Jdk21

# Windows keeps the jar locked while the app runs, and Maven then quietly leaves the previous jar
# in place — producing "no main manifest attribute" when you try to start it.
Write-Host "`n[1/4] Dung ung dung neu dang chay..." -ForegroundColor Cyan
Stop-LearnEApp

Write-Host "`n[2/4] Build frontend..." -ForegroundColor Cyan
Push-Location $frontend
try {
    # No VITE_API_BASE_URL: the app calls /api on its own origin.
    npm run build
    if ($LASTEXITCODE -ne 0) { throw "Build frontend that bai" }
} finally { Pop-Location }

Write-Host "`n[3/4] Copy frontend vao backend..." -ForegroundColor Cyan
if (Test-Path $static) { Remove-Item -Recurse -Force $static }
New-Item -ItemType Directory -Force -Path $static | Out-Null
Copy-Item -Recurse -Force (Join-Path $frontend "dist\*") $static
Write-Host "  -> $static"

Write-Host "`n[4/4] Build backend jar..." -ForegroundColor Cyan
Push-Location $backend
try {
    # "clean" matters: without it, resources deleted or newly excluded from the source tree linger
    # in target/classes and end up in the jar anyway. Tests are skipped - this packages what already
    # runs, it is not a CI gate.
    & .\mvnw.cmd -q -DskipTests clean package
    if ($LASTEXITCODE -ne 0) { throw "Build backend that bai" }
} finally { Pop-Location }

$jar = Get-AppJar -BackendPath $backend

# A jar that was not repackaged starts with "no main manifest attribute", so check it here rather
# than letting it fail at run time.
$manifest = & "$env:JAVA_HOME\bin\jar.exe" --describe-module --file $jar.FullName 2>$null
$isBootJar = (& "$env:JAVA_HOME\bin\jar.exe" -tf $jar.FullName 2>$null | Select-String -SimpleMatch "BOOT-INF/" -Quiet)
if (-not $isBootJar) {
    throw "Jar chua duoc Spring Boot dong goi lai. Dam bao ung dung da dung han roi build lai."
}

Write-Host "`nXong: $($jar.FullName)" -ForegroundColor Green
Write-Host ("  kich thuoc: {0:N0} MB" -f ($jar.Length / 1MB))
Write-Host "Chay tiep: .\deploy\app.ps1  (hoac .\deploy\start.ps1 neu chua co tunnel)"
