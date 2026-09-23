# One-off setup of a *named* Cloudflare Tunnel, which gives a permanent URL instead of the random
# one a quick tunnel hands out.
#
#   .\deploy\setup-tunnel.ps1 -Domain vidu.com
#   -> https://learne.vidu.com
#
# Requires the domain to already be on your Cloudflare account (automatic if you registered it at
# Cloudflare Registrar; otherwise point its nameservers at Cloudflare first).
#
# Afterwards, run it day to day with:  .\deploy\start.ps1 -TunnelName learne

param(
    [Parameter(Mandatory = $true)]
    [string]$Domain,

    # The name under the domain, so the app lives at <Subdomain>.<Domain>.
    [string]$Subdomain = "learne",

    [string]$TunnelName = "learne"
)

$ErrorActionPreference = "Stop"

. "$PSScriptRoot\_common.ps1"

$cloudflared = Resolve-Cloudflared
$hostname = "$Subdomain.$Domain"
$cfDir = Join-Path $env:USERPROFILE ".cloudflared"
$configPath = Join-Path $cfDir "config.yml"

# --- 1. Authorise this machine -------------------------------------------------------------------
# Opens a browser to pick the zone; writes cert.pem, which every later command needs.
$cert = Join-Path $cfDir "cert.pem"
if (Test-Path $cert) {
    Write-Host "[1/4] Da dang nhap Cloudflare truoc do ($cert)" -ForegroundColor Cyan
} else {
    Write-Host "[1/4] Dang nhap Cloudflare - trinh duyet se mo ra, chon domain $Domain" -ForegroundColor Cyan
    & $cloudflared tunnel login
    if (-not (Test-Path $cert)) { throw "Dang nhap that bai, khong thay $cert" }
}

# --- 2. Create the tunnel ------------------------------------------------------------------------
Write-Host "`n[2/4] Tunnel '$TunnelName'..." -ForegroundColor Cyan
$existing = (& $cloudflared tunnel list 2>$null) -join "`n"
if ($existing -match "\s$([regex]::Escape($TunnelName))\s") {
    Write-Host "  Da ton tai, dung lai."
} else {
    & $cloudflared tunnel create $TunnelName
    if ($LASTEXITCODE -ne 0) { throw "Tao tunnel that bai" }
}

# The credentials file is named after the tunnel UUID.
$uuid = ((& $cloudflared tunnel list 2>$null) |
    Select-String -Pattern "^(\S+)\s+$([regex]::Escape($TunnelName))\s" |
    ForEach-Object { $_.Matches[0].Groups[1].Value } |
    Select-Object -First 1)
if (-not $uuid) { throw "Khong doc duoc UUID cua tunnel '$TunnelName'" }
$credentials = Join-Path $cfDir "$uuid.json"
if (-not (Test-Path $credentials)) { throw "Khong thay file credentials $credentials" }

# --- 3. Ingress rules ----------------------------------------------------------------------------
# Everything for this hostname goes to the app; anything else gets a 404 rather than being proxied.
Write-Host "`n[3/4] Ghi $configPath" -ForegroundColor Cyan
$config = @"
tunnel: $uuid
credentials-file: $credentials

ingress:
  - hostname: $hostname
    service: http://localhost:8080
  - service: http_status:404
"@
if (Test-Path $configPath) {
    $backup = "$configPath.bak"
    Copy-Item $configPath $backup -Force
    Write-Host "  Da sao luu cau hinh cu -> $backup" -ForegroundColor Yellow
}
Set-Content -Path $configPath -Value $config -Encoding utf8
Write-Host "  $hostname -> http://localhost:8080"

# --- 4. DNS ---------------------------------------------------------------------------------------
Write-Host "`n[4/4] Tro DNS $hostname ve tunnel..." -ForegroundColor Cyan
& $cloudflared tunnel route dns $TunnelName $hostname
# Re-running is normal; an existing record is not an error worth stopping for.
if ($LASTEXITCODE -ne 0) {
    Write-Host "  (Neu bao ban ghi da ton tai thi bo qua.)" -ForegroundColor Yellow
}

Write-Host "`nXong. Tu gio chay:" -ForegroundColor Green
Write-Host "  .\deploy\start.ps1 -TunnelName $TunnelName"
Write-Host "  hoac 2 cua so: .\deploy\tunnel.ps1 -TunnelName $TunnelName  +  .\deploy\app.ps1"
Write-Host "`nDia chi co dinh: https://$hostname" -ForegroundColor Green
