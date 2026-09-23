# Starts only the Cloudflare Tunnel pointing at localhost:8080.
#
# Leave this window open. The tunnel forwards to a port, not to a particular process, so the app
# behind it can be rebuilt and restarted as often as you like and the public URL stays the same.
# A quick tunnel only gets a new URL when *this* window is stopped.

param(
    # Pass a named tunnel once you have one set up against your own domain, for a permanent URL.
    [string]$TunnelName = ""
)

$ErrorActionPreference = "Stop"

. "$PSScriptRoot\_common.ps1"

Stop-LearnETunnel
$cloudflared = Resolve-Cloudflared

# A configured named tunnel is what the domain points at, so it wins by default.
if (-not $TunnelName) { $TunnelName = Get-ConfiguredTunnelName }

if ($TunnelName) {
    Write-Host "Named tunnel: $TunnelName (URL co dinh)" -ForegroundColor Cyan
    & $cloudflared tunnel run $TunnelName
} else {
    Write-Host "Quick tunnel - URL *.trycloudflare.com hien ben duoi, giu cua so nay mo." -ForegroundColor Cyan
    Write-Host "Sua code thi build lai roi chay .\deploypp.ps1, URL nay van giu nguyen.`n"
    & $cloudflared tunnel --url http://localhost:8080
}
