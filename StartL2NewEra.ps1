[CmdletBinding()]
param(
    [Parameter(Position = 0)]
    [ValidateSet("init", "validate", "build", "up", "down", "restart", "status", "logs")]
    [string] $Action = "up",

    [switch] $SkipBuild,
    [switch] $Follow,
    [string[]] $Services = @()
)

$ErrorActionPreference = "Stop"
$runtimeScript = Join-Path $PSScriptRoot "tools\runtime\l2newera.ps1"

if (-not (Test-Path -LiteralPath $runtimeScript)) {
    throw "Launcher oficial nao encontrado: $runtimeScript"
}

& $runtimeScript -Action $Action -SkipBuild:$SkipBuild -Follow:$Follow -Services $Services
exit $LASTEXITCODE
