$ErrorActionPreference = "Stop"
$root = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
$out = Join-Path $root "server\purpur.jar"
Invoke-WebRequest "https://api.purpurmc.org/v2/purpur/1.21.11/latest/download" -OutFile $out
Write-Host "Purpur baixado para $out"
