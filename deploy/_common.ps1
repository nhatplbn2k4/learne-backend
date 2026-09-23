# Helpers shared by build.ps1, app.ps1, start.ps1 and tunnel.ps1. Dot-source it:  . "$PSScriptRoot\_common.ps1"

# Stops a running instance of this app, whether started from the jar or from mvnw.
#
# Both scripts need this: app.ps1 because port 8080 would be taken, and build.ps1 because Windows
# locks the jar file while java is running, which makes Maven silently skip the repackage step and
# leave a jar with no main manifest behind.
function Stop-LearnEApp {
    $busy = Get-NetTCPConnection -LocalPort 8080 -State Listen -ErrorAction SilentlyContinue |
        Select-Object -First 1
    if (-not $busy) { return }

    $owner = Get-CimInstance Win32_Process -Filter "ProcessId = $($busy.OwningProcess)" -ErrorAction SilentlyContinue
    $isOurs = $owner -and $owner.CommandLine -and
              ($owner.CommandLine -match "learnE" -or $owner.CommandLine -match "spring-boot")
    if (-not $isOurs) {
        throw "Cong 8080 dang bi PID $($busy.OwningProcess) su dung va do khong phai app nay. Hay dung no truoc."
    }

    Write-Host "  Dung ung dung dang chay (PID $($busy.OwningProcess))..." -ForegroundColor Yellow
    Stop-Process -Id $busy.OwningProcess -Force
    # Windows releases the file handle a moment after the process goes.
    Start-Sleep -Seconds 3
}

# Stops any cloudflared left over from an earlier run.
#
# Without this every start.ps1 added another tunnel process: the first one keeps the connection and
# the rest retry forever with "Unauthorized: Tunnel not found", filling the log and the task list.
function Stop-LearnETunnel {
    $running = Get-Process cloudflared -ErrorAction SilentlyContinue
    if (-not $running) { return }

    Write-Host "  Dung $($running.Count) tunnel dang chay..." -ForegroundColor Yellow
    $running | Stop-Process -Force -ErrorAction SilentlyContinue
    Start-Sleep -Seconds 2
}

# The named tunnel recorded in %USERPROFILE%\.cloudflared\config.yml, or "" if there is none.
#
# Without this, forgetting -TunnelName starts a *quick* tunnel: it comes up happily on a random
# trycloudflare.com URL while learne.nhatdev.fun answers 530, because nothing is serving it any
# more. A silent outage that looks like a healthy tunnel in the log - so the name is read from the
# config that already has it rather than left to memory.
function Get-ConfiguredTunnelName {
    $config = Join-Path $env:USERPROFILE ".cloudflared\config.yml"
    if (-not (Test-Path $config)) { return "" }

    $line = Select-String -Path $config -Pattern '^\s*tunnel:\s*(\S+)' | Select-Object -First 1
    if (-not $line) { return "" }
    return $line.Matches[0].Groups[1].Value
}

function Use-Jdk21 {
    if ($env:JAVA_HOME -and (Test-Path $env:JAVA_HOME)) { return }
    $jdk = "C:\Program Files\Java\jdk-21"
    if (Test-Path $jdk) {
        Write-Host "  JAVA_HOME khong hop le, dung $jdk"
        $env:JAVA_HOME = $jdk
    }
}

function Start-Postgres {
    # Docker Desktop does not always survive a reboot; starting it here avoids a confusing
    # "connection refused" from Flyway later on.
    docker ps *> $null
    if ($LASTEXITCODE -ne 0) {
        $desktop = "C:\Program Files\Docker\Docker\Docker Desktop.exe"
        if (-not (Test-Path $desktop)) { throw "Docker Desktop chua chay va khong tim thay $desktop" }
        Write-Host "  Docker Desktop chua chay, dang bat..."
        Start-Process -FilePath $desktop -WindowStyle Hidden
        for ($i = 0; $i -lt 60; $i++) {
            Start-Sleep -Seconds 5
            docker ps *> $null
            if ($LASTEXITCODE -eq 0) { break }
        }
        if ($LASTEXITCODE -ne 0) { throw "Docker Desktop khong khoi dong duoc" }
    }

    $running = docker ps --filter "name=learne-postgres" --format "{{.Names}}" 2>$null
    if ($running -notcontains "learne-postgres") { docker start learne-postgres | Out-Null }
    for ($i = 0; $i -lt 30; $i++) {
        docker exec learne-postgres pg_isready -U learne *> $null
        if ($LASTEXITCODE -eq 0) { break }
        Start-Sleep -Seconds 2
    }
}

function Get-AppSecrets {
    if (-not $env:JWT_SECRET) {
        $env:JWT_SECRET = [Environment]::GetEnvironmentVariable("JWT_SECRET", "User")
    }
    if (-not $env:JWT_SECRET) {
        Write-Host "Chua co JWT_SECRET. Tao mot lan roi luu vinh vien bang lenh:" -ForegroundColor Yellow
        Write-Host '  [Environment]::SetEnvironmentVariable("JWT_SECRET", [Convert]::ToBase64String((1..48 | ForEach-Object { Get-Random -Max 256 })), "User")'
        throw "Thieu JWT_SECRET (profile prod khong cho dung secret mac dinh)"
    }
    if (-not $env:GEMINI_API_KEY) {
        $env:GEMINI_API_KEY = [Environment]::GetEnvironmentVariable("GEMINI_API_KEY", "User")
    }
}

function Get-AppJar {
    param([string]$BackendPath)
    $jar = Get-ChildItem (Join-Path $BackendPath "target\*.jar") -ErrorAction SilentlyContinue |
        Where-Object { $_.Name -notlike "*sources*" -and $_.Name -notlike "*.original" } |
        Select-Object -First 1
    if (-not $jar) { throw "Chua co file jar - chay .\deploy\build.ps1 truoc" }
    return $jar
}

function Resolve-Cloudflared {
    $cmd = Get-Command cloudflared -ErrorAction SilentlyContinue
    if ($cmd) { return $cmd.Source }
    # winget puts it on the machine PATH, which shells opened earlier do not see.
    foreach ($candidate in @(
        "C:\Program Files (x86)\cloudflared\cloudflared.exe",
        "C:\Program Files\cloudflared\cloudflared.exe")) {
        if (Test-Path $candidate) { return $candidate }
    }

    Write-Host "Chua cai cloudflared, dang cai bang winget..." -ForegroundColor Yellow
    winget install --id Cloudflare.cloudflared --accept-source-agreements --accept-package-agreements
    $env:Path = [Environment]::GetEnvironmentVariable("Path", "Machine") + ";" +
                [Environment]::GetEnvironmentVariable("Path", "User")
    $cmd = Get-Command cloudflared -ErrorAction SilentlyContinue
    if ($cmd) { return $cmd.Source }
    throw "Cai cloudflared that bai - cai tay roi chay lai: winget install --id Cloudflare.cloudflared"
}
