# Local multiplayer test: a dedicated server and two clients (Dev1 and Dev2) that join it on their own.
# Usage: .\multiplayer-test.ps1   (or .\multiplayer-test.ps1 -Clients 1 for a single client)
# Each one opens in its own window; close a window to stop it.
param([int]$Clients = 2)

$root = $PSScriptRoot

# Offline names (Dev1, Dev2) can only join a server in offline mode
$properties = Join-Path $root 'run\server.properties'
if (Test-Path $properties) {
    (Get-Content $properties) -replace '^online-mode=true$', 'online-mode=false' | Set-Content $properties -Encoding ascii
} else {
    New-Item -ItemType Directory -Force (Join-Path $root 'run') | Out-Null
    Set-Content $properties 'online-mode=false' -Encoding ascii
}
Set-Content (Join-Path $root 'run\eula.txt') 'eula=true' -Encoding ascii

# Compile once first, so the three Gradle runs don't all rebuild at the same time
& "$root\gradlew.bat" classes
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

function Start-Run($task) {
    Start-Process powershell -ArgumentList '-NoExit', '-Command', "Set-Location '$root'; .\gradlew.bat $task"
}

function Test-Server {
    try { (New-Object Net.Sockets.TcpClient('localhost', 25565)).Close(); return $true } catch { return $false }
}

if (-not (Test-Server)) {
    Start-Run 'runServer'
    Write-Host 'Waiting for the server on port 25565...'
    while (-not (Test-Server)) { Start-Sleep -Seconds 3 }
}

Start-Run 'runClient -Pconnect'
if ($Clients -ge 2) {
    Start-Sleep -Seconds 5
    Start-Run 'runClient -Pconnect -Pplayer=Dev2 -PclientDir=run-client2'
}
